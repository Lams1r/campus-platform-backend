package com.campus.system.modules.edu.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 课程资料。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("edu_course_material")
@Schema(description = "课程资料")
public class EduCourseMaterial extends BaseEntity {

    @Schema(description = "课程ID")
    private Long courseId;

    @Schema(description = "上传人ID")
    private Long uploadUserId;

    @Schema(description = "原始文件名")
    private String fileName;

    @Schema(description = "存储路径")
    private String filePath;

    @Schema(description = "文件大小，单位字节")
    private Long fileSize;

    @Schema(description = "文件类型")
    private String fileType;

    @Schema(description = "文件MD5")
    private String fileMd5;

    @Schema(description = "下载次数")
    private Integer downloadCount;
}
