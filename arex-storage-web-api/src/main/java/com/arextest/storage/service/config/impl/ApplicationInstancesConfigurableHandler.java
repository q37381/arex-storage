package com.arextest.storage.service.config.impl;

import com.arextest.config.model.dto.application.InstancesConfiguration;
import com.arextest.config.repository.ConfigRepositoryProvider;
import com.arextest.config.repository.impl.InstancesConfigurationRepositoryImpl;
import com.arextest.storage.repository.impl.mysql.config.InstancesConfigurationMysqlRepository;
import com.arextest.storage.service.config.AbstractConfigurableHandler;
import java.util.List;
import java.util.Optional;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author jmo
 * @since 2022/1/23
 */
@Slf4j
@Component
public final class ApplicationInstancesConfigurableHandler extends
    AbstractConfigurableHandler<InstancesConfiguration> {

  @Resource
  private InstancesConfigurationRepositoryImpl instancesConfigurationRepository;

  protected ApplicationInstancesConfigurableHandler(
      @Autowired ConfigRepositoryProvider<InstancesConfiguration> repositoryProvider) {
    super(repositoryProvider);
  }

  public void createOrUpdate(InstancesConfiguration instancesConfiguration) {
    super.update(instancesConfiguration);
  }

  public List<InstancesConfiguration> listByAppOrdered(String appId) {
    return instancesConfigurationRepository.listByAppOrdered(appId);
  }

  public boolean deleteByAppIdAndHost(String appId, String host) {
    return instancesConfigurationRepository.removeByAppIdAndHost(appId, host);
  }

  /**
   * Config-delivery trace for the console "instance running status" view. Only
   * the MySQL repository keeps the trace, the MongoDB deployment simply skips
   * it. Failures are swallowed: observability must never break the agent that
   * is pulling its configuration.
   */
  public void updateLoadTrace(String appId, String host, long loadTime, Long loadVersion,
      String loadConfigJson) {
    try {
      mysqlRepository().ifPresent(repository -> repository
          .updateLoadTrace(appId, host, loadTime, loadVersion, loadConfigJson));
    } catch (Exception e) {
      LOGGER.warn("update instance load trace failed, appId: {}, host: {}", appId, host, e);
    }
  }

  public void updateHeartbeatTrace(String appId, String host, long heartbeatTime,
      String currentRate, Integer decelerateCode) {
    try {
      mysqlRepository().ifPresent(repository -> repository
          .updateHeartbeatTrace(appId, host, heartbeatTime, currentRate, decelerateCode));
    } catch (Exception e) {
      LOGGER.warn("update instance heartbeat trace failed, appId: {}, host: {}", appId, host, e);
    }
  }

  private Optional<InstancesConfigurationMysqlRepository> mysqlRepository() {
    if (instancesConfigurationRepository instanceof InstancesConfigurationMysqlRepository) {
      return Optional
          .of((InstancesConfigurationMysqlRepository) instancesConfigurationRepository);
    }
    return Optional.empty();
  }
}