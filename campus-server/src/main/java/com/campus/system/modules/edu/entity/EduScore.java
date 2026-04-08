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

    @Schema(description = "成绩分数")
    private BigDecimal score;

    @Schema(description = "成绩类型，0-百分制，1-等级制")
    private Integer scoreType;

    @Schema(description = "等级成绩，如 A/B/C/D/F")
    private String scoreLevel;

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
