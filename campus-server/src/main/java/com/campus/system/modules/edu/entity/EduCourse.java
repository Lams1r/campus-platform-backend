package com.campus.system.modules.edu.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 课程信息表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("edu_course")
@Schema(description = "课程信息")
public class EduCourse extends BaseEntity {

    /** 课程名称 */
    @Schema(description = "课程名称")
    private String courseName;

    /** 课程编码（唯一） */
    @Schema(description = "课程编码")
    private String courseCode;

    /** 学分 */
    @Schema(description = "学分")
    private BigDecimal credit;

    /** 学时 */
    @Schema(description = "学时")
    private Integer hours;

    /** 学期（如 2025-2026-1） */
    @Schema(description = "学期")
    private String semester;

    /** 课程简介 */
    @Schema(description = "课程简介")
    private String description;

    /** 状态 0-正常 1-已结课 */
    @Schema(description = "课程状态")
    private Integer status;
}
