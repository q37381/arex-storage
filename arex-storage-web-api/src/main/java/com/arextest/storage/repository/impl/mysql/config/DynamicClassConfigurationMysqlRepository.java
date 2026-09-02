package com.arextest.storage.repository.impl.mysql.config;

import com.arextest.config.model.dto.record.DynamicClassConfiguration;
import com.arextest.config.repository.impl.DynamicClassConfigurationRepositoryImpl;
import com.arextest.storage.repository.impl.mysql.config.ConfigEntities.DynamicClass;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mysql implementation of the dynamic class config store.
 */
@Slf4j
public class DynamicClassConfigurationMysqlRepository extends DynamicClassConfigurationRepositoryImpl {

  private final ConfigDynamicClassMapper mapper;

  public DynamicClassConfigurationMysqlRepository(ConfigDynamicClassMapper mapper) {
    super(null);
    this.mapper = mapper;
  }

  @Override
  public List<DynamicClassConfiguration> list() {
    throw new UnsupportedOperationException("this method is not implemented");
  }

  @Override
  public List<DynamicClassConfiguration> listBy(String appId) {
    return mapper.selectByAppId(appId).stream()
        .map(ConfigConverters::toDto)
        .collect(Collectors.toList());
  }

  /**
   * mongo findAndModify(upsert by _id): update when the row exists, insert
   * otherwise.
   */
  @Override
  public boolean update(DynamicClassConfiguration configuration) {
    if (configuration == null || StringUtils.isBlank(configuration.getId())) {
      return false;
    }
    long now = System.currentTimeMillis();
    if (mapper.selectById(configuration.getId()) != null) {
      return mapper.updateById(ConfigConverters.fromDto(configuration), now) > 0;
    }
    DynamicClass entity = ConfigConverters.fromDto(configuration);
    entity.setDataChangeCreateTime(now);
    entity.setDataChangeUpdateTime(now);
    return mapper.insert(entity) > 0;
  }

  @Override
  public boolean remove(DynamicClassConfiguration configuration) {
    if (configuration == null || configuration.getId() == null) {
      return false;
    }
    return mapper.deleteById(configuration.getId()) > 0;
  }

  @Override
  public boolean insert(DynamicClassConfiguration configuration) {
    DynamicClass entity = ConfigConverters.fromDto(configuration);
    long now = System.currentTimeMillis();
    entity.setDataChangeCreateTime(now);
    entity.setDataChangeUpdateTime(now);
    boolean inserted = mapper.insert(entity) > 0;
    if (inserted) {
      configuration.setId(entity.getId());
    }
    return inserted;
  }

  @Override
  public boolean removeByAppId(String appId) {
    return mapper.deleteByAppId(appId) > 0;
  }

  /**
   * Replace all dynamic classes of the app. The mongo edition is delete-all
   * then insert-all; here the two steps run in one transaction so a failed
   * insert does not leave the app without dynamic classes.
   */
  @Override
  @Transactional(rollbackFor = Exception.class)
  public boolean cover(String appId, List<DynamicClassConfiguration> configuration) {
    mapper.deleteByAppId(appId);
    if (CollectionUtils.isEmpty(configuration)) {
      return true;
    }
    try {
      long now = System.currentTimeMillis();
      List<DynamicClass> entities = configuration.stream()
          .map(dto -> {
            DynamicClass entity = ConfigConverters.fromDto(dto);
            entity.setId(ConfigJsonSupport.newId());
            entity.setAppId(appId);
            entity.setDataChangeCreateTime(now);
            entity.setDataChangeUpdateTime(now);
            return entity;
          })
          .collect(Collectors.toList());
      mapper.insertBatch(entities);
    } catch (RuntimeException e) {
      LOGGER.error("cover dynamic class failed, appId: {}", appId, e);
      throw e;
    }
    return true;
  }
}
