package com.arextest.storage.repository.impl.mysql.config;

import com.arextest.config.model.dto.ComparisonExclusionsConfiguration;
import com.arextest.config.model.dto.application.ApplicationConfiguration;
import com.arextest.config.model.dto.application.ApplicationOperationConfiguration;
import com.arextest.config.model.dto.application.ApplicationServiceConfiguration;
import com.arextest.config.model.dto.application.InstancesConfiguration;
import com.arextest.config.model.dto.record.DynamicClassConfiguration;
import com.arextest.config.model.dto.record.SerializeSkipInfoConfiguration;
import com.arextest.config.model.dto.record.ServiceCollectConfiguration;
import com.arextest.config.model.dto.system.ComparePluginInfo;
import com.arextest.config.model.dto.system.DesensitizationJar;
import com.arextest.config.model.dto.system.SystemConfiguration;
import com.arextest.storage.repository.impl.mysql.config.ConfigEntities.Application;
import com.arextest.storage.repository.impl.mysql.config.ConfigEntities.ComparisonExclusions;
import com.arextest.storage.repository.impl.mysql.config.ConfigEntities.DynamicClass;
import com.arextest.storage.repository.impl.mysql.config.ConfigEntities.Instances;
import com.arextest.storage.repository.impl.mysql.config.ConfigEntities.RecordService;
import com.arextest.storage.repository.impl.mysql.config.ConfigEntities.Service;
import com.arextest.storage.repository.impl.mysql.config.ConfigEntities.ServiceOperation;
import com.arextest.storage.repository.impl.mysql.config.ConfigEntities.System;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

/**
 * Conversions between the flat mysql rows ({@link ConfigEntities}) and the
 * config DTOs consumed by the service layer. Mirrors the MapStruct mappers of
 * the mongo edition, including their quirks:
 * <ul>
 *   <li>modifiedTime is derived from dataChangeUpdateTime</li>
 *   <li>ServiceCollect: recordMachineCountLimit defaults to 1, timeMock is
 *   always reported as true to agents (see RecordServiceConfigMapper)</li>
 *   <li>operationBaseInfoList hides operationResponse</li>
 * </ul>
 */
final class ConfigConverters {

  private static final TypeReference<Set<String>> STRING_SET = new TypeReference<Set<String>>() {
  };
  private static final TypeReference<List<String>> STRING_LIST =
      new TypeReference<List<String>>() {
      };
  private static final TypeReference<Map<String, String>> STRING_MAP =
      new TypeReference<Map<String, String>>() {
      };
  private static final TypeReference<Map<String, Integer>> STRING_INT_MAP =
      new TypeReference<Map<String, Integer>>() {
      };
  private static final TypeReference<Map<String, Set<String>>> TAGS_MAP =
      new TypeReference<Map<String, Set<String>>>() {
      };
  private static final TypeReference<Map<String, List<String>>> ENV_TAGS_MAP =
      new TypeReference<Map<String, List<String>>>() {
      };
  private static final TypeReference<List<SerializeSkipInfoConfiguration>> SKIP_INFO_LIST =
      new TypeReference<List<SerializeSkipInfoConfiguration>>() {
      };
  private static final TypeReference<List<ServiceCollectConfiguration>> MULTI_ENV_LIST =
      new TypeReference<List<ServiceCollectConfiguration>>() {
      };

  private ConfigConverters() {
  }

  // ---------------- application ----------------

  static ApplicationConfiguration toDto(Application entity) {
    if (entity == null) {
      return null;
    }
    ApplicationConfiguration dto = new ApplicationConfiguration();
    dto.setId(entity.getId());
    dto.setAppId(entity.getAppId());
    dto.setFeatures(entity.getFeatures() == null ? 0 : entity.getFeatures());
    dto.setGroupName(entity.getGroupName());
    dto.setGroupId(entity.getGroupId());
    dto.setAgentVersion(entity.getAgentVersion());
    dto.setAgentExtVersion(entity.getAgentExtVersion());
    dto.setAppName(entity.getAppName());
    dto.setDescription(entity.getDescription());
    dto.setCategory(entity.getCategory());
    dto.setOwner(entity.getOwner());
    dto.setOwners(ConfigJsonSupport.readJson(entity.getOwners(), STRING_SET));
    dto.setOrganizationName(entity.getOrganizationName());
    dto.setRecordedCaseCount(entity.getRecordedCaseCount());
    dto.setOrganizationId(entity.getOrganizationId());
    dto.setStatus(entity.getStatus());
    dto.setVisibilityLevel(entity.getVisibilityLevel() == null ? 0 : entity.getVisibilityLevel());
    dto.setTags(ConfigJsonSupport.readJson(entity.getTags(), TAGS_MAP));
    dto.setModifiedTime(ConfigJsonSupport.toTimestamp(entity.getDataChangeUpdateTime()));
    return dto;
  }

