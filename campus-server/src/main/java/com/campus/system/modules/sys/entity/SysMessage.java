package com.campus.system.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_message")
@Schema(description = "消息通知")
public class SysMessage extends BaseEntity {

    @Schema(description = "发送者ID")
    private Long senderId;

    @Schema(description = "接收者ID")
    private Long receiverId;

    @Schema(description = "消息标题")
    private String title;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "消息类型，0-普通消息，1-系统通知")
    private Integer msgType;

    @Schema(description = "是否已读，0-未读，1-已读")
    private Integer isRead;

    @Schema(description = "阅读时间")
    private LocalDateTime readTime;
}
