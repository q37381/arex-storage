package com.arextest.storage.repository.impl.mysql.config;

import com.arextest.storage.repository.impl.mysql.config.ConfigEntities.DynamicClass;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * Mapper of config_dynamic_class.
 */
@Mapper
public interface ConfigDynamicClassMapper {

  String COLUMNS = "id, app_id, full_class_name, method_name, parameter_types, config_type, "
      + "key_formula, data_change_create_time, data_change_update_time";

  @Select("SELECT " + COLUMNS + " FROM config_dynamic_class WHERE app_id = #{appId}")
  List<DynamicClass> selectByAppId(@Param("appId") String appId);

  @Select("SELECT " + COLUMNS + " FROM config_dynamic_class WHERE id = #{id}")
  DynamicClass selectById(@Param("id") String id);

  @Insert("INSERT INTO config_dynamic_class (" + COLUMNS + ") VALUES "
      + "(#{it.id}, #{it.appId}, #{it.fullClassName}, #{it.methodName}, #{it.parameterTypes}, "
      + "#{it.configType}, #{it.keyFormula}, #{it.dataChangeCreateTime}, "
      + "#{it.dataChangeUpdateTime})")
  int insert(@Param("it") DynamicClass entity);

  @Insert("<script>INSERT INTO config_dynamic_class (" + COLUMNS + ") VALUES "
      + "<foreach collection='list' item='it' separator=','>"
      + "(#{it.id}, #{it.appId}, #{it.fullClassName}, #{it.methodName}, #{it.parameterTypes}, "
      + "#{it.configType}, #{it.keyFormula}, #{it.dataChangeCreateTime}, "
      + "#{it.dataChangeUpdateTime})</foreach></script>")
  int insertBatch(@Param("list") List<DynamicClass> list);

  /**
   * Null-skip update by id, matching MongoHelper.getMongoTemplateUpdates
   * semantics of the mongo edition.
   */
  @Update("<script>UPDATE config_dynamic_class <set>"
      + "<if test='it.fullClassName != null'>full_class_name = #{it.fullClassName},</if>"
      + "<if test='it.methodName != null'>method_name = #{it.methodName},</if>"
      + "<if test='it.parameterTypes != null'>parameter_types = #{it.parameterTypes},</if>"
      + "<if test='it.keyFormula != null'>key_formula = #{it.keyFormula},</if>"
      + "data_change_update_time = #{now}"
      + "</set> WHERE id = #{it.id}</script>")
  int updateById(@Param("it") DynamicClass entity, @Param("now") long now);

  @Delete("DELETE FROM config_dynamic_class WHERE id = #{id}")
  int deleteById(@Param("id") String id);

  @Delete("DELETE FROM config_dynamic_class WHERE app_id = #{appId}")
  int deleteByAppId(@Param("appId") String appId);
}
