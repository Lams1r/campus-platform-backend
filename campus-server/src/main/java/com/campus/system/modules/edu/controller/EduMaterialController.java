package com.campus.system.modules.edu.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.system.annotation.LogRecord;
import com.campus.system.common.api.Result;
import com.campus.system.common.exception.BusinessException;
import com.campus.system.modules.edu.entity.EduCourseMaterial;
import com.campus.system.modules.edu.service.IEduCourseMaterialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 课件资料控制器。
 */
@RestController
@RequestMapping("/edu/material")
@RequiredArgsConstructor
@Tag(name = "课件资料", description = "课程资料上传、下载与管理接口")
public class EduMaterialController {

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "zip", "rar", "jpg", "png"
    );
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private final IEduCourseMaterialService materialService;

    @Value("${campus.upload-path:./uploads}")
    private String uploadPath;

    @GetMapping("/list")
    @Operation(summary = "查询课程资料列表")
    public Result<List<EduCourseMaterial>> list(@Parameter(description = "课程ID") @RequestParam Long courseId) {
        return Result.success(materialService.list(
                new LambdaQueryWrapper<EduCourseMaterial>()
                        .eq(EduCourseMaterial::getCourseId, courseId)
                        .orderByDesc(EduCourseMaterial::getId)
        ));
    }

    @PostMapping("/upload")
    @SaCheckPermission("edu:material:upload")
    @LogRecord(module = "课件管理", type = "上传")
    @Operation(summary = "上传课程资料")
    public Result<EduCourseMaterial> upload(
            @Parameter(description = "课程ID") @RequestParam Long courseId,
            @Parameter(description = "资料文件") @RequestParam("file") MultipartFile file) throws IOException {

        if (file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("文件大小不能超过10MB");
        }

        String originalName = file.getOriginalFilename();
        String ext = FileUtil.extName(originalName);
        if (!ALLOWED_EXTENSIONS.contains(ext != null ? ext.toLowerCase() : "")) {
            throw new BusinessException("不允许上传该类型文件，仅支持: " + String.join(", ", ALLOWED_EXTENSIONS));
        }

        String md5 = DigestUtil.md5Hex(file.getInputStream());
        long existCount = materialService.count(
                new LambdaQueryWrapper<EduCourseMaterial>()
                        .eq(EduCourseMaterial::getCourseId, courseId)
                        .eq(EduCourseMaterial::getFileMd5, md5)
        );
        if (existCount > 0) {
            throw new BusinessException("该文件已存在，无需重复上传");
        }

        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        String dirPath = uploadPath + "/material/" + courseId;
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File dest = new File(dir, storedName);
        file.transferTo(dest);

        EduCourseMaterial material = new EduCourseMaterial();
        material.setCourseId(courseId);
        material.setUploadUserId(StpUtil.getLoginIdAsLong());
        material.setFileName(originalName);
        material.setFilePath(dirPath + "/" + storedName);
        material.setFileSize(file.getSize());
        material.setFileType(file.getContentType());
        material.setFileMd5(md5);
        material.setDownloadCount(0);
        materialService.save(material);

        return Result.success(material);
    }

    @GetMapping("/download/{id}")
    @Operation(summary = "下载课程资料")
    public ResponseEntity<Resource> download(@Parameter(description = "资料ID") @PathVariable Long id) {
        EduCourseMaterial material = materialService.getById(id);
        if (material == null) {
            throw new BusinessException("资料不存在");
        }

        File file = new File(material.getFilePath());
        if (!file.exists()) {
            throw new BusinessException("文件已丢失，请联系管理员");
        }

        materialService.update(new LambdaUpdateWrapper<EduCourseMaterial>()
                .eq(EduCourseMaterial::getId, id)
                .setSql("download_count = download_count + 1"));

        Resource resource = new FileSystemResource(file);
        String encodedName = URLEncoder.encode(material.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("edu:material:delete")
    @LogRecord(module = "课件管理", type = "删除")
    @Operation(summary = "删除课程资料")
    public Result<Void> delete(@Parameter(description = "资料ID") @PathVariable Long id) {
        EduCourseMaterial material = materialService.getById(id);
        if (material != null) {
            FileUtil.del(material.getFilePath());
            materialService.removeById(id);
        }
        return Result.success();
    }
}
