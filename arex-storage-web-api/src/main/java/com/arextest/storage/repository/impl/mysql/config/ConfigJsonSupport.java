package com.arextest.storage.repository.impl.mysql.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * Shared JSON (de)serialization for the config store. Complex mongo sub
 * documents (maps, sets, nested config objects) are persisted as JSON text
 * columns and converted here. Also centralizes the epoch-millis <-> Date /
 * Timestamp conversions used by the config DTOs.
 */
@Slf4j
public final class ConfigJsonSupport {

  private static final ObjectMapper MAPPER = new ObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  private ConfigJsonSupport() {
  }

  public static String writeJson(Object value) {
    if (value == null) {
      return null;
    }
    try {
      return MAPPER.writeValueAsString(value);
    } catch (Exception e) {
      LOGGER.warn("serialize config field failed: {}", e.getMessage());
      return null;
    }
  }

  public static <T> T readJson(String json, Class<T> type) {
    if (StringUtils.isEmpty(json)) {
      return null;
    }
    try {
      return MAPPER.readValue(json, type);
    } catch (Exception e) {
      LOGGER.warn("deserialize config field failed: {}", e.getMessage());
      return null;
    }
  }

  public static <T> T readJson(String json, TypeReference<T> type) {
    if (StringUtils.isEmpty(json)) {
      return null;
    }
    try {
      return MAPPER.readValue(json, type);
    } catch (Exception e) {
      LOGGER.warn("deserialize config field failed: {}", e.getMessage());
      return null;
    }
  }

  public static Long toMillis(Date date) {
    return date == null ? null : date.getTime();
  }

  public static Date toDate(Long millis) {
    return millis == null ? null : new Date(millis);
  }

  public static Timestamp toTimestamp(Long millis) {
    return millis == null ? null : new Timestamp(millis);
  }

  public static Long toMillis(Timestamp timestamp) {
    return timestamp == null ? null : timestamp.getTime();
  }

  /**
   * Mongo ObjectId-like unique id for config rows.
   */
  public static String newId() {
    return java.util.UUID.randomUUID().toString().replace("-", "");
  }
}