  static Application fromDto(ApplicationConfiguration dto) {
    Application entity = new Application();
    entity.setId(StringUtils.isBlank(dto.getId()) ? ConfigJsonSupport.newId() : dto.getId());
    entity.setAppId(dto.getAppId());
    entity.setFeatures(dto.getFeatures());
    entity.setGroupName(dto.getGroupName());
    entity.setGroupId(dto.getGroupId());
    entity.setAgentVersion(dto.getAgentVersion());
    entity.setAgentExtVersion(dto.getAgentExtVersion());
    entity.setAppName(dto.getAppName());
    entity.setDescription(dto.getDescription());
    entity.setCategory(dto.getCategory());
    entity.setOwner(dto.getOwner());
    entity.setOwners(ConfigJsonSupport.writeJson(dto.getOwners()));
    entity.setOrganizationName(dto.getOrganizationName());
    entity.setRecordedCaseCount(dto.getRecordedCaseCount());
    entity.setOrganizationId(dto.getOrganizationId());
    entity.setStatus(dto.getStatus());
    entity.setVisibilityLevel(dto.getVisibilityLevel());
    entity.setTags(ConfigJsonSupport.writeJson(dto.getTags()));
    return entity;
  }

  // ---------------- service ----------------

  static ApplicationServiceConfiguration toDto(Service entity) {
    if (entity == null) {
      return null;
    }
    ApplicationServiceConfiguration dto = new ApplicationServiceConfiguration();
    dto.setId(entity.getId());
    dto.setAppId(entity.getAppId());
    dto.setServiceName(entity.getServiceName());
    dto.setServiceKey(entity.getServiceKey());
    dto.setStatus(entity.getStatus());
    dto.setModifiedTime(ConfigJsonSupport.toTimestamp(entity.getDataChangeUpdateTime()));
    return dto;
  }

  static Service fromDto(ApplicationServiceConfiguration dto) {
    Service entity = new Service();
    entity.setId(StringUtils.isBlank(dto.getId()) ? ConfigJsonSupport.newId() : dto.getId());
    entity.setAppId(dto.getAppId());
    entity.setServiceName(dto.getServiceName());
    entity.setServiceKey(dto.getServiceKey());
    entity.setStatus(dto.getStatus());
    return entity;
  }

  // ---------------- service operation ----------------

  static ApplicationOperationConfiguration toDto(ServiceOperation entity) {
    return toDto(entity, false);
  }

  static ApplicationOperationConfiguration toBaseInfo(ServiceOperation entity) {
    return toDto(entity, true);
  }

  private static ApplicationOperationConfiguration toDto(ServiceOperation entity,
      boolean baseInfo) {
    if (entity == null) {
      return null;
    }
    ApplicationOperationConfiguration dto = new ApplicationOperationConfiguration();
    dto.setId(entity.getId());
    dto.setAppId(entity.getAppId());
    dto.setServiceId(entity.getServiceId());
    dto.setOperationName(entity.getOperationName());
    dto.setOperationResponse(baseInfo ? null : entity.getOperationResponse());
    dto.setOperationType(entity.getOperationType());
    dto.setOperationTypes(ConfigJsonSupport.readJson(entity.getOperationTypes(), STRING_SET));
    dto.setRecordedCaseCount(entity.getRecordedCaseCount());
    dto.setStatus(entity.getStatus());
    dto.setModifiedTime(ConfigJsonSupport.toTimestamp(entity.getDataChangeUpdateTime()));
    return dto;
  }

  static ServiceOperation fromDto(ApplicationOperationConfiguration dto) {
    ServiceOperation entity = new ServiceOperation();
    entity.setId(StringUtils.isBlank(dto.getId()) ? ConfigJsonSupport.newId() : dto.getId());
    entity.setAppId(dto.getAppId());
    entity.setServiceId(dto.getServiceId());
    entity.setOperationName(dto.getOperationName());
    entity.setOperationResponse(dto.getOperationResponse());
    entity.setOperationType(dto.getOperationType());
    entity.setOperationTypes(ConfigJsonSupport.writeJson(dto.getOperationTypes()));
    entity.setRecordedCaseCount(dto.getRecordedCaseCount());
    entity.setStatus(dto.getStatus());
    return entity;
  }

  // ---------------- record service (recording switch) ----------------

