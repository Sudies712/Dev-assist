-- ============================================================
-- dev-assist 平台建表脚本 schema.sql
-- 依据：需求规格说明书 V2.1 §7 数据字典 / §7.11 枚举汇总 / §12 权限矩阵
--       概要设计 V1.1 §7 数据库设计
-- 目标库：MySQL 8.x，字符集 utf8mb4 / utf8mb4_unicode_ci，引擎 InnoDB
-- ============================================================
--
-- 通用字段约定（业务实体）：
--   id          BIGINT UNSIGNED 主键自增
--   create_time DATETIME 创建时间（默认 CURRENT_TIMESTAMP）
--   update_time DATETIME 更新时间（ON UPDATE CURRENT_TIMESTAMP）
--   create_by / update_by  BIGINT UNSIGNED 审计字段（user.id）
--   version     INT 乐观锁（仅可并发修改的业务实体）
--   is_deleted  TINYINT(1) 逻辑删除标记（MyBatis-Plus @TableLogic）
--
-- 枚举字段统一 VARCHAR(32)，存英文码（见后端 Java enum）；字段注释列出可选值中文对照。
-- 关联关系由应用层维护，不建物理外键，仅在 xxx_id 上建普通索引。
-- ============================================================

-- 建库（库名与 application-dev.yaml 的 jdbc url 一致；若想改名，同步改此处置与 yaml）
CREATE DATABASE IF NOT EXISTS `dev_assist`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE `dev_assist`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 一、系统模块
-- ============================================================

DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `username`          VARCHAR(64)  NOT NULL COMMENT '登录名',
  `password`          VARCHAR(255) NOT NULL COMMENT 'BCrypt 哈希',
  `real_name`         VARCHAR(64)  NULL COMMENT '姓名',
  `email`             VARCHAR(128) NULL,
  `phone`             VARCHAR(32)  NULL,
  `avatar`            VARCHAR(255) NULL COMMENT '头像 URL',
  `status`            VARCHAR(16)  NOT NULL DEFAULT 'ENABLED' COMMENT '用户状态：ENABLED/DISABLED',
  `login_fail_count`  INT          NOT NULL DEFAULT 0 COMMENT '连续登录失败次数',
  `lock_until`        DATETIME     NULL COMMENT '锁定截止时间（空=未锁定）',
  `last_login_time`   DATETIME     NULL,
  `create_by`         BIGINT UNSIGNED NULL,
  `update_by`         BIGINT UNSIGNED NULL,
  `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`        TINYINT(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';

DROP TABLE IF EXISTS `role`;
CREATE TABLE `role` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `role_name`   VARCHAR(64)  NOT NULL COMMENT '角色名',
  `role_code`   VARCHAR(64)  NOT NULL COMMENT '角色码：ADMIN/OWNER/DEVELOPER/TESTER',
  `description` VARCHAR(255) NULL,
  `status`      VARCHAR(16)  NOT NULL DEFAULT 'ENABLED',
  `build_in`    TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否预置角色（不可删）',
  `create_by`   BIGINT UNSIGNED NULL,
  `update_by`   BIGINT UNSIGNED NULL,
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`  TINYINT(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色';

DROP TABLE IF EXISTS `user_role`;
CREATE TABLE `user_role` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT UNSIGNED NOT NULL,
  `role_id`     BIGINT UNSIGNED NOT NULL,
  `create_by`   BIGINT UNSIGNED NULL,
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
  KEY `idx_ur_role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色';

DROP TABLE IF EXISTS `permission`;
CREATE TABLE `permission` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `parent_id`  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父权限 id，0=顶级',
  `perm_name`  VARCHAR(64)  NOT NULL COMMENT '权限/菜单名',
  `perm_code`  VARCHAR(128) NOT NULL COMMENT '权限码：模块:操作 或 菜单:xxx',
  `perm_type`  VARCHAR(16)  NOT NULL COMMENT 'MENU/BUTTON',
  `path`       VARCHAR(255) NULL COMMENT '路由路径（菜单）',
  `component`  VARCHAR(255) NULL COMMENT '前端组件路径（菜单）',
  `icon`       VARCHAR(64)  NULL,
  `sort`       INT          NOT NULL DEFAULT 0,
  `visible`    TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '菜单是否可见',
  `status`     VARCHAR(16)  NOT NULL DEFAULT 'ENABLED',
  `create_by`  BIGINT UNSIGNED NULL,
  `update_by`  BIGINT UNSIGNED NULL,
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_perm_code` (`perm_code`),
  KEY `idx_perm_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限（菜单/按钮）';

DROP TABLE IF EXISTS `role_permission`;
CREATE TABLE `role_permission` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `role_id`       BIGINT UNSIGNED NOT NULL,
  `permission_id` BIGINT UNSIGNED NOT NULL,
  `create_by`     BIGINT UNSIGNED NULL,
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_perm` (`role_id`, `permission_id`),
  KEY `idx_rp_perm` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-权限';

-- ============================================================
-- 二、项目模块
-- ============================================================

DROP TABLE IF EXISTS `project`;
CREATE TABLE `project` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name`        VARCHAR(128) NOT NULL,
  `description` VARCHAR(1024) NULL,
  `tech_stack`  VARCHAR(255) NULL COMMENT '技术栈',
  `status`      VARCHAR(32)  NOT NULL DEFAULT 'NOT_STARTED' COMMENT '项目状态：NOT_STARTED/IN_PROGRESS/PAUSED/COMPLETED/ARCHIVED',
  `start_date`  DATE NULL,
  `end_date`    DATE NULL,
  `creator_id`  BIGINT UNSIGNED NOT NULL COMMENT '创建人（项目负责人）',
  `version`     INT          NOT NULL DEFAULT 0,
  `create_by`   BIGINT UNSIGNED NULL,
  `update_by`   BIGINT UNSIGNED NULL,
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`  TINYINT(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_project_creator` (`creator_id`),
  KEY `idx_project_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目';

DROP TABLE IF EXISTS `project_member`;
CREATE TABLE `project_member` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `project_id`    BIGINT UNSIGNED NOT NULL,
  `user_id`       BIGINT UNSIGNED NOT NULL,
  `project_role`  VARCHAR(32)  NOT NULL COMMENT '项目角色：OWNER/DEVELOPER/TESTER',
  `join_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_by`     BIGINT UNSIGNED NULL,
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`    TINYINT(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_member` (`project_id`, `user_id`),
  KEY `idx_pm_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目成员';

-- ============================================================
-- 三、需求模块
-- ============================================================

DROP TABLE IF EXISTS `requirement`;
CREATE TABLE `requirement` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `project_id`      BIGINT UNSIGNED NOT NULL,
  `title`           VARCHAR(255) NOT NULL,
  `description`     TEXT NULL,
  `type`            VARCHAR(32)  NULL COMMENT '需求类型',
  `priority`        VARCHAR(16)  NOT NULL DEFAULT 'MEDIUM' COMMENT 'LOW/MEDIUM/HIGH/URGENT',
  `status`          VARCHAR(32)  NOT NULL DEFAULT 'PENDING_REVIEW' COMMENT 'PENDING_REVIEW/CONFIRMED/SCHEDULED/DEVELOPING/TESTING/DONE/CLOSED',
  `estimated_effort` INT         NULL COMMENT '预估工作量（故事点）',
  `creator_id`      BIGINT UNSIGNED NOT NULL,
  `version`         INT          NOT NULL DEFAULT 0,
  `create_by`       BIGINT UNSIGNED NULL,
  `update_by`       BIGINT UNSIGNED NULL,
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`      TINYINT(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_req_project_status` (`project_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='需求';

DROP TABLE IF EXISTS `requirement_review`;
CREATE TABLE `requirement_review` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `requirement_id` BIGINT UNSIGNED NOT NULL,
  `reviewer_id`    BIGINT UNSIGNED NOT NULL,
  `opinion`        VARCHAR(1024) NULL COMMENT '评审意见',
  `result`         VARCHAR(16)  NOT NULL COMMENT 'PASS/REJECT',
  `review_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_by`      BIGINT UNSIGNED NULL,
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_review_req` (`requirement_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='需求评审记录';

-- ============================================================
-- 四、迭代模块
-- ============================================================

DROP TABLE IF EXISTS `sprint`;
CREATE TABLE `sprint` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `project_id`     BIGINT UNSIGNED NOT NULL,
  `name`           VARCHAR(128) NOT NULL,
  `goal`           VARCHAR(512) NULL COMMENT '迭代目标',
  `status`         VARCHAR(32)  NOT NULL DEFAULT 'NOT_STARTED' COMMENT 'NOT_STARTED/IN_PROGRESS/COMPLETED/ARCHIVED',
  `start_date`     DATE NULL,
  `end_date`       DATE NULL,
  `actual_end_date` DATE NULL,
  `summary`        TEXT NULL COMMENT '迭代总结',
  `version`        INT          NOT NULL DEFAULT 0,
  `create_by`      BIGINT UNSIGNED NULL,
  `update_by`      BIGINT UNSIGNED NULL,
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`     TINYINT(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_sprint_project_status` (`project_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='迭代';

DROP TABLE IF EXISTS `sprint_requirement`;
CREATE TABLE `sprint_requirement` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `sprint_id`     BIGINT UNSIGNED NOT NULL,
  `requirement_id` BIGINT UNSIGNED NOT NULL,
  `operator_id`   BIGINT UNSIGNED NOT NULL,
  `add_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `remove_time`   DATETIME     NULL COMMENT '移出时间（空=在迭代中）',
  `create_by`     BIGINT UNSIGNED NULL,
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sprint_req_active` (`sprint_id`, `requirement_id`),
  KEY `idx_sr_req` (`requirement_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='迭代-需求（N:N）';

-- ============================================================
-- 五、任务模块
-- ============================================================

DROP TABLE IF EXISTS `task`;
CREATE TABLE `task` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `project_id`     BIGINT UNSIGNED NOT NULL,
  `sprint_id`      BIGINT UNSIGNED NOT NULL COMMENT '任务须归属迭代',
  `requirement_id` BIGINT UNSIGNED NULL COMMENT '关联需求（可空）',
  `title`          VARCHAR(255) NOT NULL,
  `description`    TEXT NULL,
  `priority`       VARCHAR(16)  NOT NULL DEFAULT 'MEDIUM' COMMENT 'LOW/MEDIUM/HIGH',
  `status`         VARCHAR(32)  NOT NULL DEFAULT 'TODO' COMMENT 'TODO/IN_PROGRESS/READY_FOR_TEST/DONE/CLOSED',
  `assignee_id`    BIGINT UNSIGNED NULL COMMENT '负责人',
  `deadline`       DATE NULL,
  `estimated_hours` DECIMAL(5,1) NULL COMMENT '预估工时',
  `actual_hours`   DECIMAL(5,1) NULL COMMENT '实际工时',
  `done_time`      DATETIME     NULL COMMENT '完成时间(→DONE 写入,燃尽图统计依据)',
  `version`        INT          NOT NULL DEFAULT 0,
  `create_by`      BIGINT UNSIGNED NULL,
  `update_by`      BIGINT UNSIGNED NULL,
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`     TINYINT(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_task_sprint_status` (`sprint_id`, `status`),
  KEY `idx_task_assignee_status` (`assignee_id`, `status`),
  KEY `idx_task_req` (`requirement_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务';

DROP TABLE IF EXISTS `task_comment`;
CREATE TABLE `task_comment` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `task_id`     BIGINT UNSIGNED NOT NULL,
  `user_id`     BIGINT UNSIGNED NOT NULL,
  `content`     TEXT NOT NULL,
  `create_by`   BIGINT UNSIGNED NULL,
  `update_by`   BIGINT UNSIGNED NULL,
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`  TINYINT(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_comment_task` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务评论';

DROP TABLE IF EXISTS `work_log`;
CREATE TABLE `work_log` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `task_id`     BIGINT UNSIGNED NOT NULL,
  `user_id`     BIGINT UNSIGNED NOT NULL,
  `sprint_id`   BIGINT UNSIGNED NOT NULL COMMENT '归属迭代',
  `content`     TEXT NULL COMMENT '工作内容',
  `hours`       DECIMAL(5,1) NOT NULL COMMENT '耗时',
  `log_date`    DATE NOT NULL,
  `create_by`   BIGINT UNSIGNED NULL,
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_worklog_task` (`task_id`),
  KEY `idx_worklog_sprint` (`sprint_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作记录';

-- ============================================================
-- 六、缺陷模块
-- ============================================================

DROP TABLE IF EXISTS `bug`;
CREATE TABLE `bug` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `project_id`        BIGINT UNSIGNED NOT NULL,
  `sprint_id`         BIGINT UNSIGNED NULL COMMENT '归属迭代（可空）',
  `requirement_id`    BIGINT UNSIGNED NULL,
  `task_id`           BIGINT UNSIGNED NULL,
  `test_case_id`      BIGINT UNSIGNED NULL,
  `title`             VARCHAR(255) NOT NULL,
  `description`       TEXT NULL,
  `steps_to_reproduce` TEXT NULL COMMENT '复现步骤',
  `severity`          VARCHAR(16)  NOT NULL DEFAULT 'NORMAL' COMMENT 'MINOR/NORMAL/MAJOR/CRITICAL',
  `priority`          VARCHAR(16)  NOT NULL DEFAULT 'MEDIUM' COMMENT 'LOW/MEDIUM/HIGH',
  `status`            VARCHAR(32)  NOT NULL DEFAULT 'PENDING_CONFIRM' COMMENT 'PENDING_CONFIRM/PENDING_FIX/FIXING/PENDING_VERIFY/CLOSED/REJECTED',
  `assignee_id`       BIGINT UNSIGNED NULL COMMENT '修复人',
  `reporter_id`       BIGINT UNSIGNED NOT NULL COMMENT '提交人',
  `fix_description`   TEXT NULL COMMENT '修复说明',
  `version`           INT          NOT NULL DEFAULT 0,
  `create_by`         BIGINT UNSIGNED NULL,
  `update_by`         BIGINT UNSIGNED NULL,
  `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`        TINYINT(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_bug_project_status` (`project_id`, `status`),
  KEY `idx_bug_assignee` (`assignee_id`),
  KEY `idx_bug_sprint` (`sprint_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='缺陷';

DROP TABLE IF EXISTS `bug_attachment`;
CREATE TABLE `bug_attachment` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `bug_id`      BIGINT UNSIGNED NOT NULL,
  `file_name`   VARCHAR(255) NOT NULL,
  `file_path`   VARCHAR(512) NOT NULL,
  `file_size`   BIGINT       NOT NULL,
  `upload_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_by`   BIGINT UNSIGNED NULL,
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_attach_bug` (`bug_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='缺陷附件';

-- ============================================================
-- 七、测试用例模块
-- ============================================================

DROP TABLE IF EXISTS `test_case`;
CREATE TABLE `test_case` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `project_id`     BIGINT UNSIGNED NOT NULL,
  `sprint_id`      BIGINT UNSIGNED NULL,
  `requirement_id` BIGINT UNSIGNED NULL,
  `title`          VARCHAR(255) NOT NULL,
  `preconditions`  TEXT NULL COMMENT '前置条件',
  `steps`          TEXT NULL COMMENT '测试步骤',
  `expected_result` TEXT NULL COMMENT '预期结果',
  `priority`       VARCHAR(16)  NOT NULL DEFAULT 'MEDIUM' COMMENT 'LOW/MEDIUM/HIGH',
  `creator_id`     BIGINT UNSIGNED NOT NULL,
  `create_by`      BIGINT UNSIGNED NULL,
  `update_by`      BIGINT UNSIGNED NULL,
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`     TINYINT(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_case_project` (`project_id`),
  KEY `idx_case_sprint` (`sprint_id`),
  KEY `idx_case_req` (`requirement_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测试用例';

DROP TABLE IF EXISTS `test_execution`;
CREATE TABLE `test_execution` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `test_case_id`  BIGINT UNSIGNED NOT NULL,
  `title`         VARCHAR(255) NULL COMMENT '用例名称快照（执行时复制）',
  `preconditions` TEXT NULL COMMENT '前置条件快照',
  `steps`         TEXT NULL COMMENT '测试步骤快照',
  `expected_result` TEXT NULL COMMENT '预期结果快照',
  `executor_id`   BIGINT UNSIGNED NOT NULL,
  `sprint_id`     BIGINT UNSIGNED NULL,
  `actual_result` TEXT NULL COMMENT '实际结果',
  `result`        VARCHAR(16)  NOT NULL COMMENT 'UNEXECUTED/PASSED/FAILED/BLOCKED/SKIPPED',
  `bug_id`        BIGINT UNSIGNED NULL COMMENT '联动缺陷 id（空=未转缺陷）',
  `execute_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_by`     BIGINT UNSIGNED NULL,
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_exec_case` (`test_case_id`),
  KEY `idx_exec_sprint` (`sprint_id`),
  KEY `idx_exec_bug` (`bug_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测试执行记录';

-- ============================================================
-- 八、文档模块（含向量化）
-- ============================================================

DROP TABLE IF EXISTS `document`;
CREATE TABLE `document` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `project_id`   BIGINT UNSIGNED NOT NULL,
  `name`         VARCHAR(255) NOT NULL,
  `type`         VARCHAR(32)  NOT NULL COMMENT 'REQUIREMENT/DESIGN/API/TEST/MEETING/STANDARD/SPRINT_SUMMARY/PROJECT_SUMMARY/OTHER',
  `description`  VARCHAR(1024) NULL,
  `file_path`    VARCHAR(512) NOT NULL,
  `file_size`    BIGINT       NOT NULL,
  `parse_status` VARCHAR(32)  NOT NULL DEFAULT 'UNPARSED' COMMENT 'UNPARSED/PARSING/PARSED/FAILED',
  `uploader_id`  BIGINT UNSIGNED NOT NULL,
  `create_by`    BIGINT UNSIGNED NULL,
  `update_by`    BIGINT UNSIGNED NULL,
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`   TINYINT(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_doc_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目文档';

DROP TABLE IF EXISTS `document_chunk`;
CREATE TABLE `document_chunk` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `document_id` BIGINT UNSIGNED NOT NULL,
  `project_id`  BIGINT UNSIGNED NOT NULL,
  `content`     TEXT NOT NULL COMMENT '片段文本',
  `vector_id`   VARCHAR(128) NULL COMMENT 'Qdrant 点 ID',
  `chunk_index` INT  NOT NULL,
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_chunk_doc` (`document_id`),
  KEY `idx_chunk_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档片段（与 Qdrant 向量关联）';

-- ============================================================
-- 九、AI 记录模块
-- ============================================================

DROP TABLE IF EXISTS `ai_record`;
CREATE TABLE `ai_record` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `project_id`    BIGINT UNSIGNED NOT NULL,
  `sprint_id`     BIGINT UNSIGNED NULL,
  `module`        VARCHAR(32)  NOT NULL COMMENT '使用模块：REQUIREMENT/SPRINT/TASK/TEST/BUG/DOCUMENT/STATISTICS',
  `ai_type`       VARCHAR(32)  NOT NULL COMMENT 'REQUIREMENT_ANALYSIS/TASK_BREAKDOWN/TEST_CASE_GENERATION/BUG_ANALYSIS/KNOWLEDGE_QA/SPRINT_SUMMARY/PROJECT_SUMMARY',
  `input_content` TEXT NULL,
  `output_content` TEXT NULL,
  `status`        VARCHAR(16)  NOT NULL DEFAULT 'UNADOPTED' COMMENT 'UNADOPTED/PARTIAL/FULL',
  `creator_id`    BIGINT UNSIGNED NOT NULL,
  `adopt_time`    DATETIME     NULL,
  `create_by`     BIGINT UNSIGNED NULL,
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ai_project_type` (`project_id`, `ai_type`),
  KEY `idx_ai_creator` (`creator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 生成记录';

DROP TABLE IF EXISTS `ai_draft`;
CREATE TABLE `ai_draft` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `ai_record_id`  BIGINT UNSIGNED NOT NULL,
  `project_id`    BIGINT UNSIGNED NOT NULL,
  `target_module` VARCHAR(32)  NOT NULL COMMENT '草稿落点模块：REQUIREMENT/TASK/TESTCASE/BUG/SPRINT_SUMMARY/PROJECT_SUMMARY',
  `target_type`   VARCHAR(32)  NULL COMMENT '落点子类型，如任务下前端/后端',
  `draft_content` TEXT NOT NULL COMMENT '草稿内容（JSON）',
  `status`        VARCHAR(16)  NOT NULL DEFAULT 'PENDING_CONFIRM' COMMENT 'PENDING_CONFIRM/ADOPTED/DISCARDED',
  `creator_id`    BIGINT UNSIGNED NOT NULL,
  `confirm_time`  DATETIME     NULL,
  `create_by`     BIGINT UNSIGNED NULL,
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_draft_record` (`ai_record_id`),
  KEY `idx_draft_project_status` (`project_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 草稿（二次确认）';

-- ============================================================
-- 十、操作日志模块
-- ============================================================

DROP TABLE IF EXISTS `operation_log`;
CREATE TABLE `operation_log` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT UNSIGNED NULL,
  `module`      VARCHAR(32)  NOT NULL COMMENT '操作模块',
  `action_type` VARCHAR(32)  NOT NULL COMMENT 'CREATE/UPDATE/DELETE/STATUS_CHANGE/LOGIN/LOGOUT',
  `target_id`   BIGINT UNSIGNED NULL,
  `before_value` TEXT NULL,
  `after_value`  TEXT NULL,
  `ip_address`  VARCHAR(64) NULL,
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_log_user` (`user_id`),
  KEY `idx_log_module_time` (`module`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志';

SET FOREIGN_KEY_CHECKS = 1;


-- ============================================================
-- 初始数据
-- ============================================================

-- 角色（预置 4 个系统角色）
INSERT INTO `role` (`id`, `role_name`, `role_code`, `description`, `build_in`) VALUES
  (1, '系统管理员', 'ADMIN',     '系统级基础管理', 1),
  (2, '项目负责人', 'OWNER',     'Product Owner + Scrum Master', 1),
  (3, '开发人员',   'DEVELOPER', '完成迭代任务与缺陷修复', 1),
  (4, '测试人员',   'TESTER',    '测试用例设计与缺陷验证', 1);

-- 管理员账号 admin / admin123（password 为 BCrypt('admin123') 哈希；生产请改密）
INSERT INTO `user` (`id`, `username`, `password`, `real_name`, `status`) VALUES
  (1, 'admin', '$2a$10$.sa1NbJRQaafAt9Hik1hQuWj1E5WFtj1ErDA7kRJqxO113TfX5CGy', '系统管理员', 'ENABLED');

INSERT INTO `user_role` (`user_id`, `role_id`) VALUES (1, 1);

-- 权限：菜单(MENU) + 按钮(BUTTON)，按 SRS §12 模块组织
-- parent_id=0 顶级；菜单按模块，按钮挂在对应菜单下（此处 parent_id 统一填模块菜单 id）
INSERT INTO `permission` (`id`, `parent_id`, `perm_name`, `perm_code`, `perm_type`, `path`, `component`, `sort`, `visible`) VALUES
  -- 菜单
  (100, 0, '仪表盘',   'menu:dashboard', 'MENU', '/dashboard', 'dashboard/index', 10, 1),
  (110, 0, '项目',     'menu:project',   'MENU', '/projects',  'project/index',   20, 1),
  (120, 0, '需求',     'menu:requirement','MENU', NULL, NULL, 30, 0),
  (130, 0, '迭代',     'menu:sprint',    'MENU', NULL, NULL, 40, 0),
  (140, 0, '任务',     'menu:task',      'MENU', NULL, NULL, 50, 0),
  (150, 0, '缺陷',     'menu:bug',       'MENU', NULL, NULL, 60, 0),
  (160, 0, '测试用例', 'menu:testcase',  'MENU', NULL, NULL, 70, 0),
  (170, 0, '文档',     'menu:document',  'MENU', NULL, NULL, 80, 0),
  (180, 0, '知识库',   'menu:kb',        'MENU', NULL, NULL, 85, 0),
  (190, 0, 'AI 记录',  'menu:ai',        'MENU', NULL, NULL, 90, 0),
  (200, 0, '操作日志', 'menu:log',       'MENU', '/admin/logs',   'system/logs',   100, 1),
  (210, 0, '用户管理', 'menu:user',      'MENU', '/admin/users',  'system/users',  110, 1),
  (220, 0, '角色权限', 'menu:role',      'MENU', NULL, NULL, 120, 0),
  (230, 0, '个人中心', 'menu:profile',   'MENU', '/profile',      'profile/index', 130, 0),
  -- 用户管理按钮
  (2101, 210, '用户新增', 'user:create',       'BUTTON', NULL, NULL, 1, 1),
  (2102, 210, '用户查询', 'user:read',         'BUTTON', NULL, NULL, 2, 1),
  (2103, 210, '用户编辑', 'user:update',       'BUTTON', NULL, NULL, 3, 1),
  (2104, 210, '用户删除', 'user:delete',       'BUTTON', NULL, NULL, 4, 1),
  (2105, 210, '重置密码', 'user:reset_password','BUTTON', NULL, NULL, 5, 1),
  (2201, 220, '角色查询', 'role:read',           'BUTTON', NULL, NULL, 1, 1),
  (2202, 220, '权限分配', 'role:assign_permission','BUTTON', NULL, NULL, 2, 1),
  -- 项目
  (1101, 110, '项目创建', 'project:create',        'BUTTON', NULL, NULL, 1, 1),
  (1102, 110, '项目查询', 'project:read',          'BUTTON', NULL, NULL, 2, 1),
  (1103, 110, '项目编辑', 'project:update',        'BUTTON', NULL, NULL, 3, 1),
  (1104, 110, '项目删除', 'project:delete',        'BUTTON', NULL, NULL, 4, 1),
  (1105, 110, '项目状态', 'project:change_status', 'BUTTON', NULL, NULL, 5, 1),
  (1106, 110, '成员管理', 'project:member_manage', 'BUTTON', NULL, NULL, 6, 1),
  -- 需求
  (1201, 120, '需求创建', 'requirement:create',       'BUTTON', NULL, NULL, 1, 1),
  (1202, 120, '需求查询', 'requirement:read',         'BUTTON', NULL, NULL, 2, 1),
  (1203, 120, '需求编辑', 'requirement:update',       'BUTTON', NULL, NULL, 3, 1),
  (1204, 120, '需求删除', 'requirement:delete',       'BUTTON', NULL, NULL, 4, 1),
  (1205, 120, '需求状态', 'requirement:change_status','BUTTON', NULL, NULL, 5, 1),
  (1206, 120, '需求排期', 'requirement:schedule',     'BUTTON', NULL, NULL, 6, 1),
  (1207, 120, '需求评审', 'requirement:review',       'BUTTON', NULL, NULL, 7, 1),
  -- 迭代
  (1301, 130, '迭代创建', 'sprint:create',            'BUTTON', NULL, NULL, 1, 1),
  (1302, 130, '迭代查询', 'sprint:read',              'BUTTON', NULL, NULL, 2, 1),
  (1303, 130, '迭代编辑', 'sprint:update',            'BUTTON', NULL, NULL, 3, 1),
  (1304, 130, '迭代删除', 'sprint:delete',            'BUTTON', NULL, NULL, 4, 1),
  (1305, 130, '需求规划', 'sprint:plan_requirement',  'BUTTON', NULL, NULL, 5, 1),
  (1306, 130, '迭代总结', 'sprint:summary',           'BUTTON', NULL, NULL, 6, 1),
  -- 任务
  (1401, 140, '任务创建', 'task:create',         'BUTTON', NULL, NULL, 1, 1),
  (1402, 140, '任务查询', 'task:read',           'BUTTON', NULL, NULL, 2, 1),
  (1403, 140, '任务编辑', 'task:update',         'BUTTON', NULL, NULL, 3, 1),
  (1404, 140, '任务删除', 'task:delete',         'BUTTON', NULL, NULL, 4, 1),
  (1405, 140, '任务状态', 'task:change_status',  'BUTTON', NULL, NULL, 5, 1),
  (1406, 140, '任务分配', 'task:assign',         'BUTTON', NULL, NULL, 6, 1),
  (1407, 140, '待测试流转','task:advance_test',  'BUTTON', NULL, NULL, 7, 1),
  -- 缺陷
  (1501, 150, 'Bug 提交', 'bug:submit',        'BUTTON', NULL, NULL, 1, 1),
  (1502, 150, 'Bug 查询', 'bug:read',          'BUTTON', NULL, NULL, 2, 1),
  (1503, 150, 'Bug 更新', 'bug:update',        'BUTTON', NULL, NULL, 3, 1),
  (1504, 150, 'Bug 状态', 'bug:change_status', 'BUTTON', NULL, NULL, 4, 1),
  (1505, 150, 'Bug 分配', 'bug:assign',        'BUTTON', NULL, NULL, 5, 1),
  -- 测试用例
  (1601, 160, '用例创建', 'testcase:create',  'BUTTON', NULL, NULL, 1, 1),
  (1602, 160, '用例查询', 'testcase:read',    'BUTTON', NULL, NULL, 2, 1),
  (1603, 160, '用例编辑', 'testcase:update',  'BUTTON', NULL, NULL, 3, 1),
  (1604, 160, '用例删除', 'testcase:delete',  'BUTTON', NULL, NULL, 4, 1),
  (1605, 160, '用例执行', 'testcase:execute', 'BUTTON', NULL, NULL, 5, 1),
  -- 文档
  (1701, 170, '文档上传', 'document:upload',  'BUTTON', NULL, NULL, 1, 1),
  (1702, 170, '文档查询', 'document:read',    'BUTTON', NULL, NULL, 2, 1),
  (1703, 170, '文档编辑', 'document:update',  'BUTTON', NULL, NULL, 3, 1),
  (1704, 170, '文档下载', 'document:download','BUTTON', NULL, NULL, 4, 1),
  (1705, 170, '文档删除', 'document:delete',  'BUTTON', NULL, NULL, 5, 1),
  (1706, 170, '重新解析', 'document:reparse', 'BUTTON', NULL, NULL, 6, 1),
  -- 知识库
  (1801, 180, '知识库问答', 'kb:ask', 'BUTTON', NULL, NULL, 1, 1),
  -- AI 助手触发（生成类 6 个，问答走 kb:ask）
  (1901, 190, 'AI 需求分析', 'ai:requirement_analysis', 'BUTTON', NULL, NULL, 1, 1),
  (1902, 190, 'AI 任务拆解', 'ai:task_breakdown',       'BUTTON', NULL, NULL, 2, 1),
  (1903, 190, 'AI 用例生成', 'ai:test_case_generation', 'BUTTON', NULL, NULL, 3, 1),
  (1904, 190, 'AI Bug 分析', 'ai:bug_analysis',         'BUTTON', NULL, NULL, 4, 1),
  (1905, 190, 'AI 迭代总结', 'ai:sprint_summary',       'BUTTON', NULL, NULL, 5, 1),
  (1906, 190, 'AI 项目总结', 'ai:project_summary',      'BUTTON', NULL, NULL, 6, 1);

-- 角色-权限绑定（依据 SRS §12.1 权限矩阵）
-- 系统管理员：用户/角色/日志管理 + 全局统计
INSERT INTO `role_permission` (`role_id`, `permission_id`)
SELECT 1, `id` FROM `permission` WHERE `perm_code` IN (
  'menu:dashboard','menu:log','menu:user','menu:role','menu:profile',
  'user:create','user:read','user:update','user:delete','user:reset_password',
  'role:read','role:assign_permission');

-- 项目负责人：全部业务模块 + 全部 AI 助手
INSERT INTO `role_permission` (`role_id`, `permission_id`)
SELECT 2, `id` FROM `permission` WHERE `perm_type` = 'BUTTON'
  AND (`perm_code` LIKE 'project:%' OR `perm_code` LIKE 'requirement:%'
    OR `perm_code` LIKE 'sprint:%' OR `perm_code` LIKE 'task:%'
    OR `perm_code` LIKE 'bug:%' OR `perm_code` LIKE 'testcase:%'
    OR `perm_code` LIKE 'document:%' OR `perm_code` LIKE 'ai:%'
    OR `perm_code` = 'kb:ask')
UNION ALL
SELECT 2, `id` FROM `permission` WHERE `perm_code` IN (
  'menu:dashboard','menu:project','menu:requirement','menu:sprint','menu:task',
  'menu:bug','menu:testcase','menu:document','menu:kb','menu:ai','menu:profile');

-- 开发人员：只读业务 + 任务(本人) + Bug(修复) + 工作记录 + 知识库问答
INSERT INTO `role_permission` (`role_id`, `permission_id`)
SELECT 3, `id` FROM `permission` WHERE `perm_code` IN (
  'menu:dashboard','menu:project','menu:requirement','menu:sprint','menu:task',
  'menu:bug','menu:testcase','menu:document','menu:kb','menu:profile',
  'project:read','requirement:read','sprint:read',
  'task:read','task:update','task:change_status',
  'bug:read','bug:update',
  'testcase:read','document:read','kb:ask');

-- 测试人员：只读业务 + 用例CRUD+执行 + Bug提交/验证 + 待测试流转 + AI(用例生成/Bug分析) + 知识库
INSERT INTO `role_permission` (`role_id`, `permission_id`)
SELECT 4, `id` FROM `permission` WHERE `perm_code` IN (
  'menu:dashboard','menu:project','menu:requirement','menu:sprint','menu:task',
  'menu:bug','menu:testcase','menu:document','menu:kb','menu:profile',
  'project:read','requirement:read','sprint:read',
  'task:read','task:advance_test',
  'bug:submit','bug:read','bug:update','bug:change_status',
  'testcase:create','testcase:read','testcase:update','testcase:delete','testcase:execute',
  'document:read','kb:ask',
  'ai:test_case_generation','ai:bug_analysis');

-- ============================================================
-- 数据修复：test_execution 快照列 backfill（执行历史增强）
-- 旧执行记录（迁移前）以当前用例内容补齐快照列；
-- bug_id 无法 backfill（旧联动 Bug 不归并到具体执行记录）
-- ============================================================
UPDATE `test_execution` te
LEFT JOIN `test_case` tc ON tc.`id` = te.`test_case_id`
SET te.`title`          = COALESCE(te.`title`, tc.`title`),
    te.`preconditions`  = COALESCE(te.`preconditions`, tc.`preconditions`),
    te.`steps`          = COALESCE(te.`steps`, tc.`steps`),
    te.`expected_result`= COALESCE(te.`expected_result`, tc.`expected_result`)
WHERE te.`title` IS NULL;
