package com.arextest.storage.repository.impl.mysql.config;

import lombok.Data;

/**
 * Flat rows of the config tables. Complex mongo sub documents (maps, sets,
 * nested objects) are kept as JSON strings here and decoded in the repository
 * layer via {@link ConfigJsonSupport}.
 */
public final class ConfigEntities {

  private ConfigEntities() {
  }

  @Data
  public static class Application {

    private Long seq;
    private String id;
    private String appId;
    private Integer features;
    private String groupName;
    private String groupId;
    private String agentVersion;
    private String agentExtVersion;
    private String appName;
    private String description;
    private String category;
    private String owner;
    private String owners;
    private String organizationName;
    private Integer recordedCaseCount;
    private String organizationId;
    private Integer status;
    private Integer visibilityLevel;
    private String tags;
    private Long dataChangeCreateTime;
    private Long dataChangeUpdateTime;
  }

  @Data
  public static class Service {

    private String id;
    private String appId;
    private String serviceName;
    private String serviceKey;
    private Integer status;
    private Long dataChangeCreateTime;
    private Long dataChangeUpdateTime;
  }

  @Data
  public static class ServiceOperation {

    private String id;
    private String appId;
    private String serviceId;
    private String operationName;
    private String operationResponse;
    private String operationType;
    private String operationTypes;
    private Integer recordedCaseCount;
    private Integer status;
    private Long dataChangeCreateTime;
    private Long dataChangeUpdateTime;
  }

  @Data
  public static class RecordService {

    private String id;
    private String appId;
    private Integer sampleRate;
    private Integer allowDayOfWeeks;
    private Boolean timeMock;
    private String allowTimeOfDayFrom;
    private String allowTimeOfDayTo;
    private String excludeServiceOperationSet;
    private Integer recordMachineCountLimit;
    private String extendField;
    private String serializeSkipInfoList;
    private String multiEnvConfigs;
    private String envTags;
    private Long dataChangeCreateTime;
    private Long dataChangeUpdateTime;
  }

  @Data
  public static class DynamicClass {

    private String id;
    private String appId;
    private String fullClassName;
    private String methodName;
    private String parameterTypes;
    private Integer configType;
    private String keyFormula;
    private Long dataChangeCreateTime;
    private Long dataChangeUpdateTime;
  }

  @Data
  public static class Instances {

    private Long seq;
    private String id;
    private String appId;
    private String host;
    private String recordVersion;
    private Long dataUpdateTime;
    private String agentStatus;
    private String tags;
    private String systemEnv;
    private String systemProperties;
    private String extendField;
    private Long dataChangeCreateTime;
    private Long dataChangeUpdateTime;
  }

  @Data
  public static class System {

    private String id;
    private String configKey;
    private String refreshTaskMark;
    private String desensitizationJar;
    private String callbackUrl;
    private Boolean authSwitch;
    private String comparePluginInfo;
    private String jwtSeed;
    private String ignoreNodeSet;
    private Long dataChangeCreateTime;
    private Long dataChangeUpdateTime;
  }

  @Data
  public static class ComparisonExclusions {

    private String id;
    private String appId;
    private String operationId;
    private Integer expirationType;
    private Long expirationDate;
    private Integer compareConfigType;
    private String fsInterfaceId;
    private String dependencyId;
    private String exclusions;
    private Long dataChangeCreateTime;
    private Long dataChangeUpdateTime;
  }
}
