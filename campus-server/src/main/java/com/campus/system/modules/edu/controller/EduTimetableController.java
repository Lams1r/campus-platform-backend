package com.campus.system.modules.edu.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.system.common.api.PageResult;
import com.campus.system.common.api.Result;
import com.campus.system.common.exception.BusinessException;
import com.campus.system.modules.edu.entity.EduCourse;
import com.campus.system.modules.edu.entity.EduStudentCourse;
import com.campus.system.modules.edu.entity.EduTimetable;
import com.campus.system.modules.edu.service.IEduCourseService;
import com.campus.system.modules.edu.service.IEduStudentCourseService;
import com.campus.system.modules.edu.service.IEduTimetableService;
import com.campus.system.modules.sys.entity.SysUser;
import com.campus.system.modules.sys.service.ISysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 课表管理控制器。
 */
@RestController
@RequestMapping("/edu/timetable")
@RequiredArgsConstructor
@Tag(name = "课表管理", description = "课表维护与查询接口")
public class EduTimetableController {

    private final IEduTimetableService timetableService;
    private final IEduCourseService courseService;
    private final IEduStudentCourseService studentCourseService;
    private final ISysUserService userService;

    @GetMapping("/page")
    @SaCheckPermission("edu:timetable:list")
    @Operation(summary = "分页查询课表")
    public Result<PageResult<TimetableVO>> page(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") Integer pageSize,
            @Parameter(description = "学期") @RequestParam(required = false) String semester,
            @Parameter(description = "班级名称") @RequestParam(required = false) String className,
            @Parameter(description = "教师ID") @RequestParam(required = false) Long teacherId) {

        LambdaQueryWrapper<EduTimetable> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(semester)) wrapper.eq(EduTimetable::getSemester, semester);
        if (StrUtil.isNotBlank(className)) wrapper.eq(EduTimetable::getClassName, className);
        if (teacherId != null) wrapper.eq(EduTimetable::getTeacherId, teacherId);
        wrapper.orderByAsc(EduTimetable::getDayOfWeek).orderByAsc(EduTimetable::getStartSection);

        Page<EduTimetable> page = timetableService.page(new Page<>(pageNum, pageSize), wrapper);

