package com.campus.system.modules.svc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 宿舍入住分配记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("campus_dormitory_allocation")
@Schema(description = "宿舍入住分配记录")
public class CampusDormitoryAllocation extends BaseEntity {

    @Schema(description = "房间ID")
    private Long roomId;

    @Schema(description = "学生用户ID")
    private Long studentId;

    @Schema(description = "床位号")
    private Integer bedNumber;

    @Schema(description = "入住日期")
    private LocalDate checkInDate;

    @Schema(description = "退宿日期")
    private LocalDate checkOutDate;

    @Schema(description = "状态，0-在住，1-已退宿")
    private Integer status;
}
