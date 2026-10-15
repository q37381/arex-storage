package com.arextest.config.repository;

import com.arextest.config.model.dto.system.SystemConfiguration;
import com.arextest.config.system.GlobalConfigCodec;
import com.arextest.config.system.SystemConfigKey;
import java.util.List;

/**
 * @author wildeslam.
 * @create 2024/2/21 19:57
 */
public interface SystemConfigurationRepository {
  boolean saveConfig(SystemConfiguration systemConfig);

  List<SystemConfiguration> getAllSystemConfigList();

  SystemConfiguration getSystemConfigByKey(String key);

  boolean deleteConfig(String key);

  /**
   * Read a typed global-default configuration. The raw JSON stored under {@link
   * SystemConfigKey#key()} is deserialized to the key's declared type; {@code null} when the key is
   * absent or empty, so the caller can fall back to the built-in default.
   */
  default <T> T getGlobalConfig(SystemConfigKey<T> configKey) {
    SystemConfiguration config = getSystemConfigByKey(configKey.key());
    return config == null ? null : GlobalConfigCodec.read(config.getConfigValue(), configKey.type());
  }

  /**
   * Persist a typed global-default configuration as JSON under {@link SystemConfigKey#key()}.
   * Serializing to {@code null} (a null value) removes the key so resolvers fall back to the
   * built-in default.
   */
  default boolean saveGlobalConfig(SystemConfigKey<?> configKey, Object value) {
    String json = GlobalConfigCodec.write(value);
    if (json == null) {
      return deleteConfig(configKey.key());
    }
    SystemConfiguration config = new SystemConfiguration();
    config.setKey(configKey.key());
    config.setConfigValue(json);
    return saveConfig(config);
  }

}
