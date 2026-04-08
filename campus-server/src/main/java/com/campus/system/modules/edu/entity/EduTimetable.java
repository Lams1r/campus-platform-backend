package com.campus.system.modules.edu.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 课表安排。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("edu_timetable")
@Schema(description = "课表安排")
public class EduTimetable extends BaseEntity {

    @Schema(description = "课程ID")
    private Long courseId;

    @Schema(description = "教师用户ID")
    private Long teacherId;

    @Schema(description = "班级名称")
    private String className;

    @Schema(description = "星期几，1到7")
    private Integer dayOfWeek;

    @Schema(description = "开始节次")
    private Integer startSection;

    @Schema(description = "结束节次")
    private Integer endSection;

    @Schema(description = "教室地点")
    private String classroom;

    @Schema(description = "开始周")
    private Integer startWeek;

    @Schema(description = "结束周")
    private Integer endWeek;

    @Schema(description = "学期")
    private String semester;
}
