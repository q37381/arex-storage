package com.arextest.storage.repository.impl.mysql.config;

import com.arextest.storage.repository.impl.mysql.config.ConfigEntities.ComparisonExclusions;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Mapper of config_comparison_exclusions. Read-only for the storage service,
 * mirroring the mongo edition (update/remove/insert return false there).
 */
@Mapper
public interface ConfigComparisonExclusionsMapper {

  String COLUMNS = "id, app_id, operation_id, expiration_type, expiration_date, "
      + "compare_config_type, fs_interface_id, dependency_id, exclusions, "
      + "data_change_create_time, data_change_update_time";

  @Select("SELECT " + COLUMNS + " FROM config_comparison_exclusions WHERE app_id = #{appId}")
  List<ComparisonExclusions> selectByAppId(@Param("appId") String appId);

  @Select("SELECT " + COLUMNS + " FROM config_comparison_exclusions "
      + "WHERE app_id = #{appId} AND compare_config_type = #{compareConfigType}")
  List<ComparisonExclusions> selectByAppIdAndType(@Param("appId") String appId,
      @Param("compareConfigType") int compareConfigType);
}
