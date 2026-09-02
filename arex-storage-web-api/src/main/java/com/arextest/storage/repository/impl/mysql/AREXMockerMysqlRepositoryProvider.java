package com.arextest.storage.repository.impl.mysql;

import com.arextest.common.config.DefaultApplicationConfig;
import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import com.arextest.model.replay.PagedRequestType;
import com.arextest.model.replay.SortingOption;
import com.arextest.model.replay.SortingTypeEnum;
import com.arextest.storage.beans.StorageConfigurationProperties;
import com.arextest.storage.model.Constants;
import com.arextest.storage.repository.ProviderNames;
import com.arextest.storage.repository.RepositoryProvider;
import com.arextest.storage.utils.TimeUtils;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * MySQL (MyBatis) implementation of the rolling mocker repository, semantic
 * parity with the mongo edition:
 * - entry-point rows use id = recordId and an empty record_id column;
 * - reads on the rolling provider extend expiration sideways;
 * - paged range queries pin the recordVersion of the newest record in range.
 *
 * Column values are epoch millis instead of mongo Date; nested payloads are
 * JSON columns handled by {@link MockerRecordConverter}.
 */
@Slf4j
public class AREXMockerMysqlRepositoryProvider implements RepositoryProvider<AREXMocker> {

  private static final int DEFAULT_MIN_LIMIT_SIZE = 1;
  private static final int DEFAULT_MAX_LIMIT_SIZE = 1000;
  private static final long FOURTEEN_DAYS_MILLIS = 14L * 24 * 60 * 60 * 1000;
  private static final String AUTO_PINNED_MOCKER_EXPIRATION_MILLIS =
      "AutoPinned.mocker.expiration.millis";

  /**
   * Request-visible field name -> column name. Sorting labels come from client
   * input and are inlined into ORDER BY, so they must pass this whitelist.
   */
  private static final Map<String, String> SORTABLE_COLUMNS;

  static {
    Map<String, String> columns = new HashMap<>();
    columns.put("creationTime", "creation_time");
    columns.put("updateTime", "update_time");
    columns.put("expirationTime", "expiration_time");
    columns.put("recordId", "record_id");
    columns.put("appId", "app_id");
    columns.put("operationName", "operation_name");
    columns.put("recordVersion", "record_version");
    SORTABLE_COLUMNS = Collections.unmodifiableMap(columns);
  }

  private final MockerRecordMapper mapper;
  private final String providerName;
  private final StorageConfigurationProperties properties;
  private final Set<MockCategoryType> entryPointTypes;
  private final DefaultApplicationConfig defaultApplicationConfig;
  private final String mockerType;

  public AREXMockerMysqlRepositoryProvider(MockerRecordMapper mapper,
      StorageConfigurationProperties properties,
      Set<MockCategoryType> entryPointTypes,
      DefaultApplicationConfig defaultApplicationConfig) {
    this(ProviderNames.DEFAULT, mapper, properties, entryPointTypes, defaultApplicationConfig);
  }

  public AREXMockerMysqlRepositoryProvider(String providerName,
      MockerRecordMapper mapper,
      StorageConfigurationProperties properties,
      Set<MockCategoryType> entryPointTypes,
      DefaultApplicationConfig defaultApplicationConfig) {
    this.providerName = providerName;
    this.mapper = mapper;
    this.properties = properties;
    this.entryPointTypes = entryPointTypes;
    this.defaultApplicationConfig = defaultApplicationConfig;
    this.mockerType = Constants.CLAZZ_NAME_AREX_MOCKER;
  }

  // region: reader
  @Override
  public Iterable<AREXMocker> queryRecordList(MockCategoryType category, String recordId) {
    return queryRecordList(category, recordId, null);
  }

  @Override
  public Iterable<AREXMocker> queryRecordList(MockCategoryType category, String recordId,
      String[] fieldNames) {
    if (Objects.equals(this.providerName, ProviderNames.DEFAULT)) {
      extendExpirationOnRead(category, recordId);
    }
    List<MockerRecordEntity> rows =
        mapper.selectByRecordId(category.getName(), recordId, category.isEntryPoint());
    List<AREXMocker> result = rows.stream()
        .map(row -> attach(category, MockerRecordConverter.toMocker(row, category)))
        .peek(this::addUseMocker)
        .collect(Collectors.toList());
    return result;
  }

