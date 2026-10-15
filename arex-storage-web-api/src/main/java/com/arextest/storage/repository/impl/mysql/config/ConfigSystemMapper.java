package com.arextest.storage.repository.impl.mysql.config;

import com.arextest.storage.repository.impl.mysql.config.ConfigEntities.System;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Mapper of config_system (pure key-value store).
 */
@Mapper
public interface ConfigSystemMapper {

  String COLUMNS = "id, config_key, config_value, data_change_create_time, data_change_update_time";

  @Select("SELECT " + COLUMNS + " FROM config_system")
  List<System> selectAll();

  @Select("SELECT " + COLUMNS + " FROM config_system WHERE config_key = #{key}")
  System selectByKey(@Param("key") String key);

  @Insert("INSERT INTO config_system (id, config_key, config_value, data_change_create_time, data_change_update_time) "
      + "VALUES (#{it.id}, #{it.configKey}, #{it.configValue}, #{it.dataChangeCreateTime}, #{it.dataChangeUpdateTime}) "
      + "ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), data_change_update_time = VALUES(data_change_update_time)")
  int upsert(@Param("it") System entity);

  @Delete("DELETE FROM config_system WHERE config_key = #{key}")
  int deleteByKey(@Param("key") String key);
}
