package com.campus.system.modules.svc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 报修工单。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("campus_repair_order")
@Schema(description = "报修工单")
public class CampusRepairOrder extends BaseEntity {

    @Schema(description = "工单编号")
    private String orderNo;

    @Schema(description = "报修人ID")
    private Long applicantId;

    @Schema(description = "关联房间ID")
    private Long roomId;

    @Schema(description = "报修标题")
    private String title;

    @Schema(description = "问题描述")
    private String description;

    @Schema(description = "损坏图片路径，多个文件以逗号分隔")
    private String imagePaths;

    @Schema(description = "紧急程度，0-普通，1-紧急，2-非常紧急")
    private Integer urgencyLevel;

    @Schema(description = "工单状态，0-待处理，1-处理中，2-已完成，3-已验收")
    private Integer status;

    @Schema(description = "处理人ID")
    private Long handlerId;

    @Schema(description = "受理时间")
    private LocalDateTime handleTime;

    @Schema(description = "完成时间")
    private LocalDateTime finishTime;

    @Schema(description = "完成备注")
    private String finishRemark;

    @Schema(description = "验收人ID")
    private Long verifyUserId;

    @Schema(description = "验收时间")
    private LocalDateTime verifyTime;

    @Schema(description = "验收评分，1到5分")
    private Integer verifyScore;

    @Schema(description = "验收评价")
    private String verifyRemark;
}
