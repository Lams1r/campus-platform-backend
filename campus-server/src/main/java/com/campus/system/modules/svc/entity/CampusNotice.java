package com.campus.system.modules.svc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 通知公告。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("campus_notice")
@Schema(description = "通知公告")
public class CampusNotice extends BaseEntity {

    @Schema(description = "公告标题")
    private String title;

    @Schema(description = "公告内容")
    private String content;

    @Schema(description = "公告类型，0-全体通知，1-角色定向，2-班级定向")
    private Integer noticeType;

    @Schema(description = "目标角色标识")
    private String targetRole;

    @Schema(description = "目标班级")
    private String targetClass;

    @Schema(description = "发布人ID")
    private Long publishUserId;

    @Schema(description = "状态，0-草稿，1-已发布")
    private Integer status;

    @Schema(description = "发布时间")
    private LocalDateTime publishTime;
}
