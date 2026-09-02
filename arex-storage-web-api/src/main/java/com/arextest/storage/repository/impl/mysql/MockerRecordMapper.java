package com.arextest.storage.repository.impl.mysql;

import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * MyBatis mapper of the mocker_record table. All queries are category-scoped
 * because (id, category) is the primary key (mongo used one collection per
 * category).
 */
@Mapper
public interface MockerRecordMapper {

  String COLUMNS = "id, category, record_id, replay_id, app_id, operation_name, record_env, "
      + "record_version, use_mock, creation_time, update_time, expiration_time, tags, eigen_map, "
      + "target_request, target_response";

  @Insert("<script>"
      + "INSERT INTO mocker_record (" + COLUMNS + ") VALUES "
      + "<foreach collection='list' item='it' separator=','>"
      + "(#{it.id}, #{it.category}, #{it.recordId}, #{it.replayId}, #{it.appId}, "
      + "#{it.operationName}, #{it.recordEnv}, #{it.recordVersion}, #{it.useMock}, "
      + "#{it.creationTime}, #{it.updateTime}, #{it.expirationTime}, #{it.tags}, #{it.eigenMap}, "
      + "#{it.targetRequest}, #{it.targetResponse})"
      + "</foreach>"
      + "</script>")
  int insertBatch(@Param("list") List<MockerRecordEntity> list);

  @Select("<script>"
      + "SELECT " + COLUMNS + " FROM mocker_record "
      + "WHERE category = #{category} AND "
      + "<choose>"
      + "<when test='entryPoint'>id = #{recordId}</when>"
      + "<otherwise>record_id = #{recordId}</otherwise>"
      + "</choose>"
      + "</script>")
  List<MockerRecordEntity> selectByRecordId(@Param("category") String category,
      @Param("recordId") String recordId, @Param("entryPoint") boolean entryPoint);

  @Select("<script>"
      + "SELECT " + COLUMNS + " FROM mocker_record "
      + "WHERE category = #{category} AND app_id = #{appId} "
      + "<if test='operationName != null'>AND operation_name = #{operationName} </if>"
      + "AND record_env = #{env} AND "
      + "<choose>"
      + "<when test='entryPoint'>id = #{recordId}</when>"
      + "<otherwise>record_id = #{recordId}</otherwise>"
      + "</choose>"
      + "ORDER BY creation_time DESC LIMIT 1"
      + "</script>")
  MockerRecordEntity selectLatestRecord(@Param("category") String category,
      @Param("appId") String appId, @Param("operationName") String operationName,
      @Param("env") int env, @Param("recordId") String recordId,
      @Param("entryPoint") boolean entryPoint);

  @Select("SELECT " + COLUMNS + " FROM mocker_record WHERE category = #{category} AND id = #{id}")
  MockerRecordEntity selectById(@Param("category") String category, @Param("id") String id);

  /**
   * Range query shared by paging/count/group operations. Sorting column names
   * must be translated through the whitelist in the provider before reaching
   * ${orderBy}; never expose raw request input here.
   */
  @Select("<script>"
      + "SELECT " + COLUMNS + " FROM mocker_record "
      + "<where>"
      + "category = #{category} AND app_id = #{appId} "
      + "<if test='operationName != null'>AND operation_name = #{operationName} </if>"
      + "<if test='env != null'>AND record_env = #{env} </if>"
      + "AND creation_time &gt;= #{beginTime} AND creation_time &lt; #{endTime} "
      + "<if test='recordVersion != null'>AND record_version = #{recordVersion} </if>"
      + "<if test='tags != null'>"
      + "<foreach collection='tags' index='tagKey' item='tagValue'>"
      + "AND JSON_UNQUOTE(JSON_EXTRACT(tags, CONCAT('$.\"', #{tagKey}, '\"'))) = #{tagValue} "
      + "</foreach>"
      + "</if>"
      + "</where>"
      + "ORDER BY ${orderBy} "
      + "LIMIT #{offset}, #{limit}"
      + "</script>")
  List<MockerRecordEntity> selectByRange(@Param("category") String category,
      @Param("appId") String appId, @Param("operationName") String operationName,
      @Param("env") Integer env, @Param("beginTime") long beginTime, @Param("endTime") long endTime,
      @Param("recordVersion") String recordVersion,
      @Param("tags") java.util.Map<String, String> tags,
      @Param("orderBy") String orderBy, @Param("offset") int offset, @Param("limit") int limit);

  @Select("<script>"
      + "SELECT creation_time, record_version FROM mocker_record "
      + "<where>"
      + "category = #{category} AND app_id = #{appId} "
      + "<if test='operationName != null'>AND operation_name = #{operationName} </if>"
      + "<if test='env != null'>AND record_env = #{env} </if>"
      + "AND creation_time &gt;= #{beginTime} AND creation_time &lt; #{endTime} "
      + "</where>"
      + "ORDER BY creation_time DESC LIMIT 1"
      + "</script>")
  MockerRecordEntity selectLatestVersionOfRange(@Param("category") String category,
      @Param("appId") String appId, @Param("operationName") String operationName,
      @Param("env") Integer env, @Param("beginTime") long beginTime, @Param("endTime") long endTime);

