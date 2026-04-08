package com.campus.system.modules.svc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 宿舍房间信息。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("campus_dormitory_room")
@Schema(description = "宿舍房间信息")
public class CampusDormitoryRoom extends BaseEntity {

    @Schema(description = "所属宿舍楼ID")
    private Long buildingId;

    @Schema(description = "房间号")
    private String roomCode;

    @Schema(description = "所在楼层")
    private Integer floor;

    @Schema(description = "床位总数")
    private Integer bedCount;

    @Schema(description = "已入住人数")
    private Integer usedCount;

    @Schema(description = "房间状态，0-正常，1-满员，2-维修中")
    private Integer status;
}
