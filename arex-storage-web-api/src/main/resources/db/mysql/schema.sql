-- =====================================================================
-- AREX Storage MySQL schema (minimal recording/replay closed-loop)
-- Replaces MongoDB collections of the "Rolling" provider only.
-- Pinned/AutoPinned providers are dropped in this edition.
--
-- NOTE: Mongo stores entry-point mockers with _id = recordId inside a
-- per-category collection, so the same recordId may appear under several
-- categories. The relational schema therefore keeps (id, category) unique
-- (uk_id_category) while `auto_id` is the auto-increment primary key.
-- =====================================================================

CREATE TABLE IF NOT EXISTS `mocker_record` (
  `auto_id`              BIGINT       NOT NULL AUTO_INCREMENT,
  `id`               VARCHAR(64)  NOT NULL COMMENT 'entry-point: recordId; otherwise: uuid',
  `category`         VARCHAR(64)  NOT NULL COMMENT 'MockCategoryType name, e.g. Servlet/HttpClient',
  `record_id`        VARCHAR(64)  DEFAULT NULL COMMENT 'empty for entry-point rows',
  `replay_id`        VARCHAR(64)  DEFAULT NULL,
  `app_id`           VARCHAR(128) NOT NULL DEFAULT '',
  `operation_name`   VARCHAR(512) NOT NULL DEFAULT '',
  `record_env`       INT          NOT NULL DEFAULT 0,
  `record_version`   VARCHAR(64)  DEFAULT NULL,
  `use_mock`         TINYINT(1)   DEFAULT NULL,
  `creation_time`    BIGINT       NOT NULL COMMENT 'epoch millis',
  `update_time`      BIGINT       NOT NULL COMMENT 'epoch millis',
  `expiration_time`  BIGINT       NOT NULL COMMENT 'epoch millis; periodic cleaner deletes expired rows',
  `tags`             TEXT         DEFAULT NULL COMMENT 'JSON object',
  `eigen_map`        TEXT         DEFAULT NULL COMMENT 'JSON object, eigen values of mock data',
  `target_request`   MEDIUMTEXT   DEFAULT NULL COMMENT 'JSON of Mocker.Target',
  `target_response`  MEDIUMTEXT   DEFAULT NULL COMMENT 'JSON of Mocker.Target',
  PRIMARY KEY (`auto_id`),
  UNIQUE KEY `uk_id_category` (`id`, `category`),
  KEY `idx_record_id` (`record_id`),
  KEY `idx_category_record` (`category`, `record_id`),
  KEY `idx_app_op_time` (`app_id`, `operation_name`(191), `creation_time`),
  KEY `idx_expire` (`expiration_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Replay result queue: replaces the redis list (buildReplayResultKey).
-- Only required when storage runs with a shared cache provider
-- (multi-instance). With the default local in-process cache provider the
-- queue lives in memory and this table stays unused, kept here for the
-- upcoming distributed-lock/mysql-cache switch.
CREATE TABLE IF NOT EXISTS `replay_result_queue` (
  `id`           BIGINT      NOT NULL AUTO_INCREMENT,
  `replay_id`    VARCHAR(64) NOT NULL,
  `payload`      MEDIUMBLOB  NOT NULL COMMENT 'serialized batch of CompareRelationResult',
  `expire_time`  BIGINT      NOT NULL COMMENT 'epoch millis, 10 minutes sliding window',
  `created_time` BIGINT      NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_replay` (`replay_id`),
  KEY `idx_expire` (`expire_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Scene pools: replaces the RecordingScenePool / ReplayScenePool mongo
-- collections. One relational table holding both pools, discriminated by
-- provider_name. Unique key backs the upsert (findAndModify) semantics.
CREATE TABLE IF NOT EXISTS `scene_pool` (
  `auto_id`             BIGINT       NOT NULL AUTO_INCREMENT,
  `id`              VARCHAR(64)  NOT NULL,
  `provider_name`   VARCHAR(32)  NOT NULL COMMENT 'Recording | Replay',
  `scene_key`       VARCHAR(512) NOT NULL DEFAULT '',
  `app_id`          VARCHAR(128) NOT NULL DEFAULT '',
  `record_id`       VARCHAR(64)  DEFAULT NULL,
  `execution_path`  MEDIUMTEXT   DEFAULT NULL,
  `creation_time`   BIGINT       NOT NULL COMMENT 'epoch millis',
  `update_time`     BIGINT       NOT NULL COMMENT 'epoch millis',
  `expiration_time` BIGINT       NOT NULL COMMENT 'epoch millis',
  PRIMARY KEY (`auto_id`),
  UNIQUE KEY `uk_id` (`id`),
  UNIQUE KEY `uk_pool_scene` (`provider_name`, `app_id`, `scene_key`(255)),
  KEY `idx_record_id` (`record_id`),
  KEY `idx_app` (`app_id`),
  KEY `idx_expire` (`expiration_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- =====================================================================
-- Config store: replaces the 9 mongo config collections.
-- All tables share (id, data_change_create_time, data_change_update_time);
-- complex nested fields are stored as JSON text.
-- =====================================================================

-- Application (mongo: App). auto_id preserves mongo's _id DESC listing order.
CREATE TABLE IF NOT EXISTS `config_application` (
  `auto_id`                     BIGINT       NOT NULL AUTO_INCREMENT,
  `id`                      VARCHAR(64)  NOT NULL,
  `app_id`                  VARCHAR(128) NOT NULL,
  `features`                INT          NOT NULL DEFAULT 0,
  `group_name`              VARCHAR(255) DEFAULT NULL,
  `group_id`                VARCHAR(128) DEFAULT NULL,
  `agent_version`           VARCHAR(64)  DEFAULT NULL,
  `agent_ext_version`       VARCHAR(64)  DEFAULT NULL,
  `app_name`                VARCHAR(255) DEFAULT NULL,
  `description`             VARCHAR(1024) DEFAULT NULL,
  `category`                VARCHAR(128) DEFAULT NULL,
  `owner`                   VARCHAR(128) DEFAULT NULL,
  `owners`                  TEXT         DEFAULT NULL COMMENT 'JSON array',
  `organization_name`       VARCHAR(255) DEFAULT NULL,
  `recorded_case_count`     INT          DEFAULT NULL,
  `organization_id`         VARCHAR(128) DEFAULT NULL,
  `status`                  INT          DEFAULT NULL,
  `visibility_level`        INT          NOT NULL DEFAULT 0,
  `tags`                    TEXT         DEFAULT NULL COMMENT 'JSON object: env -> set of tags',
  `data_change_create_time` BIGINT       DEFAULT NULL,
  `data_change_update_time` BIGINT       DEFAULT NULL,
  PRIMARY KEY (`auto_id`),
  UNIQUE KEY `uk_id` (`id`),
  UNIQUE KEY `uk_app_id` (`app_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Application service (mongo: Service). One row per appId.
CREATE TABLE IF NOT EXISTS `config_service` (
  `auto_id`                     BIGINT       NOT NULL AUTO_INCREMENT,
  `id`                      VARCHAR(64)  NOT NULL,
  `app_id`                  VARCHAR(128) NOT NULL,
  `service_name`            VARCHAR(512) DEFAULT NULL,
  `service_key`             VARCHAR(512) DEFAULT NULL,
  `status`                  INT          DEFAULT NULL,
  `data_change_create_time` BIGINT       DEFAULT NULL,
  `data_change_update_time` BIGINT       DEFAULT NULL,
  PRIMARY KEY (`auto_id`),
  UNIQUE KEY `uk_id` (`id`),
  UNIQUE KEY `uk_app_id` (`app_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Service operations (mongo: ServiceOperation).
CREATE TABLE IF NOT EXISTS `config_service_operation` (
  `auto_id`                     BIGINT       NOT NULL AUTO_INCREMENT,
  `id`                      VARCHAR(64)  NOT NULL,
  `app_id`                  VARCHAR(128) NOT NULL,
  `service_id`              VARCHAR(64)  DEFAULT NULL,
  `operation_name`          VARCHAR(1024) DEFAULT NULL,
  `operation_response`      MEDIUMTEXT   DEFAULT NULL,
  `operation_type`          VARCHAR(64)  DEFAULT NULL,
  `operation_types`         TEXT         DEFAULT NULL COMMENT 'JSON array, union-merged on upsert',
  `recorded_case_count`     INT          DEFAULT NULL,
  `status`                  INT          DEFAULT NULL,
  `data_change_create_time` BIGINT       DEFAULT NULL,
  `data_change_update_time` BIGINT       DEFAULT NULL,
  PRIMARY KEY (`auto_id`),
  UNIQUE KEY `uk_id` (`id`),
  UNIQUE KEY `uk_op` (`app_id`, `service_id`, `operation_name`(255)),
  KEY `idx_service` (`service_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Recording switch / sample rate (mongo: RecordServiceConfig). Core of the
-- recording on/off + sampling capability.
CREATE TABLE IF NOT EXISTS `config_record_service` (
  `auto_id`                           BIGINT       NOT NULL AUTO_INCREMENT,
  `id`                            VARCHAR(64)  NOT NULL,
  `app_id`                        VARCHAR(128) NOT NULL,
  `sample_rate`                   INT          NOT NULL DEFAULT 1,
  `allow_day_of_weeks`            INT          NOT NULL DEFAULT 127,
  `time_mock`                     TINYINT(1)   NOT NULL DEFAULT 0,
  `allow_time_of_day_from`        VARCHAR(8)   DEFAULT NULL,
  `allow_time_of_day_to`          VARCHAR(8)   DEFAULT NULL,
  `exclude_service_operation_set` TEXT         DEFAULT NULL COMMENT 'JSON array',
  `record_machine_count_limit`    INT          DEFAULT NULL,
  `extend_field`                  TEXT         DEFAULT NULL COMMENT 'JSON object',
  `serialize_skip_info_list`      TEXT         DEFAULT NULL COMMENT 'JSON array',
  `multi_env_configs`             MEDIUMTEXT   DEFAULT NULL COMMENT 'JSON array of per-env configs',
  `env_tags`                      TEXT         DEFAULT NULL COMMENT 'JSON object',
  `data_change_create_time`       BIGINT       DEFAULT NULL,
  `data_change_update_time`       BIGINT       DEFAULT NULL,
  PRIMARY KEY (`auto_id`),
  UNIQUE KEY `uk_id` (`id`),
  UNIQUE KEY `uk_app_id` (`app_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Dynamic classes (mongo: DynamicClass).
CREATE TABLE IF NOT EXISTS `config_dynamic_class` (
  `auto_id`                     BIGINT       NOT NULL AUTO_INCREMENT,
  `id`                      VARCHAR(64)  NOT NULL,
  `app_id`                  VARCHAR(128) NOT NULL,
  `full_class_name`         VARCHAR(1024) DEFAULT NULL,
  `method_name`             VARCHAR(255) DEFAULT NULL,
  `parameter_types`         VARCHAR(1024) DEFAULT NULL,
  `config_type`             INT          NOT NULL DEFAULT 0,
  `key_formula`             VARCHAR(1024) DEFAULT NULL,
  `data_change_create_time` BIGINT       DEFAULT NULL,
  `data_change_update_time` BIGINT       DEFAULT NULL,
  PRIMARY KEY (`auto_id`),
  UNIQUE KEY `uk_id` (`id`),
  KEY `idx_app_id` (`app_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Agent instances (mongo: Instances). auto_id preserves mongo's _id ASC order used
-- by the record-machine allocation logic.
CREATE TABLE IF NOT EXISTS `config_instances` (
  `auto_id`                     BIGINT       NOT NULL AUTO_INCREMENT,
  `id`                      VARCHAR(64)  NOT NULL,
  `app_id`                  VARCHAR(128) NOT NULL,
  `host`                    VARCHAR(255) NOT NULL DEFAULT '',
  `record_version`          VARCHAR(64)  DEFAULT NULL,
  `data_update_time`        BIGINT       DEFAULT NULL,
  `agent_status`            VARCHAR(32)  DEFAULT NULL,
  `tags`                    TEXT         DEFAULT NULL COMMENT 'JSON object',
  `system_env`              TEXT         DEFAULT NULL COMMENT 'JSON object',
  `system_properties`       TEXT         DEFAULT NULL COMMENT 'JSON object',
  `extend_field`            TEXT         DEFAULT NULL COMMENT 'JSON object',
  `data_change_create_time` BIGINT       DEFAULT NULL,
  `data_change_update_time` BIGINT       DEFAULT NULL,
  PRIMARY KEY (`auto_id`),
  UNIQUE KEY `uk_id` (`id`),
  UNIQUE KEY `uk_app_host` (`app_id`, `host`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- System configuration (mongo: SystemConfiguration), key-value style.
CREATE TABLE IF NOT EXISTS `config_system` (
  `auto_id`                     BIGINT       NOT NULL AUTO_INCREMENT,
  `id`                      VARCHAR(64)  NOT NULL,
  `config_key`              VARCHAR(128) NOT NULL,
  `refresh_task_mark`       TEXT         DEFAULT NULL COMMENT 'JSON object',
  `desensitization_jar`     TEXT         DEFAULT NULL COMMENT 'JSON object',
  `callback_url`            VARCHAR(1024) DEFAULT NULL,
  `auth_switch`             TINYINT(1)   DEFAULT NULL,
  `compare_plugin_info`     TEXT         DEFAULT NULL COMMENT 'JSON object',
  `jwt_seed`                VARCHAR(255) DEFAULT NULL,
  `ignore_node_set`         TEXT         DEFAULT NULL COMMENT 'JSON array',
  `data_change_create_time` BIGINT       DEFAULT NULL,
  `data_change_update_time` BIGINT       DEFAULT NULL,
  PRIMARY KEY (`auto_id`),
  UNIQUE KEY `uk_id` (`id`),
  UNIQUE KEY `uk_key` (`config_key`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Comparison exclusions (mongo: ConfigComparisonExclusions).
CREATE TABLE IF NOT EXISTS `config_comparison_exclusions` (
  `auto_id`                     BIGINT       NOT NULL AUTO_INCREMENT,
  `id`                      VARCHAR(64)  NOT NULL,
  `app_id`                  VARCHAR(128) NOT NULL,
  `operation_id`            VARCHAR(64)  DEFAULT NULL,
  `expiration_type`         INT          NOT NULL DEFAULT 0,
  `expiration_date`         BIGINT       DEFAULT NULL COMMENT 'epoch millis',
  `compare_config_type`     INT          NOT NULL DEFAULT 0,
  `fs_interface_id`         VARCHAR(64)  DEFAULT NULL,
  `dependency_id`           VARCHAR(64)  DEFAULT NULL,
  `exclusions`              TEXT         DEFAULT NULL COMMENT 'JSON array of paths',
  `data_change_create_time` BIGINT       DEFAULT NULL,
  `data_change_update_time` BIGINT       DEFAULT NULL,
  PRIMARY KEY (`auto_id`),
  UNIQUE KEY `uk_id` (`id`),
  KEY `idx_app_type` (`app_id`, `compare_config_type`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
