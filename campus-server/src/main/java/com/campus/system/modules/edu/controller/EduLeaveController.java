package com.campus.system.modules.edu.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.system.annotation.LogRecord;
import com.campus.system.common.api.PageResult;
import com.campus.system.common.api.Result;
import com.campus.system.common.exception.BusinessException;
import com.campus.system.modules.edu.entity.EduLeaveRequest;
import com.campus.system.modules.edu.service.IEduLeaveRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 请假管理控制器。
 */
@RestController
@RequestMapping("/edu/leave")
@RequiredArgsConstructor
@Tag(name = "请假管理", description = "学生请假与审批接口")
public class EduLeaveController {

    private final IEduLeaveRequestService leaveService;

    @PostMapping("/submit")
    @Operation(summary = "提交请假申请")
    public Result<Void> submit(@RequestBody EduLeaveRequest request) {
        Long studentId = StpUtil.getLoginIdAsLong();
        request.setStudentId(studentId);
        request.setStatus(0);
        leaveService.save(request);
        return Result.success();
    }

    @GetMapping("/my")
    @Operation(summary = "查询我的请假记录")
    public Result<PageResult<EduLeaveRequest>> myLeaves(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize) {
        Long studentId = StpUtil.getLoginIdAsLong();
        Page<EduLeaveRequest> page = leaveService.page(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<EduLeaveRequest>()
                        .eq(EduLeaveRequest::getStudentId, studentId)
                        .orderByDesc(EduLeaveRequest::getId)
        );
        return Result.success(new PageResult<>(page.getTotal(), page.getRecords(), (long) pageNum, (long) pageSize));
    }

    @GetMapping("/page")
    @SaCheckPermission("edu:leave:list")
    @Operation(summary = "分页查询请假单")
    public Result<PageResult<EduLeaveRequest>> page(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "审批状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "课程ID") @RequestParam(required = false) Long courseId) {

        LambdaQueryWrapper<EduLeaveRequest> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(EduLeaveRequest::getStatus, status);
        }
        if (courseId != null) {
            wrapper.eq(EduLeaveRequest::getCourseId, courseId);
        }
        wrapper.orderByDesc(EduLeaveRequest::getId);

        Page<EduLeaveRequest> page = leaveService.page(new Page<>(pageNum, pageSize), wrapper);
        return Result.success(new PageResult<>(page.getTotal(), page.getRecords(), (long) pageNum, (long) pageSize));
    }

    @PutMapping("/{id}/approve")
    @SaCheckPermission("edu:leave:approve")
    @LogRecord(module = "请假管理", type = "审批")
    @Operation(summary = "审批请假申请")
    public Result<Void> approve(
            @Parameter(description = "请假ID") @PathVariable Long id,
            @Parameter(description = "审批状态，1-通过，2-驳回") @RequestParam Integer status,
            @Parameter(description = "审批备注") @RequestParam(required = false) String remark) {

        if (status != 1 && status != 2) {
            throw new BusinessException("审批状态只能为 1(通过) 或 2(驳回)");
        }

        EduLeaveRequest leave = leaveService.getById(id);
        if (leave == null) {
            throw new BusinessException("请假记录不存在");
        }
        if (leave.getStatus() != 0) {
            throw new BusinessException("该请假单已处理，不可重复审批");
        }

        leave.setStatus(status);
        leave.setApproverId(StpUtil.getLoginIdAsLong());
        leave.setApproveTime(LocalDateTime.now());
        leave.setApproveRemark(remark);
        leaveService.updateById(leave);
        return Result.success();
    }
}
