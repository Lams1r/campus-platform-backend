package com.campus.system.modules.edu.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 成绩申诉记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("edu_score_appeal")
@Schema(description = "成绩申诉记录")
public class EduScoreAppeal extends BaseEntity {

    @Schema(description = "成绩记录ID")
    private Long scoreId;

    @Schema(description = "申诉学生ID")
    private Long studentId;

    @Schema(description = "申诉理由")
    private String reason;

    @Schema(description = "佐证附件路径")
    private String attachmentPath;

    @Schema(description = "处理状态，0-待处理，1-已受理，2-已驳回")
    private Integer status;

    @Schema(description = "处理人ID")
    private Long handlerId;

    @Schema(description = "处理时间")
    private LocalDateTime handleTime;

    @Schema(description = "处理结果")
    private String handleResult;
}
