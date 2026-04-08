package com.campus.system.modules.edu.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.system.common.api.PageResult;
import com.campus.system.common.api.Result;
import com.campus.system.modules.edu.entity.EduTimetable;
import com.campus.system.modules.edu.service.IEduTimetableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

/**
 * 课表管理控制器。
 */
@RestController
@RequestMapping("/edu/timetable")
@RequiredArgsConstructor
@Tag(name = "课表管理", description = "课表维护与查询接口")
public class EduTimetableController {

    private final IEduTimetableService timetableService;

    @GetMapping("/page")
    @SaCheckPermission("edu:timetable:list")
    @Operation(summary = "分页查询课表")
    public Result<PageResult<EduTimetable>> page(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") Integer pageSize,
            @Parameter(description = "学期") @RequestParam(required = false) String semester,
            @Parameter(description = "班级名称") @RequestParam(required = false) String className,
            @Parameter(description = "教师ID") @RequestParam(required = false) Long teacherId) {

        LambdaQueryWrapper<EduTimetable> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(semester)) {
            wrapper.eq(EduTimetable::getSemester, semester);
        }
        if (StrUtil.isNotBlank(className)) {
            wrapper.eq(EduTimetable::getClassName, className);
        }
        if (teacherId != null) {
            wrapper.eq(EduTimetable::getTeacherId, teacherId);
        }
        wrapper.orderByAsc(EduTimetable::getDayOfWeek).orderByAsc(EduTimetable::getStartSection);

        Page<EduTimetable> page = timetableService.page(new Page<>(pageNum, pageSize), wrapper);
        return Result.success(new PageResult<>(page.getTotal(), page.getRecords(), (long) pageNum, (long) pageSize));
    }

    @GetMapping("/my")
    @Operation(summary = "查询我的课表")
    public Result<List<EduTimetable>> myTimetable(@Parameter(description = "学期") @RequestParam String semester) {
        Long teacherId = StpUtil.getLoginIdAsLong();
        return Result.success(timetableService.list(
                new LambdaQueryWrapper<EduTimetable>()
                        .eq(EduTimetable::getTeacherId, teacherId)
                        .eq(EduTimetable::getSemester, semester)
                        .orderByAsc(EduTimetable::getDayOfWeek)
                        .orderByAsc(EduTimetable::getStartSection)
        ));
    }

    @GetMapping("/class")
    @Operation(summary = "按班级查询课表")
    public Result<List<EduTimetable>> classTimetable(
            @Parameter(description = "班级名称") @RequestParam String className,
            @Parameter(description = "学期") @RequestParam String semester) {
        return Result.success(timetableService.list(
                new LambdaQueryWrapper<EduTimetable>()
                        .eq(EduTimetable::getClassName, className)
                        .eq(EduTimetable::getSemester, semester)
                        .orderByAsc(EduTimetable::getDayOfWeek)
                        .orderByAsc(EduTimetable::getStartSection)
        ));
    }

    @PostMapping
    @SaCheckPermission("edu:timetable:add")
    @Operation(summary = "新增课表")
    public Result<Void> add(@Valid @RequestBody EduTimetable timetable) {
        timetableService.save(timetable);
        return Result.success();
    }

    @PutMapping
    @SaCheckPermission("edu:timetable:edit")
    @Operation(summary = "更新课表")
    public Result<Void> update(@Valid @RequestBody EduTimetable timetable) {
        timetableService.updateById(timetable);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("edu:timetable:delete")
    @Operation(summary = "删除课表")
    public Result<Void> delete(@Parameter(description = "课表ID") @PathVariable Long id) {
        timetableService.removeById(id);
        return Result.success();
    }
}
