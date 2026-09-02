package com.arextest.storage.repository.impl.mysql.config;

import com.arextest.config.model.dto.application.ApplicationOperationConfiguration;
import com.arextest.config.repository.impl.ApplicationOperationConfigurationRepositoryImpl;
import com.arextest.storage.repository.impl.mysql.config.ConfigEntities.ServiceOperation;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.collections4.MapUtils;

/**
 * Mysql implementation of the service operation config store.
 * queryByMultiCondition translates mongo field names to columns through a
 * fixed whitelist (the mapper interpolates them into SQL).
 */
public class ApplicationOperationConfigurationMysqlRepository
    extends ApplicationOperationConfigurationRepositoryImpl {

  private static final Map<String, String> CONDITION_COLUMNS;

  static {
    Map<String, String> columns = new HashMap<>();
    columns.put("id", "id");
    columns.put("appId", "app_id");
    columns.put("serviceId", "service_id");
    columns.put("operationName", "operation_name");
    columns.put("operationType", "operation_type");
    columns.put("recordedCaseCount", "recorded_case_count");
    columns.put("status", "status");
    CONDITION_COLUMNS = Collections.unmodifiableMap(columns);
  }

  private final ConfigServiceOperationMapper mapper;

  public ApplicationOperationConfigurationMysqlRepository(ConfigServiceOperationMapper mapper) {
    super(null);
    this.mapper = mapper;
  }

  @Override
  public List<ApplicationOperationConfiguration> list() {
    throw new UnsupportedOperationException("this method is not implemented");
  }

  @Override
  public List<ApplicationOperationConfiguration> listBy(String appId) {
    return mapper.selectByAppId(appId).stream()
        .map(ConfigConverters::toDto)
        .collect(Collectors.toList());
  }

  @Override
  public boolean update(ApplicationOperationConfiguration configuration) {
    if (configuration == null || configuration.getId() == null) {
      return false;
    }
    return mapper.updateStatusById(configuration.getId(), configuration.getStatus(),
        System.currentTimeMillis()) > 0;
  }

  @Override
  public boolean remove(ApplicationOperationConfiguration configuration) {
    if (configuration == null || configuration.getId() == null) {
      return false;
    }
    return mapper.deleteById(configuration.getId()) > 0;
  }

  @Override
  public boolean insert(ApplicationOperationConfiguration configuration) {
    ServiceOperation entity = ConfigConverters.fromDto(configuration);
    long now = System.currentTimeMillis();
    entity.setDataChangeCreateTime(now);
    entity.setDataChangeUpdateTime(now);
    mapper.insert(entity);
    configuration.setId(entity.getId());
    return true;
  }

  @Override
  public ApplicationOperationConfiguration listByOperationId(String operationId) {
    return ConfigConverters.toDto(mapper.selectById(operationId));
  }

  @Override
  public List<ApplicationOperationConfiguration> operationBaseInfoList(String serviceId) {
    return mapper.selectByServiceId(serviceId).stream()
        .map(ConfigConverters::toBaseInfo)
        .collect(Collectors.toList());
  }

  @Override
  public boolean removeByAppId(String appId) {
    return mapper.deleteByAppId(appId) > 0;
  }

  /**
   * mongo findAndModify(upsert) semantics: match by (appId, serviceId,
   * operationName); on update the operationTypes are union-merged
   * (addToSet); on insert a fresh row is created.
   */
  @Override
  public boolean findAndUpdate(ApplicationOperationConfiguration configuration) {
    if (configuration == null) {
      return false;
    }
    ServiceOperation existing = mapper.selectByUniqueOp(configuration.getAppId(),
        configuration.getServiceId(), configuration.getOperationName());
    long now = System.currentTimeMillis();
    if (existing == null) {
      ServiceOperation entity = ConfigConverters.fromDto(configuration);
      entity.setDataChangeCreateTime(now);
      entity.setDataChangeUpdateTime(now);
      mapper.insert(entity);
      configuration.setId(entity.getId());
      return true;
    }
    Set<String> mergedTypes = new HashSet<>(
        ConfigJsonSupport.readJson(existing.getOperationTypes(),
            new com.fasterxml.jackson.core.type.TypeReference<Set<String>>() {
            }) == null ? Collections.emptySet()
            : ConfigJsonSupport.readJson(existing.getOperationTypes(),
                new com.fasterxml.jackson.core.type.TypeReference<Set<String>>() {
                }));
    if (configuration.getOperationTypes() != null) {
      mergedTypes.addAll(configuration.getOperationTypes());
    }
    ServiceOperation update = new ServiceOperation();
    update.setId(existing.getId());
    update.setOperationType(configuration.getOperationType());
    update.setStatus(configuration.getStatus());
    update.setOperationTypes(ConfigJsonSupport.writeJson(mergedTypes));
    update.setDataChangeUpdateTime(now);
    mapper.updateForFindAndModify(update);
    configuration.setId(existing.getId());
    return true;
  }

  @Override
  public List<ApplicationOperationConfiguration> queryByMultiCondition(
      Map<String, Object> conditions) {
    if (MapUtils.isEmpty(conditions)) {
      return Collections.emptyList();
    }
    Map<String, Object> translated = new HashMap<>();
    for (Map.Entry<String, Object> condition : conditions.entrySet()) {
      String column = CONDITION_COLUMNS.get(condition.getKey());
      if (column != null && condition.getValue() != null) {
        translated.put(column, condition.getValue());
      }
    }
    if (translated.isEmpty()) {
      return Collections.emptyList();
    }
    return mapper.selectByConditions(translated).stream()
        .map(ConfigConverters::toDto)
        .collect(Collectors.toList());
  }
}
