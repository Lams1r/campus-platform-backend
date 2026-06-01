package com.campus.system.modules.edu.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.system.common.api.PageResult;
import com.campus.system.common.api.Result;
import com.campus.system.common.exception.BusinessException;
import com.campus.system.modules.edu.entity.EduCourseEvaluation;
import com.campus.system.modules.edu.service.IEduCourseEvaluationService;
import com.campus.system.modules.sys.entity.SysUser;
import com.campus.system.modules.sys.service.ISysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/edu/evaluation")
@RequiredArgsConstructor
@Tag(name = "课程评价", description = "课程评价提交与查询接口")
public class EduEvaluationController {

    private final IEduCourseEvaluationService evaluationService;
    private final ISysUserService userService;

    @GetMapping("/page")
    @Operation(summary = "分页查询课程评价")
    public Result<PageResult<EduCourseEvaluation>> page(
            @RequestParam Long courseId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        Page<EduCourseEvaluation> page = evaluationService.page(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<EduCourseEvaluation>()
                        .eq(EduCourseEvaluation::getCourseId, courseId)
                        .orderByDesc(EduCourseEvaluation::getId));
        return Result.success(new PageResult<>(page.getTotal(), page.getRecords(), (long) pageNum, (long) pageSize));
    }

    @GetMapping("/my")
    @Operation(summary = "学生查看自己的评价")
    public Result<List<EduCourseEvaluation>> myEvaluations() {
        Long studentId = StpUtil.getLoginIdAsLong();
        return Result.success(evaluationService.list(
                new LambdaQueryWrapper<EduCourseEvaluation>()
                        .eq(EduCourseEvaluation::getStudentId, studentId)
                        .orderByDesc(EduCourseEvaluation::getId)));
    }

    @PostMapping
    @Operation(summary = "提交课程评价（首次）")
    public Result<Void> submit(@RequestBody EduCourseEvaluation evaluation) {
        Long studentId = StpUtil.getLoginIdAsLong();
        evaluation.setStudentId(studentId);

        // 校验是否已评价
        long count = evaluationService.count(
                new LambdaQueryWrapper<EduCourseEvaluation>()
                        .eq(EduCourseEvaluation::getCourseId, evaluation.getCourseId())
                        .eq(EduCourseEvaluation::getStudentId, studentId));
        if (count > 0) throw new BusinessException("您已对该课程提交过评价，请使用修改功能");

        validateEvaluation(evaluation);
        evaluationService.save(evaluation);
        return Result.success();
    }

    @PutMapping
    @Operation(summary = "修改课程评价")
    public Result<Void> update(@RequestBody EduCourseEvaluation evaluation) {
        Long studentId = StpUtil.getLoginIdAsLong();

        EduCourseEvaluation existing = evaluationService.getOne(
                new LambdaQueryWrapper<EduCourseEvaluation>()
                        .eq(EduCourseEvaluation::getCourseId, evaluation.getCourseId())
                        .eq(EduCourseEvaluation::getStudentId, studentId)
                        .last("LIMIT 1"), false);
        if (existing == null) throw new BusinessException("您尚未对该课程提交过评价");

        validateEvaluation(evaluation);
        existing.setStarRating(evaluation.getStarRating());
        existing.setContent(evaluation.getContent());
        evaluationService.updateById(existing);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @SaCheckRole("admin")
    @Operation(summary = "管理员删除评价")
    public Result<Void> delete(@PathVariable Long id) {
        evaluationService.removeById(id);
        return Result.success();
    }

    private void validateEvaluation(EduCourseEvaluation e) {
        if (e.getStarRating() == null || e.getStarRating() < 1 || e.getStarRating() > 5) {
            throw new BusinessException("星级评分需在1到5之间");
        }
        if (StrUtil.isNotBlank(e.getContent()) && e.getContent().length() > 200) {
            throw new BusinessException("文字评价不能超过200字");
        }
    }
}