  static ServiceCollectConfiguration toDto(RecordService entity) {
    if (entity == null) {
      return null;
    }
    ServiceCollectConfiguration dto = new ServiceCollectConfiguration();
    dto.setAppId(entity.getAppId());
    dto.setSampleRate(entity.getSampleRate() == null ? 0 : entity.getSampleRate());
    dto.setAllowDayOfWeeks(entity.getAllowDayOfWeeks() == null ? 0 : entity.getAllowDayOfWeeks());
    // quirk of the mongo edition: agents always read timeMock as true
    dto.setTimeMock(true);
    dto.setAllowTimeOfDayFrom(entity.getAllowTimeOfDayFrom());
    dto.setAllowTimeOfDayTo(entity.getAllowTimeOfDayTo());
    dto.setExcludeServiceOperationSet(
        ConfigJsonSupport.readJson(entity.getExcludeServiceOperationSet(), STRING_SET));
    dto.setRecordMachineCountLimit(
        entity.getRecordMachineCountLimit() == null ? 1 : entity.getRecordMachineCountLimit());
    dto.setExtendField(ConfigJsonSupport.readJson(entity.getExtendField(), STRING_MAP));
    dto.setSerializeSkipInfoList(
        ConfigJsonSupport.readJson(entity.getSerializeSkipInfoList(), SKIP_INFO_LIST));
    dto.setMultiEnvConfigs(
        ConfigJsonSupport.readJson(entity.getMultiEnvConfigs(), MULTI_ENV_LIST));
    dto.setEnvTags(ConfigJsonSupport.readJson(entity.getEnvTags(), ENV_TAGS_MAP));
    dto.setModifiedTime(ConfigJsonSupport.toTimestamp(entity.getDataChangeUpdateTime()));
    return dto;
  }

  static RecordService fromDto(ServiceCollectConfiguration dto) {
    RecordService entity = new RecordService();
    entity.setId(ConfigJsonSupport.newId());
    entity.setAppId(dto.getAppId());
    entity.setSampleRate(dto.getSampleRate());
    entity.setAllowDayOfWeeks(dto.getAllowDayOfWeeks());
    entity.setTimeMock(dto.isTimeMock());
    entity.setAllowTimeOfDayFrom(dto.getAllowTimeOfDayFrom());
    entity.setAllowTimeOfDayTo(dto.getAllowTimeOfDayTo());
    entity.setExcludeServiceOperationSet(
        ConfigJsonSupport.writeJson(dto.getExcludeServiceOperationSet()));
    entity.setRecordMachineCountLimit(dto.getRecordMachineCountLimit());
    entity.setExtendField(ConfigJsonSupport.writeJson(dto.getExtendField()));
    entity.setSerializeSkipInfoList(ConfigJsonSupport.writeJson(dto.getSerializeSkipInfoList()));
    entity.setMultiEnvConfigs(ConfigJsonSupport.writeJson(dto.getMultiEnvConfigs()));
    entity.setEnvTags(ConfigJsonSupport.writeJson(dto.getEnvTags()));
    return entity;
  }

  // ---------------- dynamic class ----------------

  static DynamicClassConfiguration toDto(DynamicClass entity) {
    if (entity == null) {
      return null;
    }
    DynamicClassConfiguration dto = new DynamicClassConfiguration();
    dto.setId(entity.getId());
    dto.setAppId(entity.getAppId());
    dto.setFullClassName(entity.getFullClassName());
    dto.setMethodName(entity.getMethodName());
    dto.setParameterTypes(entity.getParameterTypes());
    dto.setConfigType(entity.getConfigType() == null ? 0 : entity.getConfigType());
    dto.setKeyFormula(entity.getKeyFormula());
    dto.setModifiedTime(ConfigJsonSupport.toTimestamp(entity.getDataChangeUpdateTime()));
    return dto;
  }

  static DynamicClass fromDto(DynamicClassConfiguration dto) {
    DynamicClass entity = new DynamicClass();
    entity.setId(StringUtils.isBlank(dto.getId()) ? ConfigJsonSupport.newId() : dto.getId());
    entity.setAppId(dto.getAppId());
    entity.setFullClassName(dto.getFullClassName());
    entity.setMethodName(dto.getMethodName());
    entity.setParameterTypes(dto.getParameterTypes());
    entity.setConfigType(dto.getConfigType());
    entity.setKeyFormula(dto.getKeyFormula());
    return entity;
  }

  // ---------------- instances ----------------

