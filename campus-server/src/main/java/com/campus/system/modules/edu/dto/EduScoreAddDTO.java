package com.campus.system.modules.edu.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 成绩录入请求 DTO
 */
@Data
@Schema(name = "成绩录入请求", description = "录入成绩时提交的参数")
public class EduScoreAddDTO {

    @NotNull(message = "课程ID不能为空")
    @Schema(description = "课程ID")
    private Long courseId;

    @NotNull(message = "学生ID不能为空")
    @Schema(description = "学生ID")
    private Long studentId;

    @NotNull(message = "成绩类型不能为空")
    @Schema(description = "成绩类型，0-百分制，1-等级制")
    private Integer scoreType;

    @Schema(description = "百分制成绩（scoreType=0 时必填，0~100）")
    @DecimalMin(value = "0", message = "成绩不能低于0分")
    @DecimalMax(value = "100", message = "成绩不能高于100分")
    @Digits(integer = 3, fraction = 1, message = "成绩格式不合法，最多保留1位小数")
    private BigDecimal score;

    @Schema(description = "等级成绩（scoreType=1 时必填，如 A/B/C/D/F）")
    private String scoreLevel;

    @NotBlank(message = "学期不能为空")
    @Schema(description = "学期，如 2025-2026-1")
    private String semester;
}
