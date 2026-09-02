package com.arextest.storage.repository.impl.mysql.config;

import com.arextest.config.model.dto.application.InstancesConfiguration;
import com.arextest.config.repository.impl.InstancesConfigurationRepositoryImpl;
import com.arextest.storage.repository.impl.mysql.config.ConfigEntities.Instances;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mysql implementation of the agent instance store. update() is an upsert by
 * (appId, host), the heartbeat path of /api/config/agent/load.
 */
public class InstancesConfigurationMysqlRepository extends InstancesConfigurationRepositoryImpl {

  private final ConfigInstancesMapper mapper;

  public InstancesConfigurationMysqlRepository(ConfigInstancesMapper mapper) {
    super(null);
    this.mapper = mapper;
  }

  @Override
  public List<InstancesConfiguration> list() {
    throw new UnsupportedOperationException("this method is not implemented");
  }

  @Override
  public List<InstancesConfiguration> listBy(String appId) {
    return mapper.selectByAppId(appId).stream()
        .map(ConfigConverters::toDto)
        .collect(Collectors.toList());
  }

  @Override
  public List<InstancesConfiguration> listByAppOrdered(String appId) {
    return mapper.selectByAppIdOrdered(appId).stream()
        .map(ConfigConverters::toDto)
        .collect(Collectors.toList());
  }

  @Override
  public boolean update(InstancesConfiguration configuration) {
    if (configuration == null || configuration.getAppId() == null) {
      return false;
    }
    Instances entity = ConfigConverters.fromDto(configuration);
    long now = System.currentTimeMillis();
    // mongo edition stamps dataUpdateTime with the server time on every report
    entity.setDataUpdateTime(now);
    entity.setDataChangeCreateTime(now);
    entity.setDataChangeUpdateTime(now);
    return mapper.upsert(entity) > 0;
  }

  @Override
  public boolean remove(InstancesConfiguration configuration) {
    if (configuration == null || configuration.getAppId() == null) {
      return false;
    }
    if (configuration.getHost() != null) {
      return mapper.deleteByAppIdAndHost(configuration.getAppId(), configuration.getHost()) > 0;
    }
    return mapper.deleteByAppId(configuration.getAppId()) > 0;
  }

  @Override
  public boolean insert(InstancesConfiguration configuration) {
    return this.update(configuration);
  }

  @Override
  public boolean removeByAppId(String appId) {
    return mapper.deleteByAppId(appId) > 0;
  }

  @Override
  public boolean removeByAppIdAndHost(String appId, String host) {
    return mapper.deleteByAppIdAndHost(appId, host) > 0;
  }
}
