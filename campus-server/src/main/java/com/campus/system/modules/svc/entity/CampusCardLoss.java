package com.campus.system.modules.svc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 校园卡挂失记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("campus_card_loss")
@Schema(description = "校园卡挂失记录")
public class CampusCardLoss extends BaseEntity {

    @Schema(description = "学生用户ID")
    private Long studentId;

    @Schema(description = "校园卡号")
    private String cardNo;

    @Schema(description = "状态，0-已挂失，1-已解挂，2-已补办")
    private Integer status;

    @Schema(description = "挂失时间")
    private LocalDateTime lossTime;

    @Schema(description = "解挂时间")
    private LocalDateTime unlockTime;

    @Schema(description = "备注")
    private String remark;
}
