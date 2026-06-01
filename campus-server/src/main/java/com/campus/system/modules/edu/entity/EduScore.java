package com.campus.system.modules.edu.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 成绩记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("edu_score")
@Schema(description = "成绩记录")
public class EduScore extends BaseEntity {

    @Schema(description = "课程ID")
    private Long courseId;

    @Schema(description = "学生ID")
    private Long studentId;

    @Schema(description = "录入教师ID")
    private Long teacherId;

    @Schema(description = "平时成绩")
    private BigDecimal regularScore;

    @Schema(description = "考试成绩")
    private BigDecimal examScore;

    @Schema(description = "总成绩(加权)")
    private BigDecimal totalScore;

    @Schema(description = "等级(不及格/及格/优秀)")
    private String gradeLevel;

    @Schema(description = "学期")
    private String semester;

    @Schema(description = "审核状态，0-待审，1-已驳回，2-已归档")
    private Integer status;

    @Schema(description = "审核人ID")
    private Long auditUserId;

    @Schema(description = "审核时间")
    private LocalDateTime auditTime;

    @Schema(description = "审核备注")
    private String auditRemark;
}
