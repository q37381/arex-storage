package com.arextest.storage.repository.impl.mysql;

import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.AREXQueryMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * Converts between the mocker domain model and its flat relational row.
 * JSON serialization is plain Jackson here (no zstd/encryption); the mongo
 * edition compresses through custom converters, which can be re-introduced
 * later behind this converter without touching callers.
 */
@Slf4j
public final class MockerRecordConverter {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final TypeReference<Map<String, String>> TAGS_TYPE =
      new TypeReference<Map<String, String>>() {
      };
  private static final TypeReference<Map<Integer, Long>> EIGEN_TYPE =
      new TypeReference<Map<Integer, Long>>() {
      };

  private MockerRecordConverter() {
  }

  public static MockerRecordEntity toEntity(AREXMocker mocker, String categoryName) {
    MockerRecordEntity entity = new MockerRecordEntity();
    entity.setId(mocker.getId());
    entity.setCategory(categoryName);
    entity.setRecordId(mocker.getRecordId());
    entity.setReplayId(mocker.getReplayId());
    entity.setAppId(mocker.getAppId());
    entity.setOperationName(mocker.getOperationName());
    entity.setRecordEnv(mocker.getRecordEnvironment());
    entity.setRecordVersion(mocker.getRecordVersion());
    entity.setUseMock(mocker.getUseMock());
    entity.setCreationTime(mocker.getCreationTime());
    entity.setUpdateTime(mocker.getUpdateTime());
    entity.setExpirationTime(mocker.getExpirationTime());
    entity.setTags(writeJson(mocker.getTags()));
    entity.setEigenMap(writeJson(mocker.getEigenMap()));
    entity.setTargetRequest(writeJson(mocker.getTargetRequest()));
    entity.setTargetResponse(writeJson(mocker.getTargetResponse()));
    return entity;
  }

  public static AREXMocker toMocker(MockerRecordEntity entity, MockCategoryType category) {
    AREXMocker mocker = new AREXMocker();
    fillCommon(entity, mocker);
    mocker.setTags(readJson(entity.getTags(), TAGS_TYPE));
    mocker.setEigenMap(readJson(entity.getEigenMap(), EIGEN_TYPE));
    mocker.setTargetRequest(readJson(entity.getTargetRequest(), Mocker.Target.class));
    mocker.setTargetResponse(readJson(entity.getTargetResponse(), Mocker.Target.class));
    mocker.setCategoryType(category);
    return mocker;
  }

  public static AREXQueryMocker toQueryMocker(MockerRecordEntity entity, MockCategoryType category) {
    AREXQueryMocker mocker = new AREXQueryMocker();
    fillCommon(entity, mocker);
    mocker.setRequest(entity.getTargetRequest());
    mocker.setResponse(entity.getTargetResponse());
    mocker.setCategoryType(category);
    return mocker;
  }

  public static List<MockerRecordEntity> toEntities(List<AREXMocker> mockers, String categoryName) {
    return mockers.stream().map(m -> toEntity(m, categoryName)).collect(Collectors.toList());
  }

  private static void fillCommon(MockerRecordEntity entity, AREXMocker mocker) {
    mocker.setId(entity.getId());
    mocker.setRecordId(entity.getRecordId());
    mocker.setReplayId(entity.getReplayId());
    mocker.setAppId(entity.getAppId());
    mocker.setOperationName(entity.getOperationName());
    mocker.setRecordEnvironment(entity.getRecordEnv() == null ? 0 : entity.getRecordEnv());
    mocker.setRecordVersion(entity.getRecordVersion());
    mocker.setUseMock(entity.getUseMock());
    mocker.setCreationTime(entity.getCreationTime() == null ? 0 : entity.getCreationTime());
    mocker.setUpdateTime(entity.getUpdateTime() == null ? 0 : entity.getUpdateTime());
    mocker.setExpirationTime(entity.getExpirationTime() == null ? 0 : entity.getExpirationTime());
  }

  private static void fillCommon(MockerRecordEntity entity, AREXQueryMocker mocker) {
    mocker.setId(entity.getId());
    mocker.setRecordId(entity.getRecordId());
    mocker.setReplayId(entity.getReplayId());
    mocker.setAppId(entity.getAppId());
    mocker.setOperationName(entity.getOperationName());
    mocker.setRecordEnvironment(entity.getRecordEnv() == null ? 0 : entity.getRecordEnv());
    mocker.setRecordVersion(entity.getRecordVersion());
    mocker.setUseMock(entity.getUseMock());
    mocker.setCreationTime(entity.getCreationTime() == null ? 0 : entity.getCreationTime());
    mocker.setUpdateTime(entity.getUpdateTime() == null ? 0 : entity.getUpdateTime());
    mocker.setExpirationTime(entity.getExpirationTime() == null ? 0 : entity.getExpirationTime());
  }

  private static String writeJson(Object value) {
    if (value == null) {
      return null;
    }
    try {
      return MAPPER.writeValueAsString(value);
    } catch (Exception e) {
      LOGGER.warn("serialize mocker field failed: {}", e.getMessage());
      return null;
    }
  }

  private static <T> T readJson(String json, Class<T> type) {
    if (StringUtils.isEmpty(json)) {
      return null;
    }
    try {
      return MAPPER.readValue(json, type);
    } catch (Exception e) {
      LOGGER.warn("deserialize mocker field failed: {}", e.getMessage());
      return null;
    }
  }

  private static <T> T readJson(String json, TypeReference<T> type) {
    if (StringUtils.isEmpty(json)) {
      return null;
    }
    try {
      return MAPPER.readValue(json, type);
    } catch (Exception e) {
      LOGGER.warn("deserialize mocker field failed: {}", e.getMessage());
      return null;
    }
  }
}
