package com.campus.system.modules.edu.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.system.annotation.LogRecord;
import com.campus.system.common.api.PageResult;
import com.campus.system.common.api.Result;
import com.campus.system.common.exception.BusinessException;
import com.campus.system.modules.edu.entity.EduLeaveRequest;
import com.campus.system.modules.edu.service.IEduLeaveRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 请假审批控制器
 * 流程：学生提交请假 → 教师/管理员审批（通过/驳回）
 */
@RestController
@RequestMapping("/edu/leave")
@RequiredArgsConstructor
public class EduLeaveController {

    private final IEduLeaveRequestService leaveService;

    /**
     * 学生提交请假申请
     */
    @PostMapping("/submit")
    public Result<Void> submit(@RequestBody EduLeaveRequest request) {
        Long studentId = StpUtil.getLoginIdAsLong();
        request.setStudentId(studentId);
        request.setStatus(0); // 待审批
        leaveService.save(request);
        return Result.success();
    }

    /**
     * 查询我的请假记录（学生端）
     */
    @GetMapping("/my")
    public Result<PageResult<EduLeaveRequest>> myLeaves(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long studentId = StpUtil.getLoginIdAsLong();
        Page<EduLeaveRequest> page = leaveService.page(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<EduLeaveRequest>()
                        .eq(EduLeaveRequest::getStudentId, studentId)
                        .orderByDesc(EduLeaveRequest::getId)
        );
        return Result.success(new PageResult<>(page.getTotal(), page.getRecords(), (long) pageNum, (long) pageSize));
    }

    /**
     * 教师/管理员分页查询待审批/全部请假单
     */
    @GetMapping("/page")
    @SaCheckPermission("edu:leave:list")
    public Result<PageResult<EduLeaveRequest>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long courseId) {

        LambdaQueryWrapper<EduLeaveRequest> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(EduLeaveRequest::getStatus, status);
        if (courseId != null) wrapper.eq(EduLeaveRequest::getCourseId, courseId);
        wrapper.orderByDesc(EduLeaveRequest::getId);

        Page<EduLeaveRequest> page = leaveService.page(new Page<>(pageNum, pageSize), wrapper);
        return Result.success(new PageResult<>(page.getTotal(), page.getRecords(), (long) pageNum, (long) pageSize));
    }

    /**
     * 审批请假（通过/驳回）
     */
    @PutMapping("/{id}/approve")
    @SaCheckPermission("edu:leave:approve")
    @LogRecord(module = "请假管理", type = "审批")
    public Result<Void> approve(
            @PathVariable Long id,
            @RequestParam Integer status,
            @RequestParam(required = false) String remark) {

        if (status != 1 && status != 2) throw new BusinessException("审批状态只能为 1(通过) 或 2(驳回)");

        EduLeaveRequest leave = leaveService.getById(id);
        if (leave == null) throw new BusinessException("请假记录不存在");
        if (leave.getStatus() != 0) throw new BusinessException("该请假单已处理，不可重复审批");

        leave.setStatus(status);
        leave.setApproverId(StpUtil.getLoginIdAsLong());
        leave.setApproveTime(LocalDateTime.now());
        leave.setApproveRemark(remark);
        leaveService.updateById(leave);
        return Result.success();
    }
}