  @Override
  public AREXMocker queryRecord(Mocker requestType) {
    MockCategoryType category = requestType.getCategoryType();
    MockerRecordEntity row = mapper.selectLatestRecord(category.getName(), requestType.getAppId(),
        requestType.getOperationName(), requestType.getRecordEnvironment(),
        requestType.getRecordId(), category.isEntryPoint());
    if (row == null) {
      return null;
    }
    AREXMocker mocker = attach(category, MockerRecordConverter.toMocker(row, category));
    addUseMocker(mocker);
    return mocker;
  }

  @Override
  public AREXMocker queryById(MockCategoryType category, String id) {
    MockerRecordEntity row = mapper.selectById(category.getName(), id);
    return row == null ? null : attach(category, MockerRecordConverter.toMocker(row, category));
  }

  @Override
  public Iterable<AREXMocker> queryEntryPointByRange(PagedRequestType pagedRequestType) {
    MockCategoryType category = pagedRequestType.getCategory();
    String recordVersion = lastRecordVersionOfRange(pagedRequestType);

    if (Objects.equals(this.providerName, ProviderNames.DEFAULT) && category != null
        && category.isEntryPoint()) {
      // the mongo edition extends expiration for the whole filtered range;
      // here we refresh per entry-point row below after fetching the page.
    }

    int pageSize = Math.min(
        pagedRequestType.getPageSize() <= 0 ? DEFAULT_MAX_LIMIT_SIZE
            : pagedRequestType.getPageSize(), DEFAULT_MAX_LIMIT_SIZE);
    Integer pageIndex = pagedRequestType.getPageIndex();
    int offset = pageIndex == null ? 0 : pageSize * (pageIndex - 1);

    List<MockerRecordEntity> rows = mapper.selectByRange(category.getName(),
        pagedRequestType.getAppId(), pagedRequestType.getOperation(), pagedRequestType.getEnv(),
        pagedRequestType.getBeginTime(), pagedRequestType.getEndTime(), recordVersion,
        MapUtils.isEmpty(pagedRequestType.getTags()) ? null : pagedRequestType.getTags(),
        toOrderBy(pagedRequestType.getSortingOptions()), offset, pageSize);

    if (Objects.equals(this.providerName, ProviderNames.DEFAULT)) {
      for (MockerRecordEntity row : rows) {
        extendExpirationOnRead(category, row.getId());
      }
    }
    return rows.stream()
        .map(row -> attach(category, MockerRecordConverter.toMocker(row, category)))
        .collect(Collectors.toList());
  }

  @Override
  public long countByRange(PagedRequestType request) {
    String recordVersion = lastRecordVersionOfRange(request);
    return mapper.countByRange(request.getCategory().getName(), request.getAppId(),
        request.getOperation(), request.getEnv(), request.getBeginTime(), request.getEndTime(),
        recordVersion);
  }

  @Override
  public Map<String, Long> countByOperationName(PagedRequestType rangeRequestType) {
    String recordVersion = lastRecordVersionOfRange(rangeRequestType);
    List<MockerRecordMapper.OperationCount> counts = mapper.countGroupByOperation(
        rangeRequestType.getCategory().getName(), rangeRequestType.getAppId(),
        rangeRequestType.getOperation(), rangeRequestType.getEnv(),
        rangeRequestType.getBeginTime(), rangeRequestType.getEndTime(), recordVersion);
    Map<String, Long> result = new HashMap<>();
    for (MockerRecordMapper.OperationCount count : counts) {
      if (count.getOperationName() != null) {
        result.put(count.getOperationName(), count.getCnt());
      }
    }
    return result;
  }
  // endregion

  // region: writer
  @Override
  public boolean save(AREXMocker value) {
    if (value == null) {
      return false;
    }
    return saveList(Collections.singletonList(value));
  }

  @Override
  public boolean saveList(List<AREXMocker> valueList) {
    if (CollectionUtils.isEmpty(valueList)) {
      return false;
    }
    MockCategoryType category = valueList.get(0).getCategoryType();
    try {
      long expiration;
      if (StringUtils.equalsIgnoreCase(ProviderNames.AUTO_PINNED, this.providerName)) {
        expiration = defaultApplicationConfig.getConfigAsLong(AUTO_PINNED_MOCKER_EXPIRATION_MILLIS,
            FOURTEEN_DAYS_MILLIS);
      } else {
        expiration = properties.getExpirationDurationMap()
            .getOrDefault(category.getName(), properties.getDefaultExpirationDuration());
      }

      long currentTime = System.currentTimeMillis();
      for (AREXMocker item : valueList) {
        item.setCreationTime(currentTime);
        item.setUpdateTime(currentTime);
        item.setExpirationTime(currentTime + expiration);
        if (category.isEntryPoint()) {
          item.setId(item.getRecordId());
          item.setRecordId(null);
        } else if (StringUtils.isEmpty(item.getId())) {
          item.setId(UUID.randomUUID().toString().replace("-", ""));
        }
      }
      mapper.insertBatch(MockerRecordConverter.toEntities(valueList, category.getName()));
    } catch (Throwable ex) {
      // rolling mocker save failed: remove all entry point data of the record
      if (Objects.equals(this.providerName, ProviderNames.DEFAULT)) {
        String recordId = valueList.get(0).getRecordId();
        for (MockCategoryType entryPointType : entryPointTypes) {
          removeBy(entryPointType, recordId);
        }
      }
      LOGGER.error("save mocker list error: {}, size: {}", ex.getMessage(), valueList.size(), ex);
      return false;
    }
    return true;
  }

