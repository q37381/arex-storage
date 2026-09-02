package com.arextest.storage.repository.impl.mysql.config;

import com.arextest.storage.repository.impl.mysql.config.ConfigEntities.Service;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * Mapper of config_service (one row per appId).
 */
@Mapper
public interface ConfigServiceMapper {

  String COLUMNS = "id, app_id, service_name, service_key, status, "
      + "data_change_create_time, data_change_update_time";

  @Select("SELECT " + COLUMNS + " FROM config_service WHERE app_id = #{appId}")
  List<Service> selectByAppId(@Param("appId") String appId);

  @Select("SELECT COUNT(1) FROM config_service WHERE app_id = #{appId}")
  long countByAppId(@Param("appId") String appId);

  @Insert("INSERT INTO config_service (" + COLUMNS + ") VALUES "
      + "(#{it.id}, #{it.appId}, #{it.serviceName}, #{it.serviceKey}, #{it.status}, "
      + "#{it.dataChangeCreateTime}, #{it.dataChangeUpdateTime}) "
      + "ON DUPLICATE KEY UPDATE service_name = VALUES(service_name), "
      + "service_key = VALUES(service_key), data_change_update_time = VALUES(data_change_update_time)")
  int upsert(@Param("it") Service entity);

  @Update("UPDATE config_service SET status = #{status}, data_change_update_time = #{now} "
      + "WHERE id = #{id}")
  int updateStatusById(@Param("id") String id, @Param("status") Integer status,
      @Param("now") long now);

  @Delete("DELETE FROM config_service WHERE id = #{id}")
  int deleteById(@Param("id") String id);

  @Delete("DELETE FROM config_service WHERE app_id = #{appId}")
  int deleteByAppId(@Param("appId") String appId);
}
