package com.arextest.storage.repository.impl.mysql;

import com.arextest.common.config.DefaultApplicationConfig;
import com.arextest.model.mock.AREXQueryMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import com.arextest.model.replay.PagedRequestType;
import com.arextest.storage.beans.StorageConfigurationProperties;
import com.arextest.storage.model.Constants;
import com.arextest.storage.repository.ProviderNames;
import com.arextest.storage.repository.RepositoryProvider;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * MySQL implementation of the query-mocker repository. Mirrors the mongo
 * edition: it reads the same store as AREXMocker but projects request/response
 * as raw strings; only queryRecordList is supported.
 */
@Slf4j
public class AREXQueryMockerMysqlRepositoryProvider implements RepositoryProvider<AREXQueryMocker> {

  private final MockerRecordMapper mapper;
  private final String providerName;
  private final String mockerType;

  public AREXQueryMockerMysqlRepositoryProvider(MockerRecordMapper mapper,
      StorageConfigurationProperties properties,
      Set<MockCategoryType> entryPointTypes,
      DefaultApplicationConfig defaultApplicationConfig) {
    this(ProviderNames.DEFAULT, mapper);
  }

  public AREXQueryMockerMysqlRepositoryProvider(String providerName, MockerRecordMapper mapper) {
    this.providerName = providerName;
    this.mapper = mapper;
    this.mockerType = Constants.CLAZZ_NAME_AREX_QUERY_MOCKER;
  }

  @Override
  public Iterable<AREXQueryMocker> queryRecordList(MockCategoryType category, String recordId) {
    return queryRecordList(category, recordId, null);
  }

  @Override
  public Iterable<AREXQueryMocker> queryRecordList(MockCategoryType category, String recordId,
      String[] fieldNames) {
    List<MockerRecordEntity> rows =
        mapper.selectByRecordId(category.getName(), recordId, category.isEntryPoint());
    return rows.stream()
        .map(row -> attach(category, MockerRecordConverter.toQueryMocker(row, category)))
        .peek(this::addUseMocker)
        .collect(Collectors.toList());
  }

  @Override
  public AREXQueryMocker queryRecord(Mocker requestType) {
    throw new UnsupportedOperationException("Not supported.");
  }

  @Override
  public AREXQueryMocker queryById(MockCategoryType categoryType, String id) {
    throw new UnsupportedOperationException("Not supported.");
  }

  @Override
  public Iterable<AREXQueryMocker> queryEntryPointByRange(PagedRequestType pagedRequestType) {
    throw new UnsupportedOperationException("Not supported.");
  }

  @Override
  public long countByRange(PagedRequestType request) {
    throw new UnsupportedOperationException("Not supported.");
  }

  @Override
  public Map<String, Long> countByOperationName(PagedRequestType rangeRequestType) {
    throw new UnsupportedOperationException("Not supported.");
  }

  @Override
  public boolean save(AREXQueryMocker value) {
    throw new UnsupportedOperationException("Not supported.");
  }

  @Override
  public boolean saveList(List<AREXQueryMocker> valueList) {
    throw new UnsupportedOperationException("Not supported.");
  }

  @Override
  public long removeBy(MockCategoryType categoryType, String recordId) {
    throw new UnsupportedOperationException("Not supported.");
  }

  @Override
  public long extendExpirationTo(MockCategoryType categoryType, String recordId, Date expireTime) {
    throw new UnsupportedOperationException("Not supported.");
  }

  @Override
  public boolean update(AREXQueryMocker value) {
    throw new UnsupportedOperationException("Not supported.");
  }

  @Override
  public long removeByAppId(MockCategoryType categoryType, String appId) {
    throw new UnsupportedOperationException("Not supported.");
  }

  @Override
  public long removeByOperationNameAndAppId(MockCategoryType categoryType, String operationName,
      String appId) {
    throw new UnsupportedOperationException("Not supported.");
  }

  @Override
  public long removeById(MockCategoryType categoryType, String id) {
    throw new UnsupportedOperationException("Not supported.");
  }

  @Override
  public String getProviderName() {
    return this.providerName;
  }

  @Override
  public String getMockerType() {
    return this.mockerType;
  }

  private void addUseMocker(AREXQueryMocker item) {
    if (item != null && item.getUseMock() == null && item.getCategoryType() != null
        && !item.getCategoryType().isEntryPoint()) {
      item.setUseMock(true);
    }
  }

  private AREXQueryMocker attach(MockCategoryType category, AREXQueryMocker item) {
    if (item != null) {
      item.setCategoryType(category);
      if (category.isEntryPoint()) {
        item.setRecordId(item.getId());
      }
    }
    return item;
  }
}
