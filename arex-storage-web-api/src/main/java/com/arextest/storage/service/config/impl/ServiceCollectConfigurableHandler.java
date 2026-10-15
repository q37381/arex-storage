package com.arextest.storage.service.config.impl;

import com.arextest.config.model.dto.application.InstancesConfiguration;
import com.arextest.config.model.dto.record.ServiceCollectConfiguration;
import com.arextest.config.model.dto.system.RecordGlobalDefault;
import com.arextest.config.repository.ConfigRepositoryProvider;
import com.arextest.config.repository.SystemConfigurationRepository;
import com.arextest.config.system.SystemConfigKeys;
import com.arextest.storage.service.config.AbstractConfigurableHandler;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * @author jmo
 * @since 2022/1/22
 */
@Slf4j
@Component
public final class ServiceCollectConfigurableHandler extends
    AbstractConfigurableHandler<ServiceCollectConfiguration> {

  private static final String INCLUDE_SERVICE_OPERATIONS = "includeServiceOperations";

  @Resource
  private ServiceCollectConfiguration globalDefaultConfiguration;

  @Resource
  private SystemConfigurationRepository systemConfigurationRepository;

  private ServiceCollectConfigurableHandler(
      @Autowired ConfigRepositoryProvider<ServiceCollectConfiguration> repositoryProvider) {
    super(repositoryProvider);
  }

  @Override
  public List<ServiceCollectConfiguration> createFromGlobalDefault(String appId) {
    ServiceCollectConfiguration serviceCollectConfiguration = new ServiceCollectConfiguration();
    serviceCollectConfiguration.setAppId(appId);
    serviceCollectConfiguration.setSampleRate(effectiveGlobal().getSampleRate());
    serviceCollectConfiguration.setAllowDayOfWeeks(effectiveGlobal().getAllowDayOfWeeks());
    serviceCollectConfiguration.setTimeMock(effectiveGlobal().isTimeMock());
    serviceCollectConfiguration.setAllowTimeOfDayFrom(effectiveGlobal().getAllowTimeOfDayFrom());
    serviceCollectConfiguration.setAllowTimeOfDayTo(effectiveGlobal().getAllowTimeOfDayTo());
    serviceCollectConfiguration.setRecordMachineCountLimit(
        effectiveGlobal().getRecordMachineCountLimit() == null ? 1
            : effectiveGlobal().getRecordMachineCountLimit());
    if (CollectionUtils.isNotEmpty(effectiveGlobal().getExcludeServiceOperationSet())) {
      serviceCollectConfiguration.setExcludeServiceOperationSet(
          new java.util.HashSet<>(effectiveGlobal().getExcludeServiceOperationSet()));
    }
    String include = effectiveGlobalIncludeOperations();
    if (StringUtils.isNotEmpty(include)) {
      Map<String, String> extendField = new HashMap<>();
      extendField.put(INCLUDE_SERVICE_OPERATIONS, include);
      serviceCollectConfiguration.setExtendField(extendField);
    }
    update(serviceCollectConfiguration);
    return Collections.singletonList(serviceCollectConfiguration);
  }

  @Override
  public boolean update(ServiceCollectConfiguration configuration) {
    return super.update(configuration) || super.insert(configuration);
  }

  /**
   * Live field-level inheritance: for each field the application has not explicitly configured
   * (sentinel: {@code <= 0} / null / empty), fall back to the dynamic global default
   * ({@code record_global_default}) and then to the built-in YAML default. Configured per-app
   * fields are never overwritten.
   */
  @Override
  protected void mergeGlobalDefaultSettings(ServiceCollectConfiguration source) {
    if (source == null) {
      return;
    }
    ServiceCollectConfiguration fallback = effectiveGlobal();
    if (source.getSampleRate() <= 0) {
      source.setSampleRate(fallback.getSampleRate());
    }
    if (source.getAllowDayOfWeeks() <= 0) {
      source.setAllowDayOfWeeks(fallback.getAllowDayOfWeeks());
    }
    if (StringUtils.isEmpty(source.getAllowTimeOfDayFrom())) {
      source.setAllowTimeOfDayFrom(fallback.getAllowTimeOfDayFrom());
    }
    if (StringUtils.isEmpty(source.getAllowTimeOfDayTo())) {
      source.setAllowTimeOfDayTo(fallback.getAllowTimeOfDayTo());
    }
    if (source.getRecordMachineCountLimit() == null || source.getRecordMachineCountLimit() <= 0) {
      source.setRecordMachineCountLimit(fallback.getRecordMachineCountLimit());
    }
    if (CollectionUtils.isEmpty(source.getExcludeServiceOperationSet())
        && CollectionUtils.isNotEmpty(fallback.getExcludeServiceOperationSet())) {
      source.setExcludeServiceOperationSet(
          new java.util.HashSet<>(fallback.getExcludeServiceOperationSet()));
    }
    Map<String, String> extendField = source.getExtendField();
    if (MapUtils.isEmpty(extendField)
        || StringUtils.isEmpty(extendField.get(INCLUDE_SERVICE_OPERATIONS))) {
      String include = effectiveGlobalIncludeOperations();
      if (StringUtils.isNotEmpty(include)) {
        Map<String, String> merged =
            extendField == null ? new HashMap<>() : new HashMap<>(extendField);
        merged.put(INCLUDE_SERVICE_OPERATIONS, include);
        source.setExtendField(merged);
      }
    }
  }

  @Override
  protected boolean shouldMergeGlobalDefault() {
    return true;
  }

  /**
   * The effective recording default: {@code record_global_default} (config_system) with any unset
   * field falling back to the built-in YAML bean. Exposed so the agent config controller can apply
   * the same include/exclude defaults.
   */
  public ServiceCollectConfiguration effectiveGlobal() {
    ServiceCollectConfiguration resolved = new ServiceCollectConfiguration();
    resolved.setSampleRate(globalDefaultConfiguration.getSampleRate());
    resolved.setAllowDayOfWeeks(globalDefaultConfiguration.getAllowDayOfWeeks());
    resolved.setTimeMock(globalDefaultConfiguration.isTimeMock());
    resolved.setAllowTimeOfDayFrom(globalDefaultConfiguration.getAllowTimeOfDayFrom());
    resolved.setAllowTimeOfDayTo(globalDefaultConfiguration.getAllowTimeOfDayTo());
    resolved.setRecordMachineCountLimit(
        globalDefaultConfiguration.getRecordMachineCountLimit() == null ? 1
            : globalDefaultConfiguration.getRecordMachineCountLimit());
    RecordGlobalDefault global = loadRecordGlobalDefault();
    if (global == null) {
      return resolved;
    }
    if (global.getSampleRate() != null && global.getSampleRate() > 0) {
      resolved.setSampleRate(global.getSampleRate());
    }
    if (global.getAllowDayOfWeeks() != null && global.getAllowDayOfWeeks() > 0) {
      resolved.setAllowDayOfWeeks(global.getAllowDayOfWeeks());
    }
    if (StringUtils.isNotEmpty(global.getAllowTimeOfDayFrom())) {
      resolved.setAllowTimeOfDayFrom(global.getAllowTimeOfDayFrom());
    }
    if (StringUtils.isNotEmpty(global.getAllowTimeOfDayTo())) {
      resolved.setAllowTimeOfDayTo(global.getAllowTimeOfDayTo());
    }
    if (global.getRecordMachineCountLimit() != null && global.getRecordMachineCountLimit() > 0) {
      resolved.setRecordMachineCountLimit(global.getRecordMachineCountLimit());
    }
    if (CollectionUtils.isNotEmpty(global.getExcludeServiceOperationSet())) {
      resolved.setExcludeServiceOperationSet(
          new java.util.HashSet<>(global.getExcludeServiceOperationSet()));
    }
    return resolved;
  }

  /**
   * The global default include-operations string, taken directly from {@code record_global_default}
   * (no built-in YAML equivalent exists for it).
   */
  public String effectiveGlobalIncludeOperations() {
    RecordGlobalDefault global = loadRecordGlobalDefault();
    return global == null ? null : global.getIncludeServiceOperations();
  }

  private RecordGlobalDefault loadRecordGlobalDefault() {
    try {
      return systemConfigurationRepository.getGlobalConfig(SystemConfigKeys.RECORD_GLOBAL_DEFAULT);
    } catch (RuntimeException e) {
      LOGGER.error("load record_global_default failed", e);
      return null;
    }
  }

  /**
   * Allocate service collect config for instances.
   * @param appId appid
   * @param instances all instances of the app
   * @param requestInstance the instance that is requesting service collect config
   * @return pair of service collect config and instances that will use this config
   */
  public Pair<ServiceCollectConfiguration, List<InstancesConfiguration>> allocateServiceCollectConfig(
      String appId, List<InstancesConfiguration> instances, InstancesConfiguration requestInstance) {

    ServiceCollectConfiguration rootConfig = super.useResult(appId);

    List<ServiceCollectConfiguration> multiEnvConfigs = Optional.ofNullable(
        rootConfig.getMultiEnvConfigs()).orElse(Collections.emptyList());
    // no multi env config, all instance use root config
    if (CollectionUtils.isEmpty(multiEnvConfigs)) {
      return Pair.of(rootConfig, instances);
    }

    // index of config -> instances that will use the config
    Map<Integer, List<InstancesConfiguration>> configAllocation = instances.stream()
        .collect(Collectors.groupingBy(instance -> findConfigIndex(instance, multiEnvConfigs)));

    // selected config index of the incoming request instance
    Integer requestInstanceConfigIndex = findConfigIndex(requestInstance, multiEnvConfigs);
    // meaning using multi env config
    if (requestInstanceConfigIndex != -1) {
      ServiceCollectConfiguration envConfig = multiEnvConfigs.get(requestInstanceConfigIndex);
      // apply the same field-level inheritance to the env config so a per-env unset field falls
      // back to the global/built-in default instead of clobbering the root config with 0/null
      mergeGlobalDefaultSettings(envConfig);
      rootConfig.setSampleRate(envConfig.getSampleRate());
      rootConfig.setAllowDayOfWeeks(envConfig.getAllowDayOfWeeks());
      rootConfig.setAllowTimeOfDayFrom(envConfig.getAllowTimeOfDayFrom());
      rootConfig.setAllowTimeOfDayTo(envConfig.getAllowTimeOfDayTo());
      rootConfig.setRecordMachineCountLimit(envConfig.getRecordMachineCountLimit());
    }

    List<InstancesConfiguration> instancesOfEnv = configAllocation.get(requestInstanceConfigIndex);

    // clear multi env config to avoid confusion
    rootConfig.setMultiEnvConfigs(Collections.emptyList());
    return Pair.of(rootConfig, instancesOfEnv);
  }

  /**
   * Find the index of the config that matches the tags of the instance.
   * @param instance instance
   * @param multiEnvConfigs multi env configs
   * @return index of the config, -1 if not found
   */
  private Integer findConfigIndex(InstancesConfiguration instance,
      List<ServiceCollectConfiguration> multiEnvConfigs) {
    for (int i = 0; i < multiEnvConfigs.size(); i++) {
      ServiceCollectConfiguration envConfig = multiEnvConfigs.get(i);
      Map<String, List<String>> configEnv = envConfig.getEnvTags();
      if (configEnv == null || configEnv.isEmpty()) {
        // this should not happen, data is validated before saving
        LOGGER.error("Invalid multi env config, appid: {}", instance.getAppId());
        continue;
      }

      Map<String, String> serverTags = Optional.ofNullable(instance.getTags()).orElse(Collections.emptyMap());
      if (configEnv.keySet().stream().allMatch(tagKey -> {
        List<String> tagVals = configEnv.get(tagKey);
        return tagVals.contains(serverTags.get(tagKey));
      })) {
        return i;
      }
    }
    return -1;
  }

  public void updateServiceCollectTime(String appId) {
    ServiceCollectConfiguration serviceCollectConfiguration = this.useResult(appId);
    this.update(serviceCollectConfiguration);
  }

  @Configuration
  @ConfigurationProperties(prefix = "arex.config.default.service.collect")
  static class GlobalServiceCollectConfiguration extends ServiceCollectConfiguration {

  }
}
