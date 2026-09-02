package com.arextest.storage.repository.impl.mysql.config;

import com.arextest.storage.repository.impl.mysql.config.ConfigEntities.System;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Mapper of config_system (key-value style system configuration).
 */
@Mapper
public interface ConfigSystemMapper {

  String COLUMNS = "id, config_key, refresh_task_mark, desensitization_jar, callback_url, "
      + "auth_switch, compare_plugin_info, jwt_seed, ignore_node_set, data_change_create_time, "
      + "data_change_update_time";

  @Select("SELECT " + COLUMNS + " FROM config_system")
  List<System> selectAll();

  @Select("SELECT " + COLUMNS + " FROM config_system WHERE config_key = #{key}")
  System selectByKey(@Param("key") String key);

  /**
   * Upsert by config_key. COALESCE on conflict keeps the stored value when the
   * incoming field is null, matching the null-skip semantics of the mongo
   * edition (MongoHelper.getFullTemplateUpdates).
   */
  @Insert("INSERT INTO config_system (id, config_key, refresh_task_mark, desensitization_jar, "
      + "callback_url, auth_switch, compare_plugin_info, jwt_seed, ignore_node_set, "
      + "data_change_create_time, data_change_update_time) VALUES "
      + "(#{it.id}, #{it.configKey}, #{it.refreshTaskMark}, #{it.desensitizationJar}, "
      + "#{it.callbackUrl}, #{it.authSwitch}, #{it.comparePluginInfo}, #{it.jwtSeed}, "
      + "#{it.ignoreNodeSet}, #{it.dataChangeCreateTime}, #{it.dataChangeUpdateTime}) "
      + "ON DUPLICATE KEY UPDATE "
      + "refresh_task_mark = COALESCE(VALUES(refresh_task_mark), refresh_task_mark), "
      + "desensitization_jar = COALESCE(VALUES(desensitization_jar), desensitization_jar), "
      + "callback_url = COALESCE(VALUES(callback_url), callback_url), "
      + "auth_switch = COALESCE(VALUES(auth_switch), auth_switch), "
      + "compare_plugin_info = COALESCE(VALUES(compare_plugin_info), compare_plugin_info), "
      + "jwt_seed = COALESCE(VALUES(jwt_seed), jwt_seed), "
      + "ignore_node_set = COALESCE(VALUES(ignore_node_set), ignore_node_set), "
      + "data_change_update_time = VALUES(data_change_update_time)")
  int upsert(@Param("it") System entity);

  @Delete("DELETE FROM config_system WHERE config_key = #{key}")
  int deleteByKey(@Param("key") String key);
}
