package com.campus.system.modules.sys.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.campus.system.annotation.LogRecord;
import com.campus.system.common.api.PageResult;
import com.campus.system.common.api.Result;
import com.campus.system.modules.sys.dto.SysUserCreateDTO;
import com.campus.system.modules.sys.dto.SysUserQueryDTO;
import com.campus.system.modules.sys.dto.SysUserUpdateDTO;
import com.campus.system.modules.sys.service.ISysUserService;
import com.campus.system.modules.sys.vo.SysUserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户管理控制器。
 */
@RestController
@RequestMapping("/sys/user")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "系统用户的查询与维护接口")
public class SysUserController {

    private final ISysUserService userService;

    @GetMapping("/page")
    @SaCheckPermission("sys:user:list")
    @Operation(summary = "分页查询用户列表", description = "按关键字、用户类型和状态分页查询系统用户")
    public Result<PageResult<SysUserVO>> page(@ParameterObject SysUserQueryDTO query) {
        return Result.success(userService.queryUserPage(query));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("sys:user:query")
    @Operation(summary = "获取用户详情")
    public Result<SysUserVO> detail(@Parameter(description = "用户ID") @PathVariable Long id) {
        return Result.success(userService.getUserDetail(id));
    }

    @PostMapping
    @SaCheckPermission("sys:user:add")
    @LogRecord(module = "用户管理", type = "新增")
    @Operation(summary = "新增用户", description = "创建新的系统用户并绑定角色")
    public Result<Void> create(@Valid @RequestBody SysUserCreateDTO dto) {
        userService.createUser(dto);
        return Result.success();
    }

    @PutMapping
    @SaCheckPermission("sys:user:edit")
    @LogRecord(module = "用户管理", type = "修改")
    @Operation(summary = "更新用户", description = "修改用户基础信息和角色绑定")
    public Result<Void> update(@Valid @RequestBody SysUserUpdateDTO dto) {
        userService.updateUser(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("sys:user:delete")
    @LogRecord(module = "用户管理", type = "删除")
    @Operation(summary = "删除用户")
    public Result<Void> delete(@Parameter(description = "用户ID") @PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success();
    }

    @PutMapping("/{id}/status/{status}")
    @SaCheckPermission("sys:user:edit")
    @LogRecord(module = "用户管理", type = "状态变更")
    @Operation(summary = "切换账号状态", description = "修改用户账号状态并在启用时清空锁定信息")
    public Result<Void> toggleStatus(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Parameter(description = "账号状态，0-正常，1-停用，2-锁定") @PathVariable Integer status) {
        userService.toggleStatus(id, status);
        return Result.success();
    }

    @PutMapping("/{id}/resetPwd")
    @SaCheckRole("admin")
    @LogRecord(module = "用户管理", type = "重置密码")
    @Operation(summary = "重置用户密码")
    public Result<Void> resetPassword(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Parameter(description = "新密码") @RequestParam String newPassword) {
        userService.resetPassword(id, newPassword);
        return Result.success();
    }

    @PostMapping("/import")
    @SaCheckRole("admin")
    @LogRecord(module = "用户管理", type = "导入")
    @Operation(summary = "导入用户", description = "通过 Excel 批量导入用户数据")
    public Result<String> importUsers(@Parameter(description = "Excel 文件") @RequestParam("file") MultipartFile file) {
        return Result.success(userService.importUsers(file));
    }

    @GetMapping("/export")
    @SaCheckRole("admin")
    @Operation(summary = "导出用户", description = "按查询条件导出用户数据为 Excel")
    public void exportUsers(@ParameterObject SysUserQueryDTO query, HttpServletResponse response) {
        userService.exportUsers(query, response);
    }
}
