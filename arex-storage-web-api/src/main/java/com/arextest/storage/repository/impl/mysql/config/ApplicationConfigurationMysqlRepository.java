package com.arextest.storage.repository.impl.mysql.config;

import com.arextest.config.model.dto.application.ApplicationConfiguration;
import com.arextest.config.repository.ConfigRepositoryProvider;
import com.arextest.config.repository.impl.ApplicationConfigurationRepositoryImpl;
import com.arextest.storage.repository.impl.mysql.config.ConfigEntities.Application;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;

/**
 * Mysql implementation of the application config store. Replaces the mongo
 * edition when arex.storage.repository.type=mysql. Cascading removeByAppId
 * keeps the mongo behaviour: every config repository is asked to drop the
 * app's rows before the application row itself is deleted.
 */
public class ApplicationConfigurationMysqlRepository extends ApplicationConfigurationRepositoryImpl {

  private final ConfigApplicationMapper mapper;

  /**
   * Same cascade list as the mongo edition. Spring excludes this bean from
   * the collection injection, so no infinite recursion occurs.
   */
  @Resource
  private List<ConfigRepositoryProvider<?>> configRepositoryProviders;

  public ApplicationConfigurationMysqlRepository(ConfigApplicationMapper mapper) {
    super(null);
    this.mapper = mapper;
  }

  @Override
  public List<ApplicationConfiguration> list() {
    return mapper.selectAll().stream()
        .map(ConfigConverters::toDto)
        .collect(Collectors.toList());
  }

  @Override
  public List<ApplicationConfiguration> listBy(String appId) {
    return mapper.selectByAppId(appId).stream()
        .map(ConfigConverters::toDto)
        .collect(Collectors.toList());
  }

  @Override
  public boolean update(ApplicationConfiguration configuration) {
    if (configuration == null || StringUtils.isBlank(configuration.getAppId())) {
      return false;
    }
    return mapper.updateByAppId(ConfigConverters.fromDto(configuration),
        System.currentTimeMillis()) > 0;
  }

  @Override
  public boolean remove(ApplicationConfiguration configuration) {
    if (configuration == null || StringUtils.isBlank(configuration.getAppId())) {
      return false;
    }
    return this.removeByAppId(configuration.getAppId());
  }

  @Override
  public boolean removeByAppId(String appId) {
    for (ConfigRepositoryProvider<?> configRepositoryProvider : configRepositoryProviders) {
      configRepositoryProvider.removeByAppId(appId);
    }
    return mapper.deleteByAppId(appId) > 0;
  }

  @Override
  public boolean insert(ApplicationConfiguration configuration) {
    Application entity = ConfigConverters.fromDto(configuration);
    long now = System.currentTimeMillis();
    entity.setDataChangeCreateTime(now);
    entity.setDataChangeUpdateTime(now);
    mapper.insert(entity);
    configuration.setId(entity.getId());
    return true;
  }

  @Override
  public boolean addEnvToApp(String appId, Map<String, String> tags) {
    if (StringUtils.isBlank(appId) || tags == null || tags.isEmpty()) {
      return false;
    }
    List<Application> rows = mapper.selectByAppId(appId);
    if (rows.isEmpty()) {
      return false;
    }
    Application row = rows.get(0);
    Map<String, Set<String>> merged = ConfigJsonSupport.readJson(row.getTags(),
        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Set<String>>>() {
        });
    if (merged == null) {
      merged = new HashMap<>();
    }
    boolean changed = false;
    for (Map.Entry<String, String> entry : tags.entrySet()) {
      if (StringUtils.isBlank(entry.getKey()) || StringUtils.isBlank(entry.getValue())) {
        continue;
      }
      Set<String> values = merged.computeIfAbsent(entry.getKey(), k -> new HashSet<>());
      changed |= values.add(entry.getValue());
    }
    if (!changed) {
      return false;
    }
    return mapper.updateTags(appId, ConfigJsonSupport.writeJson(merged),
        System.currentTimeMillis()) > 0;
  }
}
