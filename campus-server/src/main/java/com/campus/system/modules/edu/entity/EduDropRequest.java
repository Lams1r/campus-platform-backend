package com.campus.system.modules.edu.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 退课申请。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("edu_drop_request")
@Schema(description = "退课申请")
public class EduDropRequest extends BaseEntity {

    @Schema(description = "选课记录ID")
    private Long studentCourseId;

    @Schema(description = "课程ID")
    private Long courseId;

    @Schema(description = "学生用户ID")
    private Long studentId;

    @Schema(description = "审批教师ID")
    private Long teacherId;

    @Schema(description = "班级")
    private String className;

    @Schema(description = "退课原因")
    private String reason;

    @Schema(description = "状态，0-待审批，1-已通过，2-已驳回")
    private Integer status;

    @Schema(description = "审批时间")
    private LocalDateTime approveTime;

    @Schema(description = "审批意见")
    private String approveRemark;
}
