package com.arextest.storage.repository.impl.mysql.config;

import com.arextest.storage.repository.impl.mysql.config.ConfigEntities.Instances;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Mapper of config_instances. auto_id (auto increment) stands in for mongo's _id
 * ordering used by the record-machine allocation logic (ASC).
 */
@Mapper
public interface ConfigInstancesMapper {

  String COLUMNS = "auto_id, id, app_id, host, record_version, data_update_time, agent_status, "
      + "tags, system_env, system_properties, extend_field, data_change_create_time, "
      + "data_change_update_time";

  @Select("SELECT " + COLUMNS + " FROM config_instances WHERE app_id = #{appId}")
  List<Instances> selectByAppId(@Param("appId") String appId);

  @Select("SELECT " + COLUMNS + " FROM config_instances WHERE app_id = #{appId} ORDER BY auto_id ASC")
  List<Instances> selectByAppIdOrdered(@Param("appId") String appId);

  /**
   * Upsert by (app_id, host). On conflict only the reported columns are
   * overwritten; COALESCE keeps the stored value when the report carries null,
   * matching the null-skip semantics of MongoHelper.getFullTemplateUpdates.
   */
  @Insert("INSERT INTO config_instances (id, app_id, host, record_version, data_update_time, "
      + "agent_status, tags, system_env, system_properties, extend_field, "
      + "data_change_create_time, data_change_update_time) VALUES "
      + "(#{it.id}, #{it.appId}, #{it.host}, #{it.recordVersion}, #{it.dataUpdateTime}, "
      + "#{it.agentStatus}, #{it.tags}, #{it.systemEnv}, #{it.systemProperties}, "
      + "#{it.extendField}, #{it.dataChangeCreateTime}, #{it.dataChangeUpdateTime}) "
      + "ON DUPLICATE KEY UPDATE "
      + "record_version = COALESCE(VALUES(record_version), record_version), "
      + "data_update_time = VALUES(data_update_time), "
      + "agent_status = COALESCE(VALUES(agent_status), agent_status), "
      + "tags = COALESCE(VALUES(tags), tags), "
      + "system_env = COALESCE(VALUES(system_env), system_env), "
      + "system_properties = COALESCE(VALUES(system_properties), system_properties), "
      + "extend_field = COALESCE(VALUES(extend_field), extend_field), "
      + "data_change_update_time = VALUES(data_change_update_time)")
  int upsert(@Param("it") Instances entity);

  @Delete("DELETE FROM config_instances WHERE app_id = #{appId}")
  int deleteByAppId(@Param("appId") String appId);

  @Delete("DELETE FROM config_instances WHERE app_id = #{appId} AND host = #{host}")
  int deleteByAppIdAndHost(@Param("appId") String appId, @Param("host") String host);
}
