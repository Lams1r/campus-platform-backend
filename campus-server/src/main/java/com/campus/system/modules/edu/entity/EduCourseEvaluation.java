package com.campus.system.modules.edu.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 课程评价。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("edu_course_evaluation")
@Schema(description = "课程评价")
public class EduCourseEvaluation extends BaseEntity {

    @Schema(description = "课程ID")
    private Long courseId;

    @Schema(description = "评价学生ID")
    private Long studentId;

    @Schema(description = "星级评分，1到5分")
    private Integer starRating;

    @Schema(description = "文字评价")
    private String content;
}
