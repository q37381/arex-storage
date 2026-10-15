package com.arextest.config.model.dao.config;

import com.arextest.config.model.dao.BaseEntity;
import com.arextest.config.model.dto.system.ComparePluginInfo;
import com.arextest.config.model.dto.system.DesensitizationJar;
import java.util.Map;
import java.util.Set;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author wildeslam.
 * @create 2024/2/21 19:43
 */
@Data
@FieldNameConstants
@Document(SystemConfigurationCollection.DOCUMENT_NAME)
public class SystemConfigurationCollection extends BaseEntity {

  public static final String DOCUMENT_NAME = "SystemConfiguration";

  /**
   * The problem of prohibiting concurrent repeated insertions, the key is unique the function of
   * this record
   */
  private String key;
  private Map<String, Integer> refreshTaskMark;
  private DesensitizationJar desensitizationJar;
  private String callbackUrl;
  private Boolean authSwitch;
  private ComparePluginInfo comparePluginInfo;
  private String jwtSeed;
  private Set<String> ignoreNodeSet;
  private String configValue;


  public interface KeySummary {
    String REFRESH_DATA = "refresh_data";
    String DESERIALIZATION_JAR = "deserialization_jar";
    String CALLBACK_URL = "callback_url";
    String AUTH_SWITCH = "auth_switch";
    String COMPARE_PLUGIN_INFO = "compare_plugin_info";
    String JWT_SEED = "jwt_seed";
    String IGNORE_NODE_SET = "ignore_node_set";
    String AGENT_GLOBAL_CONFIG = "agent_global_config";
    /**
     * Globally configurable default values, each stored as a JSON document in config_value.
     * When a per-app config leaves a field unset, the effective value is resolved as
     * app config > global default (these keys) > built-in YAML default.
     */
    String RECORD_GLOBAL_DEFAULT = "record_global_default";
    String DYNAMIC_CLASS_GLOBAL_DEFAULT = "dynamic_class_global_default";
    String REPLAY_GLOBAL_DEFAULT = "replay_global_default";
    String COMPARE_GLOBAL_DEFAULT = "compare_global_default";
    /**
     * Allow-list of accounts allowed to write the globally effective configuration, an empty or
     * absent list leaves the platform unguarded.
     */
    String ADMIN_USERS = "admin_users";
  }
}
