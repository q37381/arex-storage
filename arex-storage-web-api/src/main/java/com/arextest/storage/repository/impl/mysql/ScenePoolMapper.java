package com.arextest.storage.repository.impl.mysql;

import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * MyBatis mapper of the scene_pool table. Pool-scoped queries carry the
 * providerName ("Recording" / "Replay") so one table serves both pools.
 * Time columns are epoch millis; conversion to java.util.Date happens in
 * the provider.
 */
@Mapper
public interface ScenePoolMapper {

  String COLUMNS = "id, provider_name, scene_key, app_id, record_id, execution_path, "
      + "creation_time, update_time, expiration_time";

  @Select("SELECT COUNT(1) FROM scene_pool "
      + "WHERE provider_name = #{provider} AND app_id = #{appId} AND scene_key = #{sceneKey}")
  long countBySceneKey(@Param("provider") String provider, @Param("appId") String appId,
      @Param("sceneKey") String sceneKey);

  @Select("SELECT " + COLUMNS + " FROM scene_pool "
      + "WHERE provider_name = #{provider} AND app_id = #{appId} AND scene_key = #{sceneKey}")
  ScenePoolEntity selectBySceneKey(@Param("provider") String provider, @Param("appId") String appId,
      @Param("sceneKey") String sceneKey);

  @Select("SELECT " + COLUMNS + " FROM scene_pool "
      + "WHERE provider_name = #{provider} AND record_id = #{recordId} LIMIT 1")
  ScenePoolEntity selectByRecordId(@Param("provider") String provider,
      @Param("recordId") String recordId);

  @Select("SELECT record_id FROM scene_pool "
      + "WHERE provider_name = #{provider} AND app_id = #{appId} "
      + "ORDER BY id ASC LIMIT #{offset}, #{limit}")
  List<String> selectRecordIdsByAppId(@Param("provider") String provider,
      @Param("appId") String appId, @Param("offset") int offset, @Param("limit") int limit);

  @Select("SELECT COUNT(1) FROM scene_pool "
      + "WHERE provider_name = #{provider} AND app_id = #{appId}")
  long countByAppId(@Param("provider") String provider, @Param("appId") String appId);

  /**
   * Upsert keyed by the (provider_name, app_id, scene_key) unique index.
   * ON DUPLICATE KEY UPDATE keeps the original id and creation_time, matching
   * mongo's setOnInsert behaviour.
   */
  @Insert("INSERT INTO scene_pool (" + COLUMNS + ") VALUES "
      + "(#{it.id}, #{it.providerName}, #{it.sceneKey}, #{it.appId}, #{it.recordId}, "
      + "#{it.executionPath}, #{it.creationTime}, #{it.updateTime}, #{it.expirationTime}) "
      + "ON DUPLICATE KEY UPDATE "
      + "record_id = VALUES(record_id), execution_path = VALUES(execution_path), "
      + "update_time = VALUES(update_time), expiration_time = VALUES(expiration_time)")
  int upsert(@Param("it") ScenePoolEntity entity);

  @Delete("DELETE FROM scene_pool WHERE provider_name = #{provider} AND app_id = #{appId}")
  long deleteByAppId(@Param("provider") String provider, @Param("appId") String appId);

  @Delete("DELETE FROM scene_pool WHERE expiration_time < #{now}")
  long deleteExpired(@Param("now") long now);
}
