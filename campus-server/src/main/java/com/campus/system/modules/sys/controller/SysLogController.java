package com.campus.system.modules.sys.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.system.common.api.PageResult;
import com.campus.system.common.api.Result;
import com.campus.system.modules.sys.entity.SysLoginLog;
import com.campus.system.modules.sys.entity.SysOperateLog;
import com.campus.system.modules.sys.service.ISysLoginLogService;
import com.campus.system.modules.sys.service.ISysOperateLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 日志管理控制器。
 */
@RestController
@RequestMapping("/sys/log")
@RequiredArgsConstructor
@Tag(name = "日志管理", description = "操作日志与登录日志查询接口")
public class SysLogController {

    private final ISysOperateLogService operateLogService;
    private final ISysLoginLogService loginLogService;

    @GetMapping("/operate")
    @SaCheckPermission("sys:log:list")
    @Operation(summary = "分页查询操作日志")
    public Result<PageResult<SysOperateLog>> operatePage(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") Integer pageSize,
            @Parameter(description = "模块名称") @RequestParam(required = false) String module,
            @Parameter(description = "操作类型") @RequestParam(required = false) String operateType) {

        LambdaQueryWrapper<SysOperateLog> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(module)) {
            wrapper.like(SysOperateLog::getModule, module);
        }
        if (StrUtil.isNotBlank(operateType)) {
            wrapper.eq(SysOperateLog::getOperateType, operateType);
        }
        wrapper.orderByDesc(SysOperateLog::getId);

        Page<SysOperateLog> page = operateLogService.page(new Page<>(pageNum, pageSize), wrapper);
        return Result.success(new PageResult<>(page.getTotal(), page.getRecords(), (long) pageNum, (long) pageSize));
    }

    @GetMapping("/login")
    @SaCheckPermission("sys:log:list")
    @Operation(summary = "分页查询登录日志")
    public Result<PageResult<SysLoginLog>> loginPage(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") Integer pageSize,
            @Parameter(description = "用户名") @RequestParam(required = false) String username) {

        LambdaQueryWrapper<SysLoginLog> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(username)) {
            wrapper.like(SysLoginLog::getUsername, username);
        }
        wrapper.orderByDesc(SysLoginLog::getId);

        Page<SysLoginLog> page = loginLogService.page(new Page<>(pageNum, pageSize), wrapper);
        return Result.success(new PageResult<>(page.getTotal(), page.getRecords(), (long) pageNum, (long) pageSize));
    }
}
