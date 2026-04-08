package com.campus.system.modules.svc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 看板快照。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("campus_dashboard_snapshot")
@Schema(description = "看板统计快照")
public class CampusDashboardSnapshot extends BaseEntity {

    @Schema(description = "快照标识")
    private String snapshotKey;

    @Schema(description = "快照JSON数据")
    private String snapshotData;

    @Schema(description = "快照生成时间")
    private LocalDateTime snapshotTime;
}
