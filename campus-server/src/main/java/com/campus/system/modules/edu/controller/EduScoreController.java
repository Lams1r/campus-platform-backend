package com.campus.system.modules.edu.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.system.annotation.LogRecord;
import com.campus.system.common.api.PageResult;
import com.campus.system.common.api.Result;
import com.campus.system.common.exception.BusinessException;
import com.campus.system.modules.edu.entity.EduScore;
import com.campus.system.modules.edu.entity.EduScoreAppeal;
import com.campus.system.modules.edu.service.IEduScoreAppealService;
import com.campus.system.modules.edu.service.IEduScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 成绩管理控制器
 * 核心防篡改机制：status=2（已归档）的成绩禁止修改
 */
@RestController
@RequestMapping("/edu/score")
@RequiredArgsConstructor
public class EduScoreController {

    private final IEduScoreService scoreService;
    private final IEduScoreAppealService appealService;

    // ============ 成绩管理 ============

    /**
     * 分页查询成绩
     */
    @GetMapping("/page")
    @SaCheckPermission("edu:score:list")
    public Result<PageResult<EduScore>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) String semester,
            @RequestParam(required = false) Integer status) {

        LambdaQueryWrapper<EduScore> wrapper = new LambdaQueryWrapper<>();
        if (courseId != null) wrapper.eq(EduScore::getCourseId, courseId);
        if (studentId != null) wrapper.eq(EduScore::getStudentId, studentId);
        if (StrUtil.isNotBlank(semester)) wrapper.eq(EduScore::getSemester, semester);
        if (status != null) wrapper.eq(EduScore::getStatus, status);
        wrapper.orderByDesc(EduScore::getId);

        Page<EduScore> page = scoreService.page(new Page<>(pageNum, pageSize), wrapper);
        return Result.success(new PageResult<>(page.getTotal(), page.getRecords(), (long) pageNum, (long) pageSize));
    }

    /**
     * 教师录入成绩
     */
    @PostMapping
    @SaCheckPermission("edu:score:add")
    @LogRecord(module = "成绩管理", type = "录入")
    public Result<Void> add(@RequestBody EduScore score) {
        score.setTeacherId(StpUtil.getLoginIdAsLong());
        score.setStatus(0); // 待审
        scoreService.save(score);
        return Result.success();
    }

    /**
     * 教师修改成绩（仅status=0或1时允许）
     */
    @PutMapping
    @SaCheckPermission("edu:score:edit")
    @LogRecord(module = "成绩管理", type = "修改")
    public Result<Void> update(@RequestBody EduScore score) {
        EduScore existing = scoreService.getById(score.getId());
        if (existing == null) throw new BusinessException("成绩记录不存在");

        // 防篡改核心拦截：已归档禁止修改
        if (existing.getStatus() == 2) {
            throw new BusinessException("终核分库已铸印查封，禁止进行改分僭越动作！");
        }

        score.setStatus(0); // 修改后重置为待审
        scoreService.updateById(score);
        return Result.success();
    }

    /**
     * 管理员审核成绩（归档 or 驳回）
     */
    @PutMapping("/{id}/audit")
    @SaCheckRole("admin")
    @LogRecord(module = "成绩管理", type = "审核")
    public Result<Void> audit(
            @PathVariable Long id,
            @RequestParam Integer status,
            @RequestParam(required = false) String remark) {

        if (status != 1 && status != 2) throw new BusinessException("审核状态只能为 1(驳回) 或 2(归档)");

        EduScore score = scoreService.getById(id);
        if (score == null) throw new BusinessException("成绩记录不存在");

        score.setStatus(status);
        score.setAuditUserId(StpUtil.getLoginIdAsLong());
        score.setAuditTime(LocalDateTime.now());
        score.setAuditRemark(remark);
        scoreService.updateById(score);
        return Result.success();
    }

    // ============ 成绩申诉 ============

    /**
     * 学生提交成绩申诉
     */
    @PostMapping("/appeal")
    public Result<Void> submitAppeal(@RequestBody EduScoreAppeal appeal) {
        Long studentId = StpUtil.getLoginIdAsLong();
        appeal.setStudentId(studentId);
        appeal.setStatus(0); // 待处理

        // 防重复申诉
        long count = appealService.count(
                new LambdaQueryWrapper<EduScoreAppeal>()
                        .eq(EduScoreAppeal::getScoreId, appeal.getScoreId())
                        .eq(EduScoreAppeal::getStudentId, studentId)
                        .ne(EduScoreAppeal::getStatus, 2) // 被驳回的可以重新申诉
        );
        if (count > 0) throw new BusinessException("该成绩已有待处理的申诉工单");

        appealService.save(appeal);
        return Result.success();
    }

    /**
     * 分页查询申诉列表
     */
    @GetMapping("/appeal/page")
    @SaCheckPermission("edu:score:list")
    public Result<PageResult<EduScoreAppeal>> appealPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status) {

        LambdaQueryWrapper<EduScoreAppeal> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(EduScoreAppeal::getStatus, status);
        wrapper.orderByDesc(EduScoreAppeal::getId);

        Page<EduScoreAppeal> page = appealService.page(new Page<>(pageNum, pageSize), wrapper);
        return Result.success(new PageResult<>(page.getTotal(), page.getRecords(), (long) pageNum, (long) pageSize));
    }

    /**
     * 处理成绩申诉（受理/驳回）
     */
    @PutMapping("/appeal/{id}/handle")
    @SaCheckPermission("edu:score:edit")
    @LogRecord(module = "成绩管理", type = "处理申诉")
    public Result<Void> handleAppeal(
            @PathVariable Long id,
            @RequestParam Integer status,
            @RequestParam(required = false) String result) {

        if (status != 1 && status != 2) throw new BusinessException("处理状态只能为 1(受理) 或 2(驳回)");

        EduScoreAppeal appeal = appealService.getById(id);
        if (appeal == null) throw new BusinessException("申诉记录不存在");
        if (appeal.getStatus() != 0) throw new BusinessException("该申诉已处理");

        appeal.setStatus(status);
        appeal.setHandlerId(StpUtil.getLoginIdAsLong());
        appeal.setHandleTime(LocalDateTime.now());
        appeal.setHandleResult(result);
        appealService.updateById(appeal);

        // 受理后将对应成绩重置为"已驳回"状态，允许教师重新修改
        if (status == 1) {
            EduScore score = scoreService.getById(appeal.getScoreId());
            if (score != null && score.getStatus() == 2) {
                score.setStatus(1); // 驳回，解锁修改
                score.setAuditRemark("因申诉受理，成绩已解锁");
                scoreService.updateById(score);
            }
        }
        return Result.success();
    }
}
