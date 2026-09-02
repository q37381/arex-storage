package com.arextest.storage.repository.impl.mysql.config;

import com.arextest.storage.repository.impl.mysql.config.ConfigEntities.Application;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * Mapper of config_application. seq (auto increment) stands in for mongo's
 * ObjectId ordering: list() shows newest first, matching _id DESC.
 */
@Mapper
public interface ConfigApplicationMapper {

  String COLUMNS = "seq, id, app_id, features, group_name, group_id, agent_version, "
      + "agent_ext_version, app_name, description, category, owner, owners, organization_name, "
      + "recorded_case_count, organization_id, status, visibility_level, tags, "
      + "data_change_create_time, data_change_update_time";

  @Select("SELECT " + COLUMNS + " FROM config_application ORDER BY seq DESC")
  List<Application> selectAll();

  @Select("SELECT " + COLUMNS + " FROM config_application WHERE app_id = #{appId}")
  List<Application> selectByAppId(@Param("appId") String appId);

  // INSERT IGNORE: concurrent first-load of the same app must not fail
  @Insert("INSERT IGNORE INTO config_application (id, app_id, features, group_name, group_id, "
      + "agent_version, agent_ext_version, app_name, description, category, owner, owners, "
      + "organization_name, recorded_case_count, organization_id, status, visibility_level, tags, "
      + "data_change_create_time, data_change_update_time) VALUES "
      + "(#{it.id}, #{it.appId}, #{it.features}, #{it.groupName}, #{it.groupId}, "
      + "#{it.agentVersion}, #{it.agentExtVersion}, #{it.appName}, #{it.description}, "
      + "#{it.category}, #{it.owner}, #{it.owners}, #{it.organizationName}, "
      + "#{it.recordedCaseCount}, #{it.organizationId}, #{it.status}, #{it.visibilityLevel}, "
      + "#{it.tags}, #{it.dataChangeCreateTime}, #{it.dataChangeUpdateTime})")
  int insert(@Param("it") Application entity);

  /**
   * Whitelisted column update, mirrors the mongo edition (agent versions,
   * status, features, appName, owners, visibilityLevel, tags). Null fields
   * are skipped, matching MongoHelper.getMongoTemplateUpdates semantics.
   */
  @Update("<script>UPDATE config_application <set>"
      + "<if test='it.agentVersion != null'>agent_version = #{it.agentVersion},</if>"
      + "<if test='it.agentExtVersion != null'>agent_ext_version = #{it.agentExtVersion},</if>"
      + "<if test='it.status != null'>status = #{it.status},</if>"
      + "<if test='it.features != null'>features = #{it.features},</if>"
      + "<if test='it.appName != null'>app_name = #{it.appName},</if>"
      + "<if test='it.owners != null'>owners = #{it.owners},</if>"
      + "<if test='it.visibilityLevel != null'>visibility_level = #{it.visibilityLevel},</if>"
      + "<if test='it.tags != null'>tags = #{it.tags},</if>"
      + "data_change_update_time = #{now}"
      + "</set> WHERE app_id = #{it.appId}</script>")
  int updateByAppId(@Param("it") Application entity, @Param("now") long now);

  @Update("UPDATE config_application SET tags = #{tags}, data_change_update_time = #{now} "
      + "WHERE app_id = #{appId}")
  int updateTags(@Param("appId") String appId, @Param("tags") String tags, @Param("now") long now);

  @Delete("DELETE FROM config_application WHERE app_id = #{appId}")
  int deleteByAppId(@Param("appId") String appId);
}