  @Select("<script>"
      + "SELECT COUNT(1) FROM mocker_record "
      + "<where>"
      + "category = #{category} AND app_id = #{appId} "
      + "<if test='operationName != null'>AND operation_name = #{operationName} </if>"
      + "<if test='env != null'>AND record_env = #{env} </if>"
      + "AND creation_time &gt;= #{beginTime} AND creation_time &lt; #{endTime} "
      + "<if test='recordVersion != null'>AND record_version = #{recordVersion} </if>"
      + "</where>"
      + "</script>")
  long countByRange(@Param("category") String category,
      @Param("appId") String appId, @Param("operationName") String operationName,
      @Param("env") Integer env, @Param("beginTime") long beginTime, @Param("endTime") long endTime,
      @Param("recordVersion") String recordVersion);

  @Select("<script>"
      + "SELECT operation_name AS operationName, COUNT(1) AS cnt FROM mocker_record "
      + "<where>"
      + "category = #{category} AND app_id = #{appId} "
      + "<if test='operationName != null'>AND operation_name = #{operationName} </if>"
      + "<if test='env != null'>AND record_env = #{env} </if>"
      + "AND creation_time &gt;= #{beginTime} AND creation_time &lt; #{endTime} "
      + "<if test='recordVersion != null'>AND record_version = #{recordVersion} </if>"
      + "</where>"
      + "GROUP BY operation_name"
      + "</script>")
  List<OperationCount> countGroupByOperation(@Param("category") String category,
      @Param("appId") String appId, @Param("operationName") String operationName,
      @Param("env") Integer env, @Param("beginTime") long beginTime, @Param("endTime") long endTime,
      @Param("recordVersion") String recordVersion);

  /**
   * Sideways expiration extension executed on reads of the rolling provider,
   * mirrors the mongo edition: only rows expiring before the allowed boundary
   * are touched.
   */
  @Update("<script>"
      + "UPDATE mocker_record SET expiration_time = #{newExpiration}, update_time = #{now} "
      + "WHERE category = #{category} AND "
      + "<choose>"
      + "<when test='entryPoint'>id = #{recordId}</when>"
      + "<otherwise>record_id = #{recordId}</otherwise>"
      + "</choose>"
      + "AND expiration_time &lt; #{allowedLastMills}"
      + "</script>")
  long extendExpirationOnRead(@Param("category") String category,
      @Param("recordId") String recordId, @Param("entryPoint") boolean entryPoint,
      @Param("newExpiration") long newExpiration, @Param("allowedLastMills") long allowedLastMills,
      @Param("now") long now);

  @Update("UPDATE mocker_record SET expiration_time = #{expireTime} "
      + "WHERE category = #{category} AND record_id = #{recordId}")
  long extendExpirationTo(@Param("category") String category, @Param("recordId") String recordId,
      @Param("expireTime") long expireTime);

  @Update("<script>"
      + "UPDATE mocker_record SET replay_id = #{it.replayId}, app_id = #{it.appId}, "
      + "operation_name = #{it.operationName}, record_env = #{it.recordEnv}, "
      + "record_version = #{it.recordVersion}, use_mock = #{it.useMock}, "
      + "creation_time = #{it.creationTime}, update_time = #{it.updateTime}, "
      + "expiration_time = #{it.expirationTime}, tags = #{it.tags}, eigen_map = #{it.eigenMap}, "
      + "target_request = #{it.targetRequest}, target_response = #{it.targetResponse} "
      + "WHERE category = #{it.category} AND id = #{it.id}"
      + "</script>")
  int updateById(@Param("it") MockerRecordEntity entity);

  @Delete("<script>"
      + "DELETE FROM mocker_record WHERE category = #{category} AND "
      + "<choose>"
      + "<when test='entryPoint'>id = #{recordId}</when>"
      + "<otherwise>record_id = #{recordId}</otherwise>"
      + "</choose>"
      + "</script>")
  long deleteByRecordId(@Param("category") String category, @Param("recordId") String recordId,
      @Param("entryPoint") boolean entryPoint);

  @Delete("DELETE FROM mocker_record WHERE category = #{category} AND app_id = #{appId}")
  long deleteByAppId(@Param("category") String category, @Param("appId") String appId);

  @Delete("DELETE FROM mocker_record WHERE category = #{category} "
      + "AND app_id = #{appId} AND operation_name = #{operationName}")
  long deleteByOperationAndAppId(@Param("category") String category,
      @Param("operationName") String operationName, @Param("appId") String appId);

  @Delete("DELETE FROM mocker_record WHERE category = #{category} AND id = #{id}")
  long deleteById(@Param("category") String category, @Param("id") String id);

  @Delete("DELETE FROM mocker_record WHERE expiration_time < #{now}")
  long deleteExpired(@Param("now") long now);

  /**
   * Projection of the GROUP BY operation_name aggregation.
   */
  class OperationCount {

    private String operationName;
    private Long cnt;

    public String getOperationName() {
      return operationName;
    }

    public void setOperationName(String operationName) {
      this.operationName = operationName;
    }

    public Long getCnt() {
      return cnt;
    }

    public void setCnt(Long cnt) {
      this.cnt = cnt;
    }
  }
}
