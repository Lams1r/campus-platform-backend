package com.campus.system.modules.svc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 宿舍楼信息。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("campus_dormitory_building")
@Schema(description = "宿舍楼信息")
public class CampusDormitoryBuilding extends BaseEntity {

    @Schema(description = "宿舍楼名称")
    private String buildingName;

    @Schema(description = "宿舍楼编号")
    private String buildingCode;

    @Schema(description = "楼层数")
    private Integer floorCount;

    @Schema(description = "宿管姓名")
    private String managerName;

    @Schema(description = "宿管电话")
    private String managerPhone;

    @Schema(description = "备注")
    private String remark;
}
