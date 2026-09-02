package com.arextest.storage.repository.impl.mysql.config;

import com.arextest.config.model.dto.ComparisonExclusionsConfiguration;
import com.arextest.config.repository.impl.ComparisonExclusionsConfigurationRepositoryImpl;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mysql implementation of the comparison exclusion store. Read-only for the
 * storage service, same as the mongo edition (writes return false there; the
 * api service owns the write path).
 */
public class ComparisonExclusionsConfigurationMysqlRepository
    extends ComparisonExclusionsConfigurationRepositoryImpl {

  private final ConfigComparisonExclusionsMapper mapper;

  public ComparisonExclusionsConfigurationMysqlRepository(ConfigComparisonExclusionsMapper mapper) {
    super(null);
    this.mapper = mapper;
  }

  @Override
  public List<ComparisonExclusionsConfiguration> list() {
    return null;
  }

  @Override
  public List<ComparisonExclusionsConfiguration> listBy(String appId) {
    return mapper.selectByAppId(appId).stream()
        .map(ConfigConverters::toDto)
        .collect(Collectors.toList());
  }

  @Override
  public List<ComparisonExclusionsConfiguration> listBy(String appId, int compareConfigType) {
    return mapper.selectByAppIdAndType(appId, compareConfigType).stream()
        .map(ConfigConverters::toDto)
        .collect(Collectors.toList());
  }

  @Override
  public boolean update(ComparisonExclusionsConfiguration configuration) {
    return false;
  }

  @Override
  public boolean remove(ComparisonExclusionsConfiguration configuration) {
    return false;
  }

  @Override
  public boolean insert(ComparisonExclusionsConfiguration configuration) {
    return false;
  }
}
