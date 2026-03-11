-- ============================================================
-- 一体化智慧校园系统 - 后勤服务组 DDL (campus_)
-- 数据库: campus_system | 字符集: utf8mb4 | 引擎: InnoDB
-- ============================================================
USE `campus_system`;

-- -----------------------------------------------------------
-- 1. 系统公告表 campus_notice
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `campus_notice`;
CREATE TABLE `campus_notice` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title`           VARCHAR(200) NOT NULL                COMMENT '公告标题',
  `content`         TEXT         NOT NULL                COMMENT '公告内容（富文本）',
  `notice_type`     TINYINT      DEFAULT 0               COMMENT '类型 0-全体通知 1-角色定向 2-班级定向',
  `target_role`     VARCHAR(50)  DEFAULT NULL            COMMENT '目标角色标识（定向时使用）',
  `target_class`    VARCHAR(100) DEFAULT NULL            COMMENT '目标班级（定向时使用）',
  `publish_user_id` BIGINT       NOT NULL                COMMENT '发布人ID',
  `status`          TINYINT      NOT NULL DEFAULT 0      COMMENT '状态 0-草稿 1-已发布',
  `publish_time`    DATETIME     DEFAULT NULL            COMMENT '发布时间',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`      TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  INDEX `idx_publish_time` (`publish_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统公告表';

-- -----------------------------------------------------------
-- 2. 公告已读状态表 campus_notice_read
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `campus_notice_read`;
CREATE TABLE `campus_notice_read` (
  `id`            BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `notice_id`     BIGINT   NOT NULL                COMMENT '公告ID',
  `user_id`       BIGINT   NOT NULL                COMMENT '用户ID',
  `read_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
  `create_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`    TINYINT  NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notice_user` (`notice_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告已读状态表';

-- -----------------------------------------------------------
-- 3. 宿舍楼栋表 campus_dormitory_building
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `campus_dormitory_building`;
CREATE TABLE `campus_dormitory_building` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `building_name`   VARCHAR(100) NOT NULL                COMMENT '楼栋名称',
  `building_code`   VARCHAR(50)  NOT NULL                COMMENT '楼栋编号',
  `floor_count`     INT          DEFAULT 0               COMMENT '楼层数',
  `manager_name`    VARCHAR(50)  DEFAULT NULL            COMMENT '宿管员姓名',
  `manager_phone`   VARCHAR(20)  DEFAULT NULL            COMMENT '宿管员电话',
  `remark`          VARCHAR(500) DEFAULT NULL            COMMENT '备注',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`      TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_building_code` (`building_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宿舍楼栋表';

-- -----------------------------------------------------------
-- 4. 宿舍房间表 campus_dormitory_room
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `campus_dormitory_room`;
CREATE TABLE `campus_dormitory_room` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `building_id`     BIGINT       NOT NULL                COMMENT '所属楼栋ID',
  `room_code`       VARCHAR(50)  NOT NULL                COMMENT '房间号',
  `floor`           INT          DEFAULT 1               COMMENT '所在楼层',
  `bed_count`       INT          NOT NULL DEFAULT 4      COMMENT '床位总数',
  `used_count`      INT          NOT NULL DEFAULT 0      COMMENT '已入住数',
  `status`          TINYINT      NOT NULL DEFAULT 0      COMMENT '状态 0-正常 1-满员 2-维修中',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`      TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_building_room` (`building_id`, `room_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宿舍房间表';

-- -----------------------------------------------------------
-- 5. 宿舍分配/入住表 campus_dormitory_allocation
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `campus_dormitory_allocation`;
CREATE TABLE `campus_dormitory_allocation` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `room_id`       BIGINT       NOT NULL                COMMENT '房间ID',
  `student_id`    BIGINT       NOT NULL                COMMENT '学生用户ID',
  `bed_number`    INT          DEFAULT NULL            COMMENT '床位号',
  `check_in_date` DATE         DEFAULT NULL            COMMENT '入住日期',
  `check_out_date` DATE        DEFAULT NULL            COMMENT '退宿日期',
  `status`        TINYINT      NOT NULL DEFAULT 0      COMMENT '状态 0-在住 1-已退宿',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`    TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_room_student` (`room_id`, `student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宿舍分配入住表';

-- -----------------------------------------------------------
-- 6. 报修工单表 campus_repair_order
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `campus_repair_order`;
CREATE TABLE `campus_repair_order` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_no`         VARCHAR(64)  NOT NULL                COMMENT '工单流水号',
  `applicant_id`     BIGINT       NOT NULL                COMMENT '报修人ID',
  `room_id`          BIGINT       DEFAULT NULL            COMMENT '关联房间ID',
  `title`            VARCHAR(200) NOT NULL                COMMENT '报修主题',
  `description`      TEXT         NOT NULL                COMMENT '问题描述',
  `image_paths`      VARCHAR(1000) DEFAULT NULL           COMMENT '损坏照片路径（多张逗号分隔）',
  `urgency_level`    TINYINT      NOT NULL DEFAULT 0      COMMENT '紧急程度 0-普通 1-紧急 2-非常紧急',
  `status`           TINYINT      NOT NULL DEFAULT 0      COMMENT '状态 0-待处理 1-处理中 2-已完成 3-已验收',
  `handler_id`       BIGINT       DEFAULT NULL            COMMENT '处理人ID（宿管/维修工）',
  `handle_time`      DATETIME     DEFAULT NULL            COMMENT '开始处理时间',
  `finish_time`      DATETIME     DEFAULT NULL            COMMENT '完成时间',
  `finish_remark`    VARCHAR(500) DEFAULT NULL            COMMENT '完成备注',
  `verify_user_id`   BIGINT       DEFAULT NULL            COMMENT '验收人ID',
  `verify_time`      DATETIME     DEFAULT NULL            COMMENT '验收时间',
  `verify_score`     TINYINT      DEFAULT NULL            COMMENT '满意度评分 1-5',
  `verify_remark`    VARCHAR(500) DEFAULT NULL            COMMENT '验收评价',
  `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`       TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  INDEX `idx_applicant` (`applicant_id`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报修工单表';

-- -----------------------------------------------------------
-- 7. 校园卡消费记录表 campus_card_record
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `campus_card_record`;
CREATE TABLE `campus_card_record` (
  `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `student_id`      BIGINT        NOT NULL                COMMENT '学生用户ID',
  `card_no`         VARCHAR(50)   DEFAULT NULL            COMMENT '校园卡号',
  `transaction_type` TINYINT      DEFAULT 0               COMMENT '交易类型 0-消费 1-充值',
  `amount`          DECIMAL(10,2) NOT NULL                COMMENT '交易金额',
  `balance`         DECIMAL(10,2) DEFAULT NULL            COMMENT '余额',
  `location`        VARCHAR(200)  DEFAULT NULL            COMMENT '消费地点',
  `transaction_time` DATETIME     NOT NULL                COMMENT '交易时间',
  `remark`          VARCHAR(200)  DEFAULT NULL            COMMENT '备注',
  `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`      TINYINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  INDEX `idx_student` (`student_id`),
  INDEX `idx_transaction_time` (`transaction_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='校园卡消费记录表';

-- -----------------------------------------------------------
-- 8. 校园卡挂失表 campus_card_loss
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `campus_card_loss`;
CREATE TABLE `campus_card_loss` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `student_id`    BIGINT       NOT NULL                COMMENT '学生用户ID',
  `card_no`       VARCHAR(50)  NOT NULL                COMMENT '校园卡号',
  `status`        TINYINT      NOT NULL DEFAULT 0      COMMENT '状态 0-已挂失 1-已解挂 2-已补办',
  `loss_time`     DATETIME     NOT NULL                COMMENT '挂失时间',
  `unlock_time`   DATETIME     DEFAULT NULL            COMMENT '解挂时间',
  `remark`        VARCHAR(200) DEFAULT NULL            COMMENT '备注',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`    TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  INDEX `idx_student` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='校园卡挂失表';

-- -----------------------------------------------------------
-- 9. 图书信息表 campus_book
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `campus_book`;
CREATE TABLE `campus_book` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `book_name`     VARCHAR(200) NOT NULL                COMMENT '书名',
  `author`        VARCHAR(100) DEFAULT NULL            COMMENT '作者',
  `isbn`          VARCHAR(30)  DEFAULT NULL            COMMENT 'ISBN编号',
  `publisher`     VARCHAR(100) DEFAULT NULL            COMMENT '出版社',
  `category`      VARCHAR(100) DEFAULT NULL            COMMENT '分类',
  `total_count`   INT          NOT NULL DEFAULT 1      COMMENT '馆藏总量',
  `available_count` INT        NOT NULL DEFAULT 1      COMMENT '可借数量',
  `location`      VARCHAR(100) DEFAULT NULL            COMMENT '存放位置',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`    TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  INDEX `idx_book_name` (`book_name`),
  INDEX `idx_isbn` (`isbn`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书信息表';

-- -----------------------------------------------------------
-- 10. 图书借阅记录表 campus_book_borrow
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `campus_book_borrow`;
CREATE TABLE `campus_book_borrow` (
  `id`              BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `book_id`         BIGINT   NOT NULL                COMMENT '图书ID',
  `student_id`      BIGINT   NOT NULL                COMMENT '借阅学生ID',
  `borrow_time`     DATETIME NOT NULL                COMMENT '借出时间',
  `due_time`        DATETIME NOT NULL                COMMENT '应归还时间',
  `return_time`     DATETIME DEFAULT NULL            COMMENT '实际归还时间',
  `status`          TINYINT  NOT NULL DEFAULT 0      COMMENT '状态 0-借阅中 1-已归还 2-逾期',
  `overdue_days`    INT      DEFAULT 0               COMMENT '逾期天数',
  `remark`          VARCHAR(200) DEFAULT NULL        COMMENT '备注',
  `create_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`      TINYINT  NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  INDEX `idx_student` (`student_id`),
  INDEX `idx_book` (`book_id`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书借阅记录表';

-- -----------------------------------------------------------
-- 11. 大屏数据快照表 campus_dashboard_snapshot
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `campus_dashboard_snapshot`;
CREATE TABLE `campus_dashboard_snapshot` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `snapshot_key`    VARCHAR(100) NOT NULL                COMMENT '快照标识（如 attendance_rate, repair_rate）',
  `snapshot_data`   JSON         NOT NULL                COMMENT '快照JSON数据',
  `snapshot_time`   DATETIME     NOT NULL                COMMENT '快照生成时间',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`      TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_snapshot_key` (`snapshot_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='大屏数据快照表';
