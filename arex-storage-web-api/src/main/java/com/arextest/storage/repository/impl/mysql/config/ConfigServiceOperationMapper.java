package com.arextest.storage.repository.impl.mysql.config;

import com.arextest.storage.repository.impl.mysql.config.ConfigEntities.ServiceOperation;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * Mapper of config_service_operation. queryByConditions only accepts the
 * whitelisted columns translated in the repository layer.
 */
@Mapper
public interface ConfigServiceOperationMapper {

  String COLUMNS = "id, app_id, service_id, operation_name, operation_response, operation_type, "
      + "operation_types, recorded_case_count, status, data_change_create_time, "
      + "data_change_update_time";

  @Select("SELECT " + COLUMNS + " FROM config_service_operation WHERE app_id = #{appId}")
  List<ServiceOperation> selectByAppId(@Param("appId") String appId);

  @Select("SELECT " + COLUMNS + " FROM config_service_operation WHERE id = #{id}")
  ServiceOperation selectById(@Param("id") String id);

  @Select("SELECT " + COLUMNS + " FROM config_service_operation WHERE service_id = #{serviceId}")
  List<ServiceOperation> selectByServiceId(@Param("serviceId") String serviceId);

  @Select("SELECT " + COLUMNS + " FROM config_service_operation "
      + "WHERE app_id = #{appId} AND service_id = #{serviceId} AND operation_name = #{operationName}")
  ServiceOperation selectByUniqueOp(@Param("appId") String appId,
      @Param("serviceId") String serviceId, @Param("operationName") String operationName);

  @Select("<script>SELECT " + COLUMNS + " FROM config_service_operation "
      + "<where>"
      + "<foreach collection='conditions' index='key' item='value'>AND ${key} = #{value} </foreach>"
      + "</where></script>")
  List<ServiceOperation> selectByConditions(@Param("conditions") Map<String, Object> conditions);

  @Insert("INSERT INTO config_service_operation (" + COLUMNS + ") VALUES "
      + "(#{it.id}, #{it.appId}, #{it.serviceId}, #{it.operationName}, #{it.operationResponse}, "
      + "#{it.operationType}, #{it.operationTypes}, #{it.recordedCaseCount}, #{it.status}, "
      + "#{it.dataChangeCreateTime}, #{it.dataChangeUpdateTime})")
  int insert(@Param("it") ServiceOperation entity);

  @Update("UPDATE config_service_operation SET status = #{status}, "
      + "data_change_update_time = #{now} WHERE id = #{id}")
  int updateStatusById(@Param("id") String id, @Param("status") Integer status,
      @Param("now") long now);

  /**
   * Null-skip update used by findAndUpdate (mongo findAndModify upsert).
   * operationTypes is the union-merged JSON array prepared by the repository.
   */
  @Update("<script>UPDATE config_service_operation <set>"
      + "<if test='it.operationType != null'>operation_type = #{it.operationType},</if>"
      + "<if test='it.status != null'>status = #{it.status},</if>"
      + "<if test='it.operationTypes != null'>operation_types = #{it.operationTypes},</if>"
      + "data_change_update_time = #{it.dataChangeUpdateTime}"
      + "</set> WHERE id = #{it.id}</script>")
  int updateForFindAndModify(@Param("it") ServiceOperation entity);

  @Delete("DELETE FROM config_service_operation WHERE id = #{id}")
  int deleteById(@Param("id") String id);

  @Delete("DELETE FROM config_service_operation WHERE app_id = #{appId}")
  int deleteByAppId(@Param("appId") String appId);
}
