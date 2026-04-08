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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 课程评价控制器。
 */
@RestController
@RequestMapping("/edu/evaluation")
@RequiredArgsConstructor
@Tag(name = "课程评价", description = "课程评价提交与查询接口")
public class EduEvaluationController {

    private final IEduCourseEvaluationService evaluationService;

    @GetMapping("/page")
    @Operation(summary = "分页查询课程评价")
    public Result<PageResult<EduCourseEvaluation>> page(
            @Parameter(description = "课程ID") @RequestParam Long courseId,
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize) {

        Page<EduCourseEvaluation> page = evaluationService.page(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<EduCourseEvaluation>()
                        .eq(EduCourseEvaluation::getCourseId, courseId)
                        .orderByDesc(EduCourseEvaluation::getId)
        );
        return Result.success(new PageResult<>(page.getTotal(), page.getRecords(), (long) pageNum, (long) pageSize));
    }

    @PostMapping
    @Operation(summary = "提交课程评价")
    public Result<Void> submit(@Valid @RequestBody EduCourseEvaluation evaluation) {
        Long studentId = StpUtil.getLoginIdAsLong();
        evaluation.setStudentId(studentId);

        long count = evaluationService.count(
                new LambdaQueryWrapper<EduCourseEvaluation>()
                        .eq(EduCourseEvaluation::getCourseId, evaluation.getCourseId())
                        .eq(EduCourseEvaluation::getStudentId, studentId)
        );
        if (count > 0) {
            throw new BusinessException("您已对该课程提交过评价，不可重复提交");
        }

        if (evaluation.getStarRating() == null || evaluation.getStarRating() < 1 || evaluation.getStarRating() > 5) {
            throw new BusinessException("星级评分需在1到5之间");
        }
        if (StrUtil.isNotBlank(evaluation.getContent()) && evaluation.getContent().length() > 200) {
            throw new BusinessException("文字评价不能超过200字");
        }
        evaluationService.save(evaluation);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @SaCheckRole("admin")
    @Operation(summary = "删除课程评价")
    public Result<Void> delete(@Parameter(description = "评价ID") @PathVariable Long id) {
        evaluationService.removeById(id);
        return Result.success();
    }
}
