package com.arextest.storage.repository.impl.mysql.config;

import com.arextest.storage.repository.impl.mysql.config.ConfigEntities.RecordService;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * Mapper of config_record_service - the recording switch / sample rate store.
 */
@Mapper
public interface ConfigRecordServiceMapper {

  String COLUMNS = "id, app_id, sample_rate, allow_day_of_weeks, time_mock, "
      + "allow_time_of_day_from, allow_time_of_day_to, exclude_service_operation_set, "
      + "record_machine_count_limit, extend_field, serialize_skip_info_list, multi_env_configs, "
      + "env_tags, data_change_create_time, data_change_update_time";

  @Select("SELECT " + COLUMNS + " FROM config_record_service WHERE app_id = #{appId}")
  List<RecordService> selectByAppId(@Param("appId") String appId);

  // INSERT IGNORE: the update-then-insert flow may race with a concurrent first load
  @Insert("INSERT IGNORE INTO config_record_service (" + COLUMNS + ") VALUES "
      + "(#{it.id}, #{it.appId}, #{it.sampleRate}, #{it.allowDayOfWeeks}, #{it.timeMock}, "
      + "#{it.allowTimeOfDayFrom}, #{it.allowTimeOfDayTo}, #{it.excludeServiceOperationSet}, "
      + "#{it.recordMachineCountLimit}, #{it.extendField}, #{it.serializeSkipInfoList}, "
      + "#{it.multiEnvConfigs}, #{it.envTags}, #{it.dataChangeCreateTime}, "
      + "#{it.dataChangeUpdateTime})")
  int insert(@Param("it") RecordService entity);

  /**
   * Null-skip update of the whitelisted columns, matching
   * MongoHelper.getMongoTemplateUpdates semantics. recordMachineCountLimit is
   * always set (repository converts null to 1 like the mongo edition).
   */
  @Update("<script>UPDATE config_record_service <set>"
      + "<if test='it.sampleRate != null'>sample_rate = #{it.sampleRate},</if>"
      + "<if test='it.allowDayOfWeeks != null'>allow_day_of_weeks = #{it.allowDayOfWeeks},</if>"
      + "<if test='it.allowTimeOfDayFrom != null'>allow_time_of_day_from = #{it.allowTimeOfDayFrom},</if>"
      + "<if test='it.allowTimeOfDayTo != null'>allow_time_of_day_to = #{it.allowTimeOfDayTo},</if>"
      + "<if test='it.excludeServiceOperationSet != null'>exclude_service_operation_set = #{it.excludeServiceOperationSet},</if>"
      + "<if test='it.timeMock != null'>time_mock = #{it.timeMock},</if>"
      + "<if test='it.extendField != null'>extend_field = #{it.extendField},</if>"
      + "<if test='it.serializeSkipInfoList != null'>serialize_skip_info_list = #{it.serializeSkipInfoList},</if>"
      + "record_machine_count_limit = #{it.recordMachineCountLimit},"
      + "data_change_update_time = #{now}"
      + "</set> WHERE app_id = #{it.appId}</script>")
  int updateByAppId(@Param("it") RecordService entity, @Param("now") long now);

  @Update("UPDATE config_record_service SET multi_env_configs = #{multiEnvConfigs}, "
      + "data_change_update_time = #{now} WHERE app_id = #{appId}")
  int updateMultiEnvConfigs(@Param("appId") String appId,
      @Param("multiEnvConfigs") String multiEnvConfigs, @Param("now") long now);

  @Update("UPDATE config_record_service SET data_change_update_time = #{now} "
      + "WHERE app_id = #{appId}")
  int touchModifiedTime(@Param("appId") String appId, @Param("now") long now);

  @Delete("DELETE FROM config_record_service WHERE app_id = #{appId}")
  int deleteByAppId(@Param("appId") String appId);
}
