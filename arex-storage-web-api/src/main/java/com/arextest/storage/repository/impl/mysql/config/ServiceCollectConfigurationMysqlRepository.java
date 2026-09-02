package com.arextest.storage.repository.impl.mysql.config;

import com.arextest.config.model.dto.record.ServiceCollectConfiguration;
import com.arextest.config.repository.impl.ServiceCollectConfigurationRepositoryImpl;
import com.arextest.storage.repository.impl.mysql.config.ConfigEntities.RecordService;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mysql implementation of the recording switch / sample rate store. This is
 * the table that drives /api/config/agent/load (recording on-off, sample
 * rate, machine count limit) and the agentStatus Last-Modified probe.
 */
public class ServiceCollectConfigurationMysqlRepository
    extends ServiceCollectConfigurationRepositoryImpl {

  private final ConfigRecordServiceMapper mapper;

  public ServiceCollectConfigurationMysqlRepository(ConfigRecordServiceMapper mapper) {
    super(null);
    this.mapper = mapper;
  }

  @Override
  public List<ServiceCollectConfiguration> list() {
    throw new UnsupportedOperationException("this method is not implemented");
  }

  @Override
  public List<ServiceCollectConfiguration> listBy(String appId) {
    return mapper.selectByAppId(appId).stream()
        .map(ConfigConverters::toDto)
        .collect(Collectors.toList());
  }

  @Override
  public boolean update(ServiceCollectConfiguration configuration) {
    if (configuration == null || configuration.getAppId() == null) {
      return false;
    }
    RecordService entity = ConfigConverters.fromDto(configuration);
    // mongo edition: recordMachineCountLimit falls back to 1 on update
    entity.setRecordMachineCountLimit(
        configuration.getRecordMachineCountLimit() == null ? 1
            : configuration.getRecordMachineCountLimit());
    return mapper.updateByAppId(entity, System.currentTimeMillis()) > 0;
  }

  @Override
  public boolean remove(ServiceCollectConfiguration configuration) {
    if (configuration == null || configuration.getAppId() == null) {
      return false;
    }
    return mapper.deleteByAppId(configuration.getAppId()) > 0;
  }

  @Override
  public boolean insert(ServiceCollectConfiguration configuration) {
    RecordService entity = ConfigConverters.fromDto(configuration);
    long now = System.currentTimeMillis();
    entity.setDataChangeCreateTime(now);
    entity.setDataChangeUpdateTime(now);
    return mapper.insert(entity) > 0;
  }

  @Override
  public boolean removeByAppId(String appId) {
    return mapper.deleteByAppId(appId) > 0;
  }

  @Override
  public boolean updateMultiEnvConfig(ServiceCollectConfiguration configuration) {
    if (configuration == null || configuration.getAppId() == null) {
      return false;
    }
    return mapper.updateMultiEnvConfigs(configuration.getAppId(),
        ConfigJsonSupport.writeJson(configuration.getMultiEnvConfigs()),
        System.currentTimeMillis()) > 0;
  }

  @Override
  public boolean updateServiceCollectTime(String appId) {
    if (appId == null) {
      return false;
    }
    return mapper.touchModifiedTime(appId, System.currentTimeMillis()) > 0;
  }
}
