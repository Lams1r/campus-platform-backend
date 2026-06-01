package com.campus.system.modules.edu.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 学生选课记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("edu_student_course")
@Schema(description = "学生选课记录")
public class EduStudentCourse extends BaseEntity {

    @Schema(description = "课程ID")
    private Long courseId;

    @Schema(description = "学生用户ID")
    private Long studentId;

    @Schema(description = "学期")
    private String semester;

    @Schema(description = "学生选择的班级")
    private String className;

    @Schema(description = "状态，0-在读，1-已退课")
    private Integer status;
}