  @Override
  public boolean update(AREXMocker value) {
    try {
      return mapper.updateById(
          MockerRecordConverter.toEntity(value, value.getCategoryType().getName())) > 0;
    } catch (Exception e) {
      LOGGER.error("update mocker record error: {}", e.getMessage(), e);
      return false;
    }
  }

  @Override
  public long removeBy(MockCategoryType category, String recordId) {
    return mapper.deleteByRecordId(category.getName(), recordId, category.isEntryPoint());
  }

  @Override
  public long extendExpirationTo(MockCategoryType category, String recordId, Date expireTime) {
    return mapper.extendExpirationTo(category.getName(), recordId, expireTime.getTime());
  }

  @Override
  public long removeByAppId(MockCategoryType category, String appId) {
    return mapper.deleteByAppId(category.getName(), appId);
  }

  @Override
  public long removeByOperationNameAndAppId(MockCategoryType category, String operationName,
      String appId) {
    return mapper.deleteByOperationAndAppId(category.getName(),
        operationName == null ? "" : operationName, appId);
  }

  @Override
  public long removeById(MockCategoryType category, String id) {
    return mapper.deleteById(category.getName(), id);
  }
  // endregion

  @Override
  public String getProviderName() {
    return this.providerName;
  }

  @Override
  public String getMockerType() {
    return this.mockerType;
  }

  private String lastRecordVersionOfRange(PagedRequestType request) {
    MockerRecordEntity latest = mapper.selectLatestVersionOfRange(request.getCategory().getName(),
        request.getAppId(), request.getOperation(), request.getEnv(), request.getBeginTime(),
        request.getEndTime());
    return latest == null ? null : latest.getRecordVersion();
  }

  /**
   * Sideways expiration extension, mirrors the mongo edition: rows expiring
   * before (today's first millisecond + allowReRunDays) are pushed beyond the
   * boundary, with a random-ish minute offset to spread the cleanup load.
   */
  private void extendExpirationOnRead(MockCategoryType category, String recordId) {
    try {
      long now = System.currentTimeMillis();
      long allowedLastMills =
          TimeUtils.getTodayFirstMills() + properties.getAllowReRunDays() * TimeUtils.ONE_DAY;
      mapper.extendExpirationOnRead(category.getName(), recordId, category.isEntryPoint(),
          allowedLastMills + now % TimeUtils.ONE_HOUR, allowedLastMills, now);
    } catch (Exception e) {
      LOGGER.warn("extend expiration on read failed: {}", e.getMessage());
    }
  }

  private String toOrderBy(List<SortingOption> sortingOptions) {
    if (CollectionUtils.isEmpty(sortingOptions)) {
      return "creation_time ASC";
    }
    StringBuilder orderBy = new StringBuilder();
    for (SortingOption option : sortingOptions) {
      String column = SORTABLE_COLUMNS.get(option.getLabel());
      if (column == null) {
        continue;
      }
      if (orderBy.length() > 0) {
        orderBy.append(", ");
      }
      orderBy.append(column)
          .append(SortingTypeEnum.ASCENDING.getCode() == option.getSortingType() ? " ASC" : " DESC");
    }
    return orderBy.length() == 0 ? "creation_time ASC" : orderBy.toString();
  }

  private AREXMocker attach(MockCategoryType category, AREXMocker item) {
    if (item != null) {
      item.setCategoryType(category);
      if (category.isEntryPoint()) {
        item.setRecordId(item.getId());
      }
    }
    return item;
  }

  private void addUseMocker(AREXMocker item) {
    if (item != null && item.getUseMock() == null && item.getCategoryType() != null
        && !item.getCategoryType().isEntryPoint()) {
      item.setUseMock(true);
    }
  }
}
