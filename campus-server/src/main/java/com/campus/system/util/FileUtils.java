package com.campus.system.util;

import com.campus.system.common.constants.SystemConstants;
import com.campus.system.common.exception.BusinessException;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

/**
 * 文件上传工具类
 * 基于 Hutool 和业务规范封装白名单校验、容量限制
 */
public class FileUtils {

    /** 允许上传的文件后缀白名单 */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "doc", "docx", "xls", "xlsx", "ppt", "pptx", "pdf",
            "txt", "md",
            "jpg", "jpeg", "png", "gif", "bmp", "webp"
    );

    /**
     * 校验上传文件是否合规
     * @param file 上传文件
     */
    public static void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        // 容量限制
        if (file.getSize() > SystemConstants.MAX_UPLOAD_SIZE) {
            throw new BusinessException("上传文件大小不能超过 10MB");
        }
        // 后缀白名单检查
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
            if (!ALLOWED_EXTENSIONS.contains(ext)) {
                throw new BusinessException("不支持的文件格式: " + ext + "，仅允许文档和图片类型");
            }
        }
    }

    /**
     * 获取文件扩展名
     */
    public static String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
