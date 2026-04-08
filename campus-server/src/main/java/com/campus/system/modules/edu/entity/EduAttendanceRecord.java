package com.campus.system.modules.edu.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 考勤记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("edu_attendance_record")
@Schema(description = "考勤记录")
public class EduAttendanceRecord extends BaseEntity {

    @Schema(description = "考勤场次ID")
    private Long sessionId;

    @Schema(description = "学生用户ID")
    private Long studentId;

    @Schema(description = "签到时间")
    private LocalDateTime signTime;

    @Schema(description = "签到状态，0-已签到，1-缺勤，2-请假，3-补签")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
