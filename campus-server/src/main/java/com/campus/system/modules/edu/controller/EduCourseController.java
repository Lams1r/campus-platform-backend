package com.campus.system.modules.edu.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.system.annotation.LogRecord;
import com.campus.system.common.api.PageResult;
import com.campus.system.common.api.Result;
import com.campus.system.common.exception.BusinessException;
import com.campus.system.modules.edu.entity.EduCourse;
import com.campus.system.modules.edu.entity.EduCourseClass;
import com.campus.system.modules.edu.entity.EduCourseTeacher;
import com.campus.system.modules.edu.service.IEduCourseService;
import com.campus.system.modules.edu.mapper.EduCourseClassMapper;
import com.campus.system.modules.edu.mapper.EduCourseTeacherMapper;
import com.campus.system.modules.sys.entity.SysUser;
import com.campus.system.modules.sys.service.ISysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 课程管理控制器
 */
@RestController
@RequestMapping("/edu/course")
@RequiredArgsConstructor
@Tag(name = "课程管理", description = "课程信息维护与绑定管理接口")
public class EduCourseController {

    private final IEduCourseService courseService;
    private final EduCourseTeacherMapper courseTeacherMapper;
    private final EduCourseClassMapper courseClassMapper;
    private final ISysUserService userService;

    @GetMapping("/page")
    @SaCheckPermission("edu:course:list")
    @Operation(summary = "分页查询课程列表")
    public Result<PageResult<CourseListVO>> page(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "学期") @RequestParam(required = false) String semester,
            @Parameter(description = "课程状态") @RequestParam(required = false) Integer status) {

        LambdaQueryWrapper<EduCourse> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(EduCourse::getCourseName, keyword).or().like(EduCourse::getCourseCode, keyword));
        }
        if (StrUtil.isNotBlank(semester)) wrapper.eq(EduCourse::getSemester, semester);
        if (status != null) wrapper.eq(EduCourse::getStatus, status);

        // 教师只能看到自己教的课程
        if (!StpUtil.hasRole("admin")) {
            Long teacherId = StpUtil.getLoginIdAsLong();
            List<Long> myCourseIds = courseTeacherMapper.selectList(
                    new LambdaQueryWrapper<EduCourseTeacher>().eq(EduCourseTeacher::getTeacherId, teacherId)
            ).stream().map(EduCourseTeacher::getCourseId).collect(Collectors.toList());
            if (myCourseIds.isEmpty()) return Result.success(new PageResult<>(0L, Collections.emptyList(), (long) pageNum, (long) pageSize));
            wrapper.in(EduCourse::getId, myCourseIds);
        }

        wrapper.orderByDesc(EduCourse::getId);

        Page<EduCourse> page = courseService.page(new Page<>(pageNum, pageSize), wrapper);

        // 批量查询教师和班级信息
        List<CourseListVO> voList = page.getRecords().stream().map(course -> {
            CourseListVO vo = new CourseListVO();
            BeanUtil.copyProperties(course, vo);

            // 查教师姓名
            List<EduCourseTeacher> ctList = courseTeacherMapper.selectList(
                    new LambdaQueryWrapper<EduCourseTeacher>().eq(EduCourseTeacher::getCourseId, course.getId()));
            if (!ctList.isEmpty()) {
                List<Long> tids = ctList.stream().map(EduCourseTeacher::getTeacherId).collect(Collectors.toList());
                List<SysUser> teachers = userService.listByIds(tids);
                vo.setTeacherNames(teachers.stream().map(SysUser::getRealName).collect(Collectors.toList()));
                vo.setTeacherIds(tids);
            } else {
                vo.setTeacherNames(Collections.emptyList());
                vo.setTeacherIds(Collections.emptyList());
            }

            // 查班级
            List<EduCourseClass> ccList = courseClassMapper.selectList(
                    new LambdaQueryWrapper<EduCourseClass>().eq(EduCourseClass::getCourseId, course.getId()));
            vo.setClassNames(ccList.stream().map(EduCourseClass::getClassName).collect(Collectors.toList()));

            return vo;
        }).collect(Collectors.toList());

        return Result.success(new PageResult<>(page.getTotal(), voList, (long) pageNum, (long) pageSize));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("edu:course:query")
    @Operation(summary = "获取课程详情")
    public Result<CourseDetailVO> detail(@Parameter(description = "课程ID") @PathVariable Long id) {
        EduCourse course = courseService.getById(id);
        if (course == null) throw new BusinessException("课程不存在");

        List<Long> teacherIds = courseTeacherMapper.selectList(
                new LambdaQueryWrapper<EduCourseTeacher>().eq(EduCourseTeacher::getCourseId, id)
        ).stream().map(EduCourseTeacher::getTeacherId).collect(Collectors.toList());

        List<String> classNames = courseClassMapper.selectList(
                new LambdaQueryWrapper<EduCourseClass>().eq(EduCourseClass::getCourseId, id)
        ).stream().map(EduCourseClass::getClassName).collect(Collectors.toList());

        CourseDetailVO vo = new CourseDetailVO();
        vo.setCourse(course);
        vo.setTeacherIds(teacherIds);
        vo.setClassNames(classNames);
        return Result.success(vo);
    }

    @PostMapping
    @SaCheckPermission("edu:course:add")
    @LogRecord(module = "课程管理", type = "新增")
    @Operation(summary = "新增课程")
    public Result<Void> create(@Valid @RequestBody CourseCreateDTO dto) {
        long count = courseService.count(new LambdaQueryWrapper<EduCourse>().eq(EduCourse::getCourseCode, dto.getCourseCode()));
        if (count > 0) throw new BusinessException("课程编码 '" + dto.getCourseCode() + "' 已存在");

        EduCourse course = new EduCourse();
        BeanUtil.copyProperties(dto, course, "teacherIds", "classNames");
        course.setStatus(0);

        // 教师创建课程时，若未指定任课教师则自动绑定自己
        List<Long> teacherIds = dto.getTeacherIds();
        if ((teacherIds == null || teacherIds.isEmpty()) && StpUtil.hasRole("teacher")) {
            teacherIds = new ArrayList<>();
            teacherIds.add(StpUtil.getLoginIdAsLong());
        }

        courseService.createCourse(course, teacherIds, dto.getClassNames());
        return Result.success();
    }

    @PutMapping
    @SaCheckPermission("edu:course:edit")
    @LogRecord(module = "课程管理", type = "修改")
    @Operation(summary = "更新课程")
    public Result<Void> update(@Valid @RequestBody CourseCreateDTO dto) {
        if (dto.getId() == null) throw new BusinessException("课程ID不能为空");
        EduCourse course = courseService.getById(dto.getId());
        if (course == null) throw new BusinessException("课程不存在");

        BeanUtil.copyProperties(dto, course, "teacherIds", "classNames");
        courseService.updateCourse(course, dto.getTeacherIds(), dto.getClassNames());
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("edu:course:delete")
    @LogRecord(module = "课程管理", type = "删除")
    @Operation(summary = "删除课程")
    public Result<Void> delete(@Parameter(description = "课程ID") @PathVariable Long id) {
        courseService.deleteCourseWithBindings(id);
        return Result.success();
    }

    @PutMapping("/{id}/finish")
    @SaCheckPermission("edu:course:edit")
    @LogRecord(module = "课程管理", type = "结课")
    @Operation(summary = "课程结课")
    public Result<Void> finish(@Parameter(description = "课程ID") @PathVariable Long id) {
        EduCourse course = courseService.getById(id);
        if (course == null) throw new BusinessException("课程不存在");
        course.setStatus(1);
        courseService.updateById(course);
        return Result.success();
    }

    @Data
    @Schema(name = "课程新增请求", description = "新增或更新课程时提交的请求参数")
    public static class CourseCreateDTO {
        @Schema(description = "课程ID，更新时必填")
        private Long id;

        @Schema(description = "课程名称")
        private String courseName;

        @Schema(description = "课程编码")
        private String courseCode;

        @Schema(description = "学分")
        private java.math.BigDecimal credit;

        @Schema(description = "学时")
        private Integer hours;

        @Schema(description = "学期")
        private String semester;

        @Schema(description = "课程简介")
        private String description;

        @Schema(description = "课程类型")
        private String courseType;

        @Schema(description = "平时成绩占比(%)")
        private Integer regularRatio;

        @Schema(description = "考试成绩占比(%)")
        private Integer examRatio;

        @Schema(description = "授课教师ID列表")
        private List<Long> teacherIds;

        @Schema(description = "关联班级名称列表")
        private List<String> classNames;
    }

    @Data
    @Schema(name = "课程详情", description = "课程详情响应")
    public static class CourseDetailVO {
        @Schema(description = "课程基础信息")
        private EduCourse course;

        @Schema(description = "授课教师ID列表")
        private List<Long> teacherIds;

        @Schema(description = "关联班级名称列表")
        private List<String> classNames;
    }

    @Data
    @Schema(name = "课程列表项", description = "分页查询课程列表时的单条记录")
    public static class CourseListVO {
        @Schema(description = "课程ID")
        private Long id;

        @Schema(description = "课程名称")
        private String courseName;

        @Schema(description = "课程编码")
        private String courseCode;

        @Schema(description = "学分")
        private java.math.BigDecimal credit;

        @Schema(description = "学时")
        private Integer hours;

        @Schema(description = "学期")
        private String semester;

        @Schema(description = "课程简介")
        private String description;

        @Schema(description = "课程类型")
        private String courseType;

        @Schema(description = "平时成绩占比(%)")
        private Integer regularRatio;

        @Schema(description = "考试成绩占比(%)")
        private Integer examRatio;

        @Schema(description = "课程状态")
        private Integer status;

        @Schema(description = "创建时间")
        private java.time.LocalDateTime createTime;

        @Schema(description = "任课教师ID列表")
        private List<Long> teacherIds;

        @Schema(description = "任课教师姓名列表")
        private List<String> teacherNames;

        @Schema(description = "关联班级名称列表")
        private List<String> classNames;
    }
}
