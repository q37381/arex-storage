package com.arextest.storage.repository.impl.mysql.config;

import com.arextest.config.model.dto.application.ApplicationServiceConfiguration;
import com.arextest.config.repository.impl.ApplicationOperationConfigurationRepositoryImpl;
import com.arextest.config.repository.impl.ApplicationServiceConfigurationRepositoryImpl;
import com.arextest.storage.repository.impl.mysql.config.ConfigEntities.Service;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mysql implementation of the application service config store.
 */
public class ApplicationServiceConfigurationMysqlRepository
    extends ApplicationServiceConfigurationRepositoryImpl {

  private final ConfigServiceMapper mapper;
  private final ApplicationOperationConfigurationRepositoryImpl operationRepository;

  public ApplicationServiceConfigurationMysqlRepository(ConfigServiceMapper mapper,
      ApplicationOperationConfigurationRepositoryImpl operationRepository) {
    super(null, operationRepository);
    this.mapper = mapper;
    this.operationRepository = operationRepository;
  }

  @Override
  public List<ApplicationServiceConfiguration> list() {
    throw new UnsupportedOperationException("this method is not implemented");
  }

  @Override
  public List<ApplicationServiceConfiguration> listBy(String appId) {
    return mapper.selectByAppId(appId).stream()
        .map(entity -> {
          ApplicationServiceConfiguration dto = ConfigConverters.toDto(entity);
          dto.setOperationList(operationRepository.operationBaseInfoList(dto.getId()));
          return dto;
        })
        .collect(Collectors.toList());
  }

  @Override
  public boolean update(ApplicationServiceConfiguration configuration) {
    if (configuration == null || configuration.getId() == null) {
      return false;
    }
    return mapper.updateStatusById(configuration.getId(), configuration.getStatus(),
        System.currentTimeMillis()) > 0;
  }

  @Override
  public boolean remove(ApplicationServiceConfiguration configuration) {
    if (configuration == null || configuration.getId() == null) {
      return false;
    }
    return mapper.deleteById(configuration.getId()) > 0;
  }

  @Override
  public boolean insert(ApplicationServiceConfiguration configuration) {
    Service entity = ConfigConverters.fromDto(configuration);
    long now = System.currentTimeMillis();
    entity.setDataChangeCreateTime(now);
    entity.setDataChangeUpdateTime(now);
    // upsert guards against concurrent first-load of the same app
    mapper.upsert(entity);
    configuration.setId(entity.getId());
    return true;
  }

  @Override
  public long count(String appId) {
    return mapper.countByAppId(appId);
  }

  @Override
  public boolean removeByAppId(String appId) {
    return mapper.deleteByAppId(appId) > 0;
  }
}
