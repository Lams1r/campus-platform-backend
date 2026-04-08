package com.campus.system.modules.svc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 校园卡流水。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("campus_card_record")
@Schema(description = "校园卡流水")
public class CampusCardRecord extends BaseEntity {

    @Schema(description = "学生用户ID")
    private Long studentId;

    @Schema(description = "校园卡号")
    private String cardNo;

    @Schema(description = "交易类型，0-消费，1-充值")
    private Integer transactionType;

    @Schema(description = "交易金额")
    private BigDecimal amount;

    @Schema(description = "账户余额")
    private BigDecimal balance;

    @Schema(description = "交易地点")
    private String location;

    @Schema(description = "交易时间")
    private LocalDateTime transactionTime;

    @Schema(description = "备注")
    private String remark;
}