  static InstancesConfiguration toDto(Instances entity) {
    if (entity == null) {
      return null;
    }
    InstancesConfiguration dto = new InstancesConfiguration();
    dto.setId(entity.getId());
    dto.setAppId(entity.getAppId());
    dto.setHost(entity.getHost());
    dto.setRecordVersion(entity.getRecordVersion());
    dto.setDataUpdateTime(ConfigJsonSupport.toDate(entity.getDataUpdateTime()));
    dto.setAgentStatus(entity.getAgentStatus());
    dto.setTags(ConfigJsonSupport.readJson(entity.getTags(), STRING_MAP));
    dto.setSystemEnv(ConfigJsonSupport.readJson(entity.getSystemEnv(), STRING_MAP));
    dto.setSystemProperties(ConfigJsonSupport.readJson(entity.getSystemProperties(), STRING_MAP));
    dto.setExtendField(ConfigJsonSupport.readJson(entity.getExtendField(), STRING_MAP));
    dto.setModifiedTime(ConfigJsonSupport.toTimestamp(entity.getDataChangeUpdateTime()));
    return dto;
  }

  static Instances fromDto(InstancesConfiguration dto) {
    Instances entity = new Instances();
    entity.setId(StringUtils.isBlank(dto.getId()) ? ConfigJsonSupport.newId() : dto.getId());
    entity.setAppId(dto.getAppId());
    // host is part of the upsert unique key uk_app_host and declared NOT NULL
    // DEFAULT ''; agent reports may omit it (mongo stored the field absent).
    entity.setHost(dto.getHost() == null ? "" : dto.getHost());
    entity.setRecordVersion(dto.getRecordVersion());
    entity.setDataUpdateTime(ConfigJsonSupport.toMillis(dto.getDataUpdateTime()));
    entity.setAgentStatus(dto.getAgentStatus());
    entity.setTags(ConfigJsonSupport.writeJson(dto.getTags()));
    entity.setSystemEnv(ConfigJsonSupport.writeJson(dto.getSystemEnv()));
    entity.setSystemProperties(ConfigJsonSupport.writeJson(dto.getSystemProperties()));
    entity.setExtendField(ConfigJsonSupport.writeJson(dto.getExtendField()));
    return entity;
  }

  // ---------------- system configuration ----------------

  static SystemConfiguration toDto(System entity) {
    if (entity == null) {
      return null;
    }
    SystemConfiguration dto = new SystemConfiguration();
    dto.setKey(entity.getConfigKey());
    dto.setRefreshTaskMark(ConfigJsonSupport.readJson(entity.getRefreshTaskMark(),
        STRING_INT_MAP));
    dto.setDesensitizationJar(ConfigJsonSupport.readJson(entity.getDesensitizationJar(),
        DesensitizationJar.class));
    dto.setCallbackUrl(entity.getCallbackUrl());
    dto.setAuthSwitch(entity.getAuthSwitch());
    dto.setComparePluginInfo(ConfigJsonSupport.readJson(entity.getComparePluginInfo(),
        ComparePluginInfo.class));
    dto.setJwtSeed(entity.getJwtSeed());
    dto.setIgnoreNodeSet(ConfigJsonSupport.readJson(entity.getIgnoreNodeSet(), STRING_SET));
    return dto;
  }

  static System fromDto(SystemConfiguration dto) {
    System entity = new System();
    entity.setId(ConfigJsonSupport.newId());
    entity.setConfigKey(dto.getKey());
    entity.setRefreshTaskMark(ConfigJsonSupport.writeJson(dto.getRefreshTaskMark()));
    entity.setDesensitizationJar(ConfigJsonSupport.writeJson(dto.getDesensitizationJar()));
    entity.setCallbackUrl(dto.getCallbackUrl());
    entity.setAuthSwitch(dto.getAuthSwitch());
    entity.setComparePluginInfo(ConfigJsonSupport.writeJson(dto.getComparePluginInfo()));
    entity.setJwtSeed(dto.getJwtSeed());
    entity.setIgnoreNodeSet(ConfigJsonSupport.writeJson(dto.getIgnoreNodeSet()));
    return entity;
  }

  // ---------------- comparison exclusions ----------------

  static ComparisonExclusionsConfiguration toDto(ComparisonExclusions entity) {
    if (entity == null) {
      return null;
    }
    ComparisonExclusionsConfiguration dto = new ComparisonExclusionsConfiguration();
    dto.setId(entity.getId());
    dto.setAppId(entity.getAppId());
    dto.setOperationId(entity.getOperationId());
    dto.setExpirationType(entity.getExpirationType() == null ? 0 : entity.getExpirationType());
    dto.setExpirationDate(ConfigJsonSupport.toDate(entity.getExpirationDate()));
    dto.setCompareConfigType(
        entity.getCompareConfigType() == null ? 0 : entity.getCompareConfigType());
    dto.setFsInterfaceId(entity.getFsInterfaceId());
    dto.setDependencyId(entity.getDependencyId());
    dto.setExclusions(ConfigJsonSupport.readJson(entity.getExclusions(), STRING_LIST));
    dto.setModifiedTime(ConfigJsonSupport.toTimestamp(entity.getDataChangeUpdateTime()));
    return dto;
  }
}