        List<TimetableVO> voList = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return Result.success(new PageResult<>(page.getTotal(), voList, (long) pageNum, (long) pageSize));
    }

    @GetMapping("/my")
    @Operation(summary = "查询我的课表")
    public Result<List<TimetableVO>> myTimetable(@Parameter(description = "学期") @RequestParam String semester) {
        Long userId = StpUtil.getLoginIdAsLong();
        boolean isStudent = StpUtil.hasRole("student");

        List<EduTimetable> list;
        if (isStudent) {
            // 学生：查已选课程的课表（用选课时记录的 className）
            List<EduStudentCourse> selections = studentCourseService.list(
                    new LambdaQueryWrapper<EduStudentCourse>()
                            .eq(EduStudentCourse::getStudentId, userId)
                            .eq(EduStudentCourse::getSemester, semester)
                            .eq(EduStudentCourse::getStatus, 0)
            );

            if (selections.isEmpty()) return Result.success(Collections.emptyList());

            list = new ArrayList<>();
            for (EduStudentCourse sel : selections) {
                if (sel.getClassName() == null) continue;
                List<EduTimetable> ttList = timetableService.list(
                        new LambdaQueryWrapper<EduTimetable>()
                                .eq(EduTimetable::getCourseId, sel.getCourseId())
                                .eq(EduTimetable::getClassName, sel.getClassName())
                                .eq(EduTimetable::getSemester, semester)
                                .orderByAsc(EduTimetable::getDayOfWeek)
                                .orderByAsc(EduTimetable::getStartSection)
                );
                list.addAll(ttList);
            }
        } else {
            // 教师：查自己课程的课表
            list = timetableService.list(
                    new LambdaQueryWrapper<EduTimetable>()
                            .eq(EduTimetable::getTeacherId, userId)
                            .eq(EduTimetable::getSemester, semester)
                            .orderByAsc(EduTimetable::getDayOfWeek)
                            .orderByAsc(EduTimetable::getStartSection)
            );
        }

        return Result.success(list.stream().map(this::toVO).collect(Collectors.toList()));
    }

    @GetMapping("/class")
    @Operation(summary = "按班级查询课表")
    public Result<List<TimetableVO>> classTimetable(
            @Parameter(description = "班级名称") @RequestParam String className,
            @Parameter(description = "学期") @RequestParam String semester) {

        List<EduTimetable> list = timetableService.list(
                new LambdaQueryWrapper<EduTimetable>()
                        .eq(EduTimetable::getClassName, className)
                        .eq(EduTimetable::getSemester, semester)
                        .orderByAsc(EduTimetable::getDayOfWeek)
                        .orderByAsc(EduTimetable::getStartSection)
        );
        return Result.success(list.stream().map(this::toVO).collect(Collectors.toList()));
    }

    @PostMapping
    @SaCheckPermission("edu:timetable:add")
    @Operation(summary = "新增排课")
    public Result<Void> add(@RequestBody EduTimetable timetable) {
        // 校验重复：同一课程+班级+学期只能排课一次
        long exists = timetableService.count(
                new LambdaQueryWrapper<EduTimetable>()
                        .eq(EduTimetable::getCourseId, timetable.getCourseId())
                        .eq(EduTimetable::getClassName, timetable.getClassName())
                        .eq(EduTimetable::getSemester, timetable.getSemester())
        );
        if (exists > 0) {
            throw new BusinessException("该课程的该班级在本学期已排课，不能重复排课");
        }

        // 校验冲突：同一班级+学期+星期+节次范围不能重复
        long classConflict = timetableService.count(
                new LambdaQueryWrapper<EduTimetable>()
                        .eq(EduTimetable::getClassName, timetable.getClassName())
                        .eq(EduTimetable::getSemester, timetable.getSemester())
                        .eq(EduTimetable::getDayOfWeek, timetable.getDayOfWeek())
                        .le(EduTimetable::getStartSection, timetable.getEndSection())
                        .ge(EduTimetable::getEndSection, timetable.getStartSection())
        );
        if (classConflict > 0) {
            throw new BusinessException("该班级在该时间段已有其他课程安排");
        }

        // 自动填充教师ID
        if (timetable.getTeacherId() == null) {
            timetable.setTeacherId(StpUtil.getLoginIdAsLong());
        }

        // 校验冲突：同一教师+学期+星期+节次范围不能重复
        long teacherConflict = timetableService.count(
                new LambdaQueryWrapper<EduTimetable>()
                        .eq(EduTimetable::getTeacherId, timetable.getTeacherId())
                        .eq(EduTimetable::getSemester, timetable.getSemester())
                        .eq(EduTimetable::getDayOfWeek, timetable.getDayOfWeek())
                        .le(EduTimetable::getStartSection, timetable.getEndSection())
                        .ge(EduTimetable::getEndSection, timetable.getStartSection())
        );
        if (teacherConflict > 0) {
            throw new BusinessException("您在该时间段已有其他课程安排，无法排课");
        }

        timetableService.save(timetable);
        return Result.success();
    }

    @PutMapping
    @SaCheckPermission("edu:timetable:edit")
    @Operation(summary = "更新排课")
    public Result<Void> update(@RequestBody EduTimetable timetable) {
        timetableService.updateById(timetable);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("edu:timetable:delete")
    @Operation(summary = "删除排课")
    public Result<Void> delete(@Parameter(description = "排课ID") @PathVariable Long id) {
        timetableService.removeById(id);
        return Result.success();
    }

    private TimetableVO toVO(EduTimetable tt) {
        TimetableVO vo = new TimetableVO();
        vo.setId(tt.getId());
        vo.setCourseId(tt.getCourseId());
        vo.setTeacherId(tt.getTeacherId());
        vo.setClassName(tt.getClassName());
        vo.setDayOfWeek(tt.getDayOfWeek());
        vo.setStartSection(tt.getStartSection());
        vo.setEndSection(tt.getEndSection());
        vo.setClassroom(tt.getClassroom());
        vo.setStartWeek(tt.getStartWeek());
        vo.setEndWeek(tt.getEndWeek());
        vo.setSemester(tt.getSemester());

        EduCourse course = courseService.getById(tt.getCourseId());
        vo.setCourseName(course != null ? course.getCourseName() : "");

        SysUser teacher = userService.getById(tt.getTeacherId());
        vo.setTeacherName(teacher != null ? teacher.getRealName() : "");

        return vo;
    }

    @Data
    @Schema(name = "课表项", description = "课表查询结果")
    public static class TimetableVO {
        private Long id;
        private Long courseId;
        private String courseName;
        private Long teacherId;
        private String teacherName;
        private String className;
        private Integer dayOfWeek;
        private Integer startSection;
        private Integer endSection;
        private String classroom;
        private Integer startWeek;
        private Integer endWeek;
        private String semester;
    }
}
