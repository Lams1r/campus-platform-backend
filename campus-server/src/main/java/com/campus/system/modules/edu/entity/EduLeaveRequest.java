package com.campus.system.modules.edu.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 请假申请。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("edu_leave_request")
@Schema(description = "请假申请")
public class EduLeaveRequest extends BaseEntity {

    @Schema(description = "申请学生ID")
    private Long studentId;

    @Schema(description = "关联课程ID")
    private Long courseId;

    @Schema(description = "关联考勤场次ID")
    private Long sessionId;

    @Schema(description = "请假类型，0-事假，1-病假，2-其他")
    private Integer leaveType;

    @Schema(description = "请假原因")
    private String reason;

    @Schema(description = "请假开始时间")
    private LocalDateTime startTime;

    @Schema(description = "请假结束时间")
    private LocalDateTime endTime;

    @Schema(description = "附件路径")
    private String attachmentPath;

    @Schema(description = "审批状态，0-待审批，1-已通过，2-已驳回")
    private Integer status;

    @Schema(description = "审批人ID")
    private Long approverId;

    @Schema(description = "审批时间")
    private LocalDateTime approveTime;

    @Schema(description = "审批意见")
    private String approveRemark;
}
