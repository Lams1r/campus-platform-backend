package com.campus.system.modules.svc.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.system.annotation.LogRecord;
import com.campus.system.common.api.PageResult;
import com.campus.system.common.api.Result;
import com.campus.system.common.exception.BusinessException;
import com.campus.system.modules.svc.entity.CampusNotice;
import com.campus.system.modules.svc.entity.CampusNoticeRead;
import com.campus.system.modules.svc.service.ICampusNoticeReadService;
import com.campus.system.modules.svc.service.ICampusNoticeService;
import com.campus.system.modules.sys.entity.SysUser;
import com.campus.system.modules.sys.service.ISysUserService;
import com.campus.system.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 通知公告控制器。
 */
@RestController
@RequestMapping("/svc/notice")
@RequiredArgsConstructor
@Tag(name = "通知公告", description = "校园通知发布与阅读接口")
public class CampusNoticeController {

    private final ICampusNoticeService noticeService;
    private final ICampusNoticeReadService noticeReadService;

    @Autowired(required = false)
    private ISysUserService userService;

    @GetMapping("/page")
    @Operation(summary = "分页查询公告")
    public Result<PageResult<CampusNotice>> page(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "公告类型") @RequestParam(required = false) Integer noticeType) {

        LambdaQueryWrapper<CampusNotice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CampusNotice::getStatus, 1);
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.like(CampusNotice::getTitle, keyword);
        }
        if (noticeType != null) {
            wrapper.eq(CampusNotice::getNoticeType, noticeType);
        }
        if (!SecurityUtils.hasRole("admin")) {
            List<String> roleKeys = resolveCurrentRoleKeys();
            String className = getCurrentClassName();
            wrapper.and(w -> {
                w.eq(CampusNotice::getNoticeType, 0);
                if (!roleKeys.isEmpty()) {
                    w.or(q -> q.eq(CampusNotice::getNoticeType, 1).in(CampusNotice::getTargetRole, roleKeys));
                }
                if (StrUtil.isNotBlank(className)) {
                    w.or(q -> q.eq(CampusNotice::getNoticeType, 2).eq(CampusNotice::getTargetClass, className));
                }
            });
        }
        wrapper.orderByDesc(CampusNotice::getPublishTime);

        Page<CampusNotice> page = noticeService.page(new Page<>(pageNum, pageSize), wrapper);
        List<CampusNotice> visibleRecords = page.getRecords().stream()
                .filter(this::canCurrentUserView)
                .collect(Collectors.toList());
        return Result.success(new PageResult<>(page.getTotal(), visibleRecords, (long) pageNum, (long) pageSize));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取公告详情")
    public Result<CampusNotice> detail(@Parameter(description = "公告ID") @PathVariable Long id) {
        CampusNotice notice = noticeService.getById(id);
        if (notice == null || !canCurrentUserView(notice)) {
            throw new BusinessException("公告不存在或无权查看");
        }

        try {
            Long userId = SecurityUtils.getCurrentUserId();
            long readCount = noticeReadService.count(
                    new LambdaQueryWrapper<CampusNoticeRead>()
                            .eq(CampusNoticeRead::getNoticeId, id)
                            .eq(CampusNoticeRead::getUserId, userId)
            );
            if (readCount == 0) {
                CampusNoticeRead read = new CampusNoticeRead();
                read.setNoticeId(id);
                read.setUserId(userId);
                noticeReadService.save(read);
            }
        } catch (Exception ignored) {
        }

        return Result.success(notice);
    }

    @PostMapping
    @SaCheckPermission("svc:notice:add")
    @LogRecord(module = "公告管理", type = "新增")
    @Operation(summary = "新增公告")
    public Result<Void> add(@RequestBody CampusNotice notice) {
        notice.setPublishUserId(SecurityUtils.getCurrentUserId());
        notice.setStatus(0);
        noticeService.save(notice);
        return Result.success();
    }

    @PutMapping("/{id}/publish")
    @SaCheckPermission("svc:notice:edit")
    @LogRecord(module = "公告管理", type = "发布")
    @Operation(summary = "发布公告")
    public Result<Void> publish(@Parameter(description = "公告ID") @PathVariable Long id) {
        CampusNotice notice = noticeService.getById(id);
        if (notice == null) {
            throw new BusinessException("公告不存在");
        }
        notice.setStatus(1);
        notice.setPublishTime(LocalDateTime.now());
        noticeService.updateById(notice);
        return Result.success();
    }

    @PutMapping
    @SaCheckPermission("svc:notice:edit")
    @Operation(summary = "更新公告")
    public Result<Void> update(@RequestBody CampusNotice notice) {
        noticeService.updateById(notice);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("svc:notice:delete")
    @LogRecord(module = "公告管理", type = "删除")
    @Operation(summary = "删除公告")
    public Result<Void> delete(@Parameter(description = "公告ID") @PathVariable Long id) {
        noticeService.removeById(id);
        return Result.success();
    }

    private boolean canCurrentUserView(CampusNotice notice) {
        if (notice == null) {
            return false;
        }
        if (SecurityUtils.hasRole("admin")) {
            return true;
        }
        if (!Integer.valueOf(1).equals(notice.getStatus())) {
            return false;
        }
        Integer type = notice.getNoticeType();
        if (type == null || type == 0) {
            return true;
        }
        if (type == 1) {
            return StrUtil.isNotBlank(notice.getTargetRole()) && SecurityUtils.hasRole(notice.getTargetRole());
        }
        if (type == 2) {
            String className = getCurrentClassName();
            return StrUtil.isNotBlank(className) && StrUtil.equals(className, notice.getTargetClass());
        }
        return false;
    }

    private List<String> resolveCurrentRoleKeys() {
        List<String> roleKeys = new ArrayList<>();
        if (SecurityUtils.hasRole("admin")) {
            roleKeys.add("admin");
        }
        if (SecurityUtils.hasRole("teacher")) {
            roleKeys.add("teacher");
        }
        if (SecurityUtils.hasRole("student")) {
            roleKeys.add("student");
        }
        return roleKeys;
    }

    private String getCurrentClassName() {
        if (userService == null) {
            return null;
        }
        SysUser user = userService.getById(SecurityUtils.getCurrentUserId());
        return user == null ? null : user.getClassName();
    }
}
