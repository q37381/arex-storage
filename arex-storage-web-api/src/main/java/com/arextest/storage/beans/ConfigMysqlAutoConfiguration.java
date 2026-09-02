package com.arextest.storage.beans;

import com.arextest.config.repository.impl.ApplicationConfigurationRepositoryImpl;
import com.arextest.config.repository.impl.ApplicationOperationConfigurationRepositoryImpl;
import com.arextest.config.repository.impl.ApplicationServiceConfigurationRepositoryImpl;
import com.arextest.config.repository.impl.ComparisonExclusionsConfigurationRepositoryImpl;
import com.arextest.config.repository.impl.DynamicClassConfigurationRepositoryImpl;
import com.arextest.config.repository.impl.InstancesConfigurationRepositoryImpl;
import com.arextest.config.repository.impl.ServiceCollectConfigurationRepositoryImpl;
import com.arextest.config.repository.impl.SystemConfigurationRepositoryImpl;
import com.arextest.storage.repository.AppContractRepository;
import com.arextest.storage.repository.impl.mysql.config.ApplicationConfigurationMysqlRepository;
import com.arextest.storage.repository.impl.mysql.config.ApplicationOperationConfigurationMysqlRepository;
import com.arextest.storage.repository.impl.mysql.config.ApplicationServiceConfigurationMysqlRepository;
import com.arextest.storage.repository.impl.mysql.config.ComparisonExclusionsConfigurationMysqlRepository;
import com.arextest.storage.repository.impl.mysql.config.ConfigApplicationMapper;
import com.arextest.storage.repository.impl.mysql.config.ConfigComparisonExclusionsMapper;
import com.arextest.storage.repository.impl.mysql.config.ConfigDynamicClassMapper;
import com.arextest.storage.repository.impl.mysql.config.ConfigInstancesMapper;
import com.arextest.storage.repository.impl.mysql.config.ConfigRecordServiceMapper;
import com.arextest.storage.repository.impl.mysql.config.ConfigServiceMapper;
import com.arextest.storage.repository.impl.mysql.config.ConfigServiceOperationMapper;
import com.arextest.storage.repository.impl.mysql.config.ConfigSystemMapper;
import com.arextest.storage.repository.impl.mysql.config.DynamicClassConfigurationMysqlRepository;
import com.arextest.storage.repository.impl.mysql.config.InstancesConfigurationMysqlRepository;
import com.arextest.storage.repository.impl.mysql.config.ServiceCollectConfigurationMysqlRepository;
import com.arextest.storage.repository.impl.mysql.config.SystemConfigurationMysqlRepository;
import java.util.Collections;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Mysql-backed config repositories (recording switch, agent config push,
 * instances, dynamic classes, system config). Mirrors
 * {@link ConfigServiceAutoConfiguration} which registers the mongo editions
 * for arex.storage.repository.type=mongodb.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "arex.storage.repository.type", havingValue = "mysql",
    matchIfMissing = true)
public class ConfigMysqlAutoConfiguration {

  /**
   * The mongo edition only reads app contracts for display; not required by
   * the record/replay closed loop, kept as an empty stub.
   */
  @Bean
  public AppContractRepository mysqlAppContractRepository() {
    return appId -> Collections.emptyList();
  }

  @Bean
  public ApplicationConfigurationRepositoryImpl applicationConfigurationRepositoryImpl(
      ConfigApplicationMapper mapper) {
    return new ApplicationConfigurationMysqlRepository(mapper);
  }

  @Bean
  public ApplicationServiceConfigurationRepositoryImpl applicationServiceConfigurationRepositoryImpl(
      ConfigServiceMapper mapper,
      ApplicationOperationConfigurationRepositoryImpl operationConfigurationRepository) {
    return new ApplicationServiceConfigurationMysqlRepository(mapper,
        operationConfigurationRepository);
  }

  @Bean
  public ApplicationOperationConfigurationRepositoryImpl applicationOperationConfigurationRepositoryImpl(
      ConfigServiceOperationMapper mapper) {
    return new ApplicationOperationConfigurationMysqlRepository(mapper);
  }

  @Bean
  public InstancesConfigurationRepositoryImpl instancesConfigurationRepositoryImpl(
      ConfigInstancesMapper mapper) {
    return new InstancesConfigurationMysqlRepository(mapper);
  }

  @Bean
  public ServiceCollectConfigurationRepositoryImpl serviceCollectConfigurationRepositoryImpl(
      ConfigRecordServiceMapper mapper) {
    return new ServiceCollectConfigurationMysqlRepository(mapper);
  }

  @Bean
  public DynamicClassConfigurationRepositoryImpl dynamicClassConfigurationRepositoryImpl(
      ConfigDynamicClassMapper mapper) {
    return new DynamicClassConfigurationMysqlRepository(mapper);
  }

  @Bean
  public SystemConfigurationRepositoryImpl systemConfigurationRepositoryImpl(
      ConfigSystemMapper mapper) {
    return new SystemConfigurationMysqlRepository(mapper);
  }

  @Bean
  public ComparisonExclusionsConfigurationRepositoryImpl comparisonExclusionsConfigurationRepositoryImpl(
      ConfigComparisonExclusionsMapper mapper) {
    return new ComparisonExclusionsConfigurationMysqlRepository(mapper);
  }
}
