package com.campus.system.modules.edu.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 考勤场次。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("edu_attendance_session")
@Schema(description = "考勤场次")
public class EduAttendanceSession extends BaseEntity {

    @Schema(description = "课程ID")
    private Long courseId;

    @Schema(description = "发起教师ID")
    private Long teacherId;

    @Schema(description = "考勤班级")
    private String className;

    @Schema(description = "签到码")
    private String sessionCode;

    @Schema(description = "签到有效时长，单位分钟")
    private Integer durationMinutes;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "状态，0-进行中，1-已结束")
    private Integer status;
}
