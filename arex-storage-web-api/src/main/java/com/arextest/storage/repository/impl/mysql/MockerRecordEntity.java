package com.arextest.storage.repository.impl.mysql;

import lombok.Data;

/**
 * Flat relational representation of an AREXMocker/AREXQueryMocker row.
 * Nested structures (target request/response, tags, eigenMap) are stored as
 * JSON strings and (de)serialized by {@link MockerRecordConverter}.
 */
@Data
public class MockerRecordEntity {

  private String id;
  private String category;
  private String recordId;
  private String replayId;
  private String appId;
  private String operationName;
  private Integer recordEnv;
  private String recordVersion;
  private Boolean useMock;
  private Long creationTime;
  private Long updateTime;
  private Long expirationTime;
  private String tags;
  private String eigenMap;
  private String targetRequest;
  private String targetResponse;
}
