-- ============================================================
-- 一体化智慧校园系统 - 教研链组 DDL (edu_)
-- 数据库: campus_system | 字符集: utf8mb4 | 引擎: InnoDB
-- ============================================================
USE `campus_system`;

-- -----------------------------------------------------------
-- 1. 课程信息表 edu_course
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `edu_course`;
CREATE TABLE `edu_course` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `course_name`   VARCHAR(100)  NOT NULL                COMMENT '课程名称',
  `course_code`   VARCHAR(50)   NOT NULL                COMMENT '课程编码（唯一）',
  `credit`        DECIMAL(3,1)  DEFAULT 0               COMMENT '学分',
  `hours`         INT           DEFAULT 0               COMMENT '学时',
  `semester`      VARCHAR(50)   DEFAULT NULL            COMMENT '学期（如 2025-2026-1）',
  `description`   VARCHAR(500)  DEFAULT NULL            COMMENT '课程简介',
  `status`        TINYINT       NOT NULL DEFAULT 0      COMMENT '状态 0-正常 1-已结课',
  `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`    TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_code` (`course_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程信息表';

-- -----------------------------------------------------------
-- 2. 课程-教师关联表 edu_course_teacher
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `edu_course_teacher`;
CREATE TABLE `edu_course_teacher` (
  `id`            BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `course_id`     BIGINT   NOT NULL                COMMENT '课程ID',
  `teacher_id`    BIGINT   NOT NULL                COMMENT '教师用户ID',
  `create_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`    TINYINT  NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_teacher` (`course_id`, `teacher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程与教师关联表';

-- -----------------------------------------------------------
-- 3. 课程-班级关联表 edu_course_class
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `edu_course_class`;
CREATE TABLE `edu_course_class` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `course_id`     BIGINT       NOT NULL                COMMENT '课程ID',
  `class_name`    VARCHAR(100) NOT NULL                COMMENT '行政班级名称',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`    TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_class` (`course_id`, `class_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程与班级关联表';

-- -----------------------------------------------------------
-- 4. 课表/排课表 edu_timetable
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `edu_timetable`;
CREATE TABLE `edu_timetable` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `course_id`     BIGINT       NOT NULL                COMMENT '课程ID',
  `teacher_id`    BIGINT       NOT NULL                COMMENT '教师用户ID',
  `class_name`    VARCHAR(100) DEFAULT NULL            COMMENT '班级名称',
  `day_of_week`   TINYINT      NOT NULL                COMMENT '星期几 1-7',
  `start_section` TINYINT      NOT NULL                COMMENT '开始节次',
  `end_section`   TINYINT      NOT NULL                COMMENT '结束节次',
  `classroom`     VARCHAR(100) DEFAULT NULL            COMMENT '教室地点',
  `start_week`    INT          DEFAULT 1               COMMENT '起始周',
  `end_week`      INT          DEFAULT 18              COMMENT '结束周',
  `semester`      VARCHAR(50)  DEFAULT NULL            COMMENT '学期',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`    TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  INDEX `idx_course` (`course_id`),
  INDEX `idx_teacher` (`teacher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课表排课表';

-- -----------------------------------------------------------
-- 5. 教学资料/课件表 edu_course_material
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `edu_course_material`;
CREATE TABLE `edu_course_material` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `course_id`      BIGINT       NOT NULL                COMMENT '课程ID',
  `upload_user_id` BIGINT       NOT NULL                COMMENT '上传者ID（教师）',
  `file_name`      VARCHAR(200) NOT NULL                COMMENT '文件原始名称',
  `file_path`      VARCHAR(500) NOT NULL                COMMENT '存储路径',
  `file_size`      BIGINT       DEFAULT 0               COMMENT '文件大小（字节）',
  `file_type`      VARCHAR(50)  DEFAULT NULL            COMMENT '文件MIME类型',
  `file_md5`       VARCHAR(64)  DEFAULT NULL            COMMENT '文件MD5（防重复上传）',
  `download_count` INT          DEFAULT 0               COMMENT '下载次数',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  INDEX `idx_course` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教学课件资料表';

-- -----------------------------------------------------------
-- 6. 考勤场次表 edu_attendance_session
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `edu_attendance_session`;
CREATE TABLE `edu_attendance_session` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `course_id`       BIGINT       NOT NULL                COMMENT '课程ID',
  `teacher_id`      BIGINT       NOT NULL                COMMENT '发起教师ID',
  `class_name`      VARCHAR(100) DEFAULT NULL            COMMENT '考勤班级',
  `session_code`    VARCHAR(64)  NOT NULL                COMMENT '场次编码（短时缓存键）',
  `duration_minutes` INT         NOT NULL DEFAULT 30     COMMENT '签到有效时长（分钟）',
  `start_time`      DATETIME     NOT NULL                COMMENT '签到开始时间',
  `end_time`        DATETIME     NOT NULL                COMMENT '签到截止时间',
  `status`          TINYINT      NOT NULL DEFAULT 0      COMMENT '状态 0-进行中 1-已结束',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`      TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_code` (`session_code`),
  INDEX `idx_course` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤场次表';

-- -----------------------------------------------------------
-- 7. 考勤签到记录表 edu_attendance_record
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `edu_attendance_record`;
CREATE TABLE `edu_attendance_record` (
  `id`             BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `session_id`     BIGINT   NOT NULL                COMMENT '考勤场次ID',
  `student_id`     BIGINT   NOT NULL                COMMENT '学生用户ID',
  `sign_time`      DATETIME DEFAULT NULL            COMMENT '实际签到时间',
  `status`         TINYINT  NOT NULL DEFAULT 0      COMMENT '状态 0-已签到 1-缺勤 2-请假 3-补签',
  `remark`         VARCHAR(200) DEFAULT NULL        COMMENT '备注（补签原因等）',
  `create_time`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`     TINYINT  NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_student` (`session_id`, `student_id`),
  INDEX `idx_student` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤签到记录表';

-- -----------------------------------------------------------
-- 8. 请假申请表 edu_leave_request
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `edu_leave_request`;
CREATE TABLE `edu_leave_request` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `student_id`      BIGINT       NOT NULL                COMMENT '申请学生ID',
  `course_id`       BIGINT       DEFAULT NULL            COMMENT '关联课程ID',
  `session_id`      BIGINT       DEFAULT NULL            COMMENT '关联考勤场次ID',
  `leave_type`      TINYINT      DEFAULT 0               COMMENT '请假类型 0-事假 1-病假 2-其他',
  `reason`          VARCHAR(500) NOT NULL                COMMENT '请假事由',
  `start_time`      DATETIME     NOT NULL                COMMENT '请假开始时间',
  `end_time`        DATETIME     NOT NULL                COMMENT '请假结束时间',
  `attachment_path`  VARCHAR(500) DEFAULT NULL            COMMENT '附件举证图片路径',
  `status`          TINYINT      NOT NULL DEFAULT 0      COMMENT '状态 0-待审批 1-已通过 2-已驳回',
  `approver_id`     BIGINT       DEFAULT NULL            COMMENT '审批人ID',
  `approve_time`    DATETIME     DEFAULT NULL            COMMENT '审批时间',
  `approve_remark`  VARCHAR(500) DEFAULT NULL            COMMENT '审批意见',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`      TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  INDEX `idx_student` (`student_id`),
  INDEX `idx_course` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='请假申请表';

-- -----------------------------------------------------------
-- 9. 成绩记录表 edu_score
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `edu_score`;
CREATE TABLE `edu_score` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `course_id`      BIGINT        NOT NULL                COMMENT '课程ID',
  `student_id`     BIGINT        NOT NULL                COMMENT '学生ID',
  `teacher_id`     BIGINT        NOT NULL                COMMENT '录入教师ID',
  `score`          DECIMAL(5,2)  DEFAULT NULL            COMMENT '分数',
  `score_type`     TINYINT       DEFAULT 0               COMMENT '评分制 0-百分制 1-等级制',
  `score_level`    VARCHAR(10)   DEFAULT NULL            COMMENT '等级（A/B/C/D/F，等级制时使用）',
  `semester`       VARCHAR(50)   DEFAULT NULL            COMMENT '学期',
  `status`         TINYINT       NOT NULL DEFAULT 0      COMMENT '状态 0-待审 1-已驳回 2-已归档',
  `audit_user_id`  BIGINT        DEFAULT NULL            COMMENT '审核管理员ID',
  `audit_time`     DATETIME      DEFAULT NULL            COMMENT '审核时间',
  `audit_remark`   VARCHAR(500)  DEFAULT NULL            COMMENT '审核/驳回意见',
  `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`     TINYINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_student` (`course_id`, `student_id`, `semester`),
  INDEX `idx_student` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成绩记录表';

-- -----------------------------------------------------------
-- 10. 成绩申诉/复议表 edu_score_appeal
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `edu_score_appeal`;
CREATE TABLE `edu_score_appeal` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `score_id`         BIGINT       NOT NULL                COMMENT '成绩记录ID',
  `student_id`       BIGINT       NOT NULL                COMMENT '申诉学生ID',
  `reason`           VARCHAR(500) NOT NULL                COMMENT '申诉理由',
  `attachment_path`  VARCHAR(500) DEFAULT NULL            COMMENT '佐证图片路径',
  `status`           TINYINT      NOT NULL DEFAULT 0      COMMENT '状态 0-待处理 1-已受理 2-已驳回',
  `handler_id`       BIGINT       DEFAULT NULL            COMMENT '处理人ID',
  `handle_time`      DATETIME     DEFAULT NULL            COMMENT '处理时间',
  `handle_result`    VARCHAR(500) DEFAULT NULL            COMMENT '处理结果',
  `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`       TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  INDEX `idx_score` (`score_id`),
  INDEX `idx_student` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成绩申诉复议表';

-- -----------------------------------------------------------
-- 11. 课程评价表 edu_course_evaluation
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `edu_course_evaluation`;
CREATE TABLE `edu_course_evaluation` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `course_id`      BIGINT       NOT NULL                COMMENT '课程ID',
  `student_id`     BIGINT       NOT NULL                COMMENT '评价学生ID',
  `star_rating`    TINYINT      NOT NULL DEFAULT 5      COMMENT '星级评分 1-5',
  `content`        VARCHAR(200) DEFAULT NULL            COMMENT '文字评价（上限200字）',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_student` (`course_id`, `student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程评价表';
