package com.arextest.storage.repository.impl.mysql;

import lombok.Data;

/**
 * Flat row of the scene_pool table. Time columns are epoch millis.
 */
@Data
public class ScenePoolEntity {

  private String id;
  private String providerName;
  private String sceneKey;
  private String appId;
  private String recordId;
  private String executionPath;
  private Long creationTime;
  private Long updateTime;
  private Long expirationTime;
}
