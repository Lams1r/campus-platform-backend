package com.campus.system.modules.svc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("campus_dorm_swap_request")
@Schema(description = "换宿舍申请")
public class CampusDormSwapRequest extends BaseEntity {

    @Schema(description = "申请人ID")
    private Long studentId;

    @Schema(description = "当前房间ID")
    private Long currentRoomId;

    @Schema(description = "目标房间ID")
    private Long targetRoomId;

    @Schema(description = "换宿原因")
    private String reason;

    @Schema(description = "状态，0-待审批，1-已通过，2-已驳回")
    private Integer status;

    @Schema(description = "审批人ID")
    private Long approverId;

    @Schema(description = "审批时间")
    private LocalDateTime approveTime;

    @Schema(description = "审批意见")
    private String approveRemark;
}
