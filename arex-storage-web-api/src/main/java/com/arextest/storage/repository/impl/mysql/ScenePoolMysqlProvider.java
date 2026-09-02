package com.arextest.storage.repository.impl.mysql;

import com.arextest.model.scenepool.Scene;
import com.arextest.storage.repository.scenepool.ScenePoolProvider;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * MySQL implementation of a scene pool. Does not extend
 * AbstractScenePoolProvider (that base class injects a MongoTemplate).
 *
 * Semantic parity notes:
 * - findAndUpdate returns the pre-update row (mongo findAndModify with
 *   returnNew=false) and upserts keyed by (provider, appId, sceneKey);
 * - expiration is now + 14 days on every write, rows are purged by the
 *   periodic cleaner in MysqlStorageConfiguration;
 * - the upsert is not atomic read-modify-write (select-then-write), which is
 *   acceptable for the coverage de-dup use case under a single storage
 *   instance; switch to the shared lock provider before scaling out.
 */
public class ScenePoolMysqlProvider implements ScenePoolProvider {

  private static final long EXPIRATION_DAYS = 14L;

  private final String providerName;
  private final ScenePoolMapper mapper;

  public ScenePoolMysqlProvider(String providerName, ScenePoolMapper mapper) {
    this.providerName = providerName;
    this.mapper = mapper;
  }

  @Override
  public String getProviderName() {
    return providerName;
  }

  @Override
  public boolean checkSceneExist(String appId, String sceneKey) {
    return mapper.countBySceneKey(providerName, appId, sceneKey) > 0;
  }

  @Override
  public Scene findAndUpdate(Scene newScene) {
    ScenePoolEntity previous = mapper.selectBySceneKey(providerName, newScene.getAppId(),
        newScene.getSceneKey());
    upsertOne(newScene);
    return toScene(previous);
  }

  @Override
  public void upsertOne(Scene scene) {
    mapper.upsert(toEntity(scene));
  }

  @Override
  public long clearSceneByAppid(String appid) {
    return mapper.deleteByAppId(providerName, appid);
  }

  @Override
  public Scene findByRecordId(String recordId) {
    return toScene(mapper.selectByRecordId(providerName, recordId));
  }

  @Override
  public List<String> findRecordsByAppId(String appId, int pageIndex, int pageSize) {
    int offset = Math.max(0, pageIndex) * pageSize;
    return mapper.selectRecordIdsByAppId(providerName, appId, offset, pageSize);
  }

  @Override
  public long countByAppId(String appId) {
    return mapper.countByAppId(providerName, appId);
  }

  private ScenePoolEntity toEntity(Scene scene) {
    long now = System.currentTimeMillis();
    long expire = Date.from(LocalDateTime.now().plusDays(EXPIRATION_DAYS)
        .atZone(ZoneId.systemDefault()).toInstant()).getTime();

    ScenePoolEntity entity = new ScenePoolEntity();
    entity.setId(scene.getId() == null ? UUID.randomUUID().toString().replace("-", "")
        : scene.getId());
    entity.setProviderName(providerName);
    entity.setSceneKey(scene.getSceneKey() == null ? "" : scene.getSceneKey());
    entity.setAppId(scene.getAppId() == null ? "" : scene.getAppId());
    entity.setRecordId(scene.getRecordId());
    entity.setExecutionPath(scene.getExecutionPath());
    entity.setCreationTime(scene.getCreationTime() == null ? now
        : scene.getCreationTime().getTime());
    entity.setUpdateTime(now);
    entity.setExpirationTime(expire);
    return entity;
  }

  private static Scene toScene(ScenePoolEntity entity) {
    if (entity == null) {
      return null;
    }
    Scene scene = new Scene();
    scene.setId(entity.getId());
    scene.setSceneKey(entity.getSceneKey());
    scene.setAppId(entity.getAppId());
    scene.setRecordId(entity.getRecordId());
    scene.setExecutionPath(entity.getExecutionPath());
    if (entity.getCreationTime() != null) {
      scene.setCreationTime(new Date(entity.getCreationTime()));
    }
    if (entity.getUpdateTime() != null) {
      scene.setUpdateTime(new Date(entity.getUpdateTime()));
    }
    if (entity.getExpirationTime() != null) {
      scene.setExpirationTime(new Date(entity.getExpirationTime()));
    }
    return scene;
  }
}
