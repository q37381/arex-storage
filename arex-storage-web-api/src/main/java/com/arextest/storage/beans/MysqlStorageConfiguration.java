package com.arextest.storage.beans;

import com.arextest.common.config.DefaultApplicationConfig;
import com.arextest.common.desensitization.DesensitizationProvider;
import com.arextest.extension.desensitization.DataDesensitization;
import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.AREXQueryMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.storage.repository.RepositoryProvider;
import com.arextest.storage.repository.impl.mysql.AREXMockerMysqlRepositoryProvider;
import com.arextest.storage.repository.impl.mysql.AREXQueryMockerMysqlRepositoryProvider;
import com.arextest.storage.repository.impl.mysql.MockerRecordMapper;
import com.arextest.storage.repository.impl.mysql.ScenePoolMapper;
import com.arextest.storage.repository.impl.mysql.ScenePoolMysqlProvider;
import com.arextest.storage.repository.scenepool.ScenePoolFactory;
import com.arextest.storage.repository.scenepool.ScenePoolProvider;
import com.zaxxer.hikari.HikariDataSource;
import java.util.Set;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Relational (mysql) wiring, active when arex.storage.repository.type=mysql
 * (the default). Replaces the mongo-backed beans in StorageAutoConfiguration:
 * mocker providers, desensitization (empty default) and the TTL index with a
 * periodic cleanup task.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "arex.storage.repository.type", havingValue = "mysql",
    matchIfMissing = true)
@MapperScan("com.arextest.storage.repository.impl.mysql")
@EnableScheduling
@Slf4j
public class MysqlStorageConfiguration {

  private static final long EXPIRED_CLEAN_INTERVAL_MS = 10 * 60 * 1000L;

  private final MockerRecordMapper mockerRecordMapper;
  private final ScenePoolMapper scenePoolMapper;

  // @Lazy breaks the startup cycle: this config class provides the DataSource
  // bean, while mappers require the SqlSessionFactory built from that DataSource.
  // The proxies are resolved on first use (the scheduled cleanup runs long after
  // the SqlSessionFactory is ready).
  public MysqlStorageConfiguration(@Lazy MockerRecordMapper mockerRecordMapper,
      @Lazy ScenePoolMapper scenePoolMapper) {
    this.mockerRecordMapper = mockerRecordMapper;
    this.scenePoolMapper = scenePoolMapper;
  }

  /**
   * DataSourceAutoConfiguration is excluded application-wide (the mongo backend
   * must not require a database), so the mysql backend builds its own pool.
   * MybatisAutoConfiguration still applies: it activates on the presence of
   * this single DataSource bean and creates the SqlSessionFactory.
   */
  @Bean
  public DataSource mysqlDataSource(
      @Value("${spring.datasource.url}") String url,
      @Value("${spring.datasource.username}") String username,
      @Value("${spring.datasource.password}") String password,
      @Value("${spring.datasource.driver-class-name}") String driverClassName) {
    HikariDataSource dataSource = new HikariDataSource();
    dataSource.setJdbcUrl(url);
    dataSource.setUsername(username);
    dataSource.setPassword(password);
    dataSource.setDriverClassName(driverClassName);
    return dataSource;
  }

  @Bean
  @Order(1)
  public RepositoryProvider<AREXMocker> mysqlMockerProvider(MockerRecordMapper mapper,
      StorageConfigurationProperties properties,
      Set<MockCategoryType> entryPointTypes, DefaultApplicationConfig defaultApplicationConfig) {
    return new AREXMockerMysqlRepositoryProvider(mapper, properties, entryPointTypes,
        defaultApplicationConfig);
  }

  @Bean
  @Order(2)
  public RepositoryProvider<AREXQueryMocker> mysqlQueryMockerProvider(MockerRecordMapper mapper,
      StorageConfigurationProperties properties,
      Set<MockCategoryType> entryPointTypes, DefaultApplicationConfig defaultApplicationConfig) {
    return new AREXQueryMockerMysqlRepositoryProvider(mapper, properties, entryPointTypes,
        defaultApplicationConfig);
  }

  @Bean
  public ScenePoolProvider mysqlRecordingScenePool(ScenePoolMapper scenePoolMapper) {
    return new ScenePoolMysqlProvider(ScenePoolFactory.RECORDING_SCENE_POOL, scenePoolMapper);
  }

  @Bean
  public ScenePoolProvider mysqlReplayScenePool(ScenePoolMapper scenePoolMapper) {
    return new ScenePoolMysqlProvider(ScenePoolFactory.REPLAY_SCENE_POOL, scenePoolMapper);
  }

  /**
   * No remote desensitization jar in mysql mode; DesensitizationProvider with
   * a null jar url falls back to the default no-op implementation.
   */
  @Bean
  public DesensitizationProvider mysqlDesensitizationProvider() {
    return new DesensitizationProvider(null);
  }

  @Bean
  @ConditionalOnMissingBean(DataDesensitization.class)
  public DataDesensitization mysqlDataDesensitization(
      DesensitizationProvider mysqlDesensitizationProvider) {
    return mysqlDesensitizationProvider.get();
  }

  /**
   * Replaces the mongo TTL index: purge expired mockers periodically.
   */
  @Scheduled(fixedDelay = EXPIRED_CLEAN_INTERVAL_MS, initialDelay = 60_000L)
  public void cleanExpiredMockers() {
    try {
      long removed = mockerRecordMapper.deleteExpired(System.currentTimeMillis());
      if (removed > 0) {
        LOGGER.info("cleaned {} expired mocker records", removed);
      }
      long removedScenes = scenePoolMapper.deleteExpired(System.currentTimeMillis());
      if (removedScenes > 0) {
        LOGGER.info("cleaned {} expired scene pool rows", removedScenes);
      }
    } catch (Exception e) {
      LOGGER.warn("clean expired rows failed: {}", e.getMessage());
    }
  }
}
