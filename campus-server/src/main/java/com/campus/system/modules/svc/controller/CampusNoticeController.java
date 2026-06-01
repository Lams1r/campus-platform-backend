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
    public Result<PageResult<NoticeVO>> page(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "关键字") @RequestParam(required = false) String keyword) {

        LambdaQueryWrapper<CampusNotice> wrapper = new LambdaQueryWrapper<>();
        if (!SecurityUtils.hasRole("admin")) {
            wrapper.eq(CampusNotice::getStatus, 1);
        }
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.like(CampusNotice::getTitle, keyword);
        }
        wrapper.orderByDesc(CampusNotice::getPublishTime);

        Page<CampusNotice> page = noticeService.page(new Page<>(pageNum, pageSize), wrapper);

        List<NoticeVO> voList = page.getRecords().stream().map(notice -> {
            NoticeVO vo = new NoticeVO();
            vo.setId(notice.getId());
            vo.setTitle(notice.getTitle());
            vo.setContent(notice.getContent());
            vo.setStatus(notice.getStatus());
            vo.setPublishTime(notice.getPublishTime());
            vo.setCreateTime(notice.getCreateTime());
            if (userService != null && notice.getPublishUserId() != null) {
                SysUser publisher = userService.getById(notice.getPublishUserId());
                vo.setPublisherName(publisher != null ? publisher.getRealName() : "");
            }
            return vo;
        }).collect(Collectors.toList());

        return Result.success(new PageResult<>(page.getTotal(), voList, (long) pageNum, (long) pageSize));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取公告详情")
    public Result<CampusNotice> detail(@Parameter(description = "公告ID") @PathVariable Long id) {
        CampusNotice notice = noticeService.getById(id);
        if (notice == null) throw new BusinessException("公告不存在");
        return Result.success(notice);
    }

    @PostMapping
    @SaCheckPermission("svc:notice:add")
    @LogRecord(module = "公告管理", type = "新增")
    @Operation(summary = "新增公告")
    public Result<Void> add(@RequestBody CampusNotice notice) {
        notice.setPublishUserId(SecurityUtils.getCurrentUserId());
        if (notice.getStatus() == null) notice.setStatus(0);
        if (notice.getStatus() == 1) notice.setPublishTime(LocalDateTime.now());
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
        // 如果状态改为发布，自动设置发布时间
        if (notice.getStatus() != null && notice.getStatus() == 1) {
            CampusNotice existing = noticeService.getById(notice.getId());
            if (existing != null && (existing.getStatus() == null || existing.getStatus() == 0)) {
                notice.setPublishTime(LocalDateTime.now());
            }
        }
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

    @lombok.Data
    public static class NoticeVO {
        private Long id;
        private String title;
        private String content;
        private Integer status;
        private String publisherName;
        private java.time.LocalDateTime publishTime;
        private java.time.LocalDateTime createTime;
    }
}
