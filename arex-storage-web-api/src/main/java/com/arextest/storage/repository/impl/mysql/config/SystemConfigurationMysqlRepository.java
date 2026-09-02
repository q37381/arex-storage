package com.arextest.storage.repository.impl.mysql.config;

import com.arextest.config.model.dto.system.SystemConfiguration;
import com.arextest.config.repository.impl.SystemConfigurationRepositoryImpl;
import com.arextest.storage.repository.impl.mysql.config.ConfigEntities.System;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mysql implementation of the system configuration store (key-value style).
 */
public class SystemConfigurationMysqlRepository extends SystemConfigurationRepositoryImpl {

  private final ConfigSystemMapper mapper;

  public SystemConfigurationMysqlRepository(ConfigSystemMapper mapper) {
    super(null);
    this.mapper = mapper;
  }

  @Override
  public boolean saveConfig(SystemConfiguration systemConfig) {
    if (systemConfig == null || systemConfig.getKey() == null) {
      return false;
    }
    System entity = ConfigConverters.fromDto(systemConfig);
    // fully qualified: ConfigEntities.System shadows java.lang.System here
    long now = java.lang.System.currentTimeMillis();
    entity.setDataChangeCreateTime(now);
    entity.setDataChangeUpdateTime(now);
    return mapper.upsert(entity) > 0;
  }

  @Override
  public List<SystemConfiguration> getAllSystemConfigList() {
    return mapper.selectAll().stream()
        .map(ConfigConverters::toDto)
        .collect(Collectors.toList());
  }

  @Override
  public SystemConfiguration getSystemConfigByKey(String key) {
    return ConfigConverters.toDto(mapper.selectByKey(key));
  }

  @Override
  public boolean deleteConfig(String key) {
    return mapper.deleteByKey(key) > 0;
  }
}
