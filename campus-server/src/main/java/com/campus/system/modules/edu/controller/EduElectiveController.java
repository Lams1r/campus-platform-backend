package com.campus.system.modules.edu.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.system.common.api.Result;
import com.campus.system.common.exception.BusinessException;
import com.campus.system.modules.edu.entity.EduDropRequest;
import com.campus.system.modules.edu.entity.EduCourse;
import com.campus.system.modules.edu.entity.EduCourseClass;
import com.campus.system.modules.edu.entity.EduCourseTeacher;
import com.campus.system.modules.edu.entity.EduStudentCourse;
import com.campus.system.modules.edu.entity.EduTimetable;
import com.campus.system.modules.edu.mapper.EduCourseClassMapper;
import com.campus.system.modules.edu.mapper.EduCourseTeacherMapper;
import com.campus.system.modules.edu.service.IEduCourseService;
import com.campus.system.modules.edu.service.IEduDropRequestService;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 学生选课控制器。
 */
@RestController
@RequestMapping("/edu/elective")
@RequiredArgsConstructor
@Tag(name = "学生选课", description = "学生选课、退课与可选课程查询接口")
public class EduElectiveController {

    private final IEduStudentCourseService studentCourseService;
    private final IEduCourseService courseService;
    private final IEduTimetableService timetableService;
    private final IEduDropRequestService dropRequestService;
    private final EduCourseTeacherMapper courseTeacherMapper;
    private final EduCourseClassMapper courseClassMapper;
    private final ISysUserService userService;

    @GetMapping("/available")
    @Operation(summary = "可选课程列表", description = "返回所有已排课的课程（排除已选），学生可选择班级加入")
    public Result<List<AvailableCourseVO>> available(
            @Parameter(description = "学期") @RequestParam String semester) {

        Long studentId = StpUtil.getLoginIdAsLong();

        // 已选课程ID集合
        Set<Long> selectedCourseIds = studentCourseService.list(
                new LambdaQueryWrapper<EduStudentCourse>()
                        .eq(EduStudentCourse::getStudentId, studentId)
                        .eq(EduStudentCourse::getSemester, semester)
                        .eq(EduStudentCourse::getStatus, 0)
        ).stream().map(EduStudentCourse::getCourseId).collect(Collectors.toSet());

        // 查所有在该学期有排课记录的课程ID（去重）
        List<EduTimetable> allTimetables = timetableService.list(
                new LambdaQueryWrapper<EduTimetable>()
                        .eq(EduTimetable::getSemester, semester)
        );
        Set<Long> scheduledCourseIds = allTimetables.stream()
                .map(EduTimetable::getCourseId)
                .collect(Collectors.toSet());

        if (scheduledCourseIds.isEmpty()) {
            return Result.success(Collections.emptyList());
        }

        // 排除已选
        List<Long> availableCourseIds = scheduledCourseIds.stream()
                .filter(id -> !selectedCourseIds.contains(id))
                .collect(Collectors.toList());

        if (availableCourseIds.isEmpty()) {
            return Result.success(Collections.emptyList());
        }

        List<EduCourse> courses = courseService.listByIds(availableCourseIds);

        List<AvailableCourseVO> voList = courses.stream()
                .filter(c -> c.getStatus() == null || c.getStatus() == 0)
                .map(course -> {
                    AvailableCourseVO vo = new AvailableCourseVO();
                    vo.setCourseId(course.getId());
                    vo.setCourseName(course.getCourseName());
                    vo.setCourseCode(course.getCourseCode());
                    vo.setCredit(course.getCredit());
                    vo.setHours(course.getHours());
                    vo.setCourseType(course.getCourseType());
                    vo.setSemester(course.getSemester());

                    // 教师姓名
                    List<EduCourseTeacher> ctList = courseTeacherMapper.selectList(
                            new LambdaQueryWrapper<EduCourseTeacher>().eq(EduCourseTeacher::getCourseId, course.getId()));
                    if (!ctList.isEmpty()) {
                        List<Long> tids = ctList.stream().map(EduCourseTeacher::getTeacherId).collect(Collectors.toList());
                        List<SysUser> teachers = userService.listByIds(tids);
                        vo.setTeacherNames(teachers.stream().map(SysUser::getRealName).collect(Collectors.toList()));
                    } else {
                        vo.setTeacherNames(Collections.emptyList());
                    }

                    // 该课程可选的班级及其课表
                    List<AvailableClassVO> classOptions = new ArrayList<>();
                    List<EduCourseClass> courseClasses = courseClassMapper.selectList(
                            new LambdaQueryWrapper<EduCourseClass>().eq(EduCourseClass::getCourseId, course.getId()));

                    for (EduCourseClass cc : courseClasses) {
                        List<EduTimetable> classTimetables = allTimetables.stream()
                                .filter(t -> t.getCourseId().equals(course.getId())
                                        && cc.getClassName().equals(t.getClassName()))
                                .collect(Collectors.toList());

                        if (!classTimetables.isEmpty()) {
                            AvailableClassVO classVO = new AvailableClassVO();
                            classVO.setClassName(cc.getClassName());
                            classVO.setTimetables(classTimetables);
                            classOptions.add(classVO);
                        }
                    }

                    vo.setClassOptions(classOptions);
                    return vo;
                })
                .filter(vo -> !vo.getClassOptions().isEmpty())
                .collect(Collectors.toList());

        return Result.success(voList);
    }

    @PostMapping("/select")
    @Operation(summary = "选课", description = "学生选择课程并指定加入的班级")
    public Result<Void> select(
            @RequestParam Long courseId,
            @RequestParam String semester,
            @RequestParam String className) {
        Long studentId = StpUtil.getLoginIdAsLong();

        EduCourse course = courseService.getById(courseId);
        if (course == null) throw new BusinessException("课程不存在");

        // 校验班级是否绑定到该课程
        long classBound = courseClassMapper.selectCount(
                new LambdaQueryWrapper<EduCourseClass>()
                        .eq(EduCourseClass::getCourseId, courseId)
                        .eq(EduCourseClass::getClassName, className)
        );
        if (classBound == 0) throw new BusinessException("该课程未关联此班级");

        // 校验该班级是否有排课
        long hasTimetable = timetableService.count(
                new LambdaQueryWrapper<EduTimetable>()
                        .eq(EduTimetable::getCourseId, courseId)
                        .eq(EduTimetable::getClassName, className)
                        .eq(EduTimetable::getSemester, semester)
        );
        if (hasTimetable == 0) throw new BusinessException("该课程的该班级尚未排课");

        // 校验重复选课
        long exists = studentCourseService.count(
                new LambdaQueryWrapper<EduStudentCourse>()
                        .eq(EduStudentCourse::getCourseId, courseId)
                        .eq(EduStudentCourse::getStudentId, studentId)
                        .eq(EduStudentCourse::getSemester, semester)
                        .eq(EduStudentCourse::getStatus, 0)
        );
        if (exists > 0) throw new BusinessException("您已选过该课程");

        // 校验时间冲突：新选课程的课表 vs 已选课程的课表
        List<EduTimetable> newTimetables = timetableService.list(
                new LambdaQueryWrapper<EduTimetable>()
                        .eq(EduTimetable::getCourseId, courseId)
                        .eq(EduTimetable::getClassName, className)
                        .eq(EduTimetable::getSemester, semester)
        );

        List<Long> selectedCourseIds = studentCourseService.list(
                new LambdaQueryWrapper<EduStudentCourse>()
                        .eq(EduStudentCourse::getStudentId, studentId)
                        .eq(EduStudentCourse::getSemester, semester)
                        .eq(EduStudentCourse::getStatus, 0)
        ).stream().map(EduStudentCourse::getCourseId).collect(Collectors.toList());

        if (!selectedCourseIds.isEmpty()) {
            List<EduStudentCourse> existingSelections = studentCourseService.list(
                    new LambdaQueryWrapper<EduStudentCourse>()
                            .in(EduStudentCourse::getCourseId, selectedCourseIds)
                            .eq(EduStudentCourse::getStudentId, studentId)
                            .eq(EduStudentCourse::getSemester, semester)
                            .eq(EduStudentCourse::getStatus, 0)
            );

            for (EduStudentCourse sel : existingSelections) {
                List<EduTimetable> existTimetables = timetableService.list(
                        new LambdaQueryWrapper<EduTimetable>()
                                .eq(EduTimetable::getCourseId, sel.getCourseId())
                                .eq(EduTimetable::getClassName, sel.getClassName())
                                .eq(EduTimetable::getSemester, semester)
                );
                for (EduTimetable newTt : newTimetables) {
                    for (EduTimetable existTt : existTimetables) {
                        if (isTimeConflict(newTt, existTt)) {
                            EduCourse conflictCourse = courseService.getById(existTt.getCourseId());
                            String conflictName = conflictCourse != null ? conflictCourse.getCourseName() : "未知课程";
                            throw new BusinessException("时间冲突：与「" + conflictName + "」在周" + existTt.getDayOfWeek()
                                    + " 第" + existTt.getStartSection() + "-" + existTt.getEndSection() + "节 冲突");
                        }
                    }
                }
            }
        }

        EduStudentCourse sc = new EduStudentCourse();
        sc.setCourseId(courseId);
        sc.setStudentId(studentId);
        sc.setSemester(semester);
        sc.setClassName(className);
        sc.setStatus(0);
        studentCourseService.save(sc);

        return Result.success();
    }

    @PostMapping("/drop-request")
    @Operation(summary = "申请退课", description = "学生发起退课申请，等待教师审批")
    public Result<Void> requestDrop(@RequestBody DropRequestDTO dto) {
        Long studentId = StpUtil.getLoginIdAsLong();

        EduStudentCourse sc = studentCourseService.getById(dto.getStudentCourseId());
        if (sc == null || !sc.getStudentId().equals(studentId)) {
            throw new BusinessException("选课记录不存在");
        }
        if (sc.getStatus() != 0) {
            throw new BusinessException("该课程已退课");
        }

        // 检查是否已有待审批的退课申请
        long pending = dropRequestService.count(
                new LambdaQueryWrapper<EduDropRequest>()
                        .eq(EduDropRequest::getStudentCourseId, sc.getId())
                        .eq(EduDropRequest::getStatus, 0)
        );
        if (pending > 0) throw new BusinessException("您已提交过退课申请，请等待教师审批");

        // 查找该课程班级的教师
        List<EduTimetable> timetables = timetableService.list(
                new LambdaQueryWrapper<EduTimetable>()
                        .eq(EduTimetable::getCourseId, sc.getCourseId())
                        .eq(EduTimetable::getClassName, sc.getClassName())
                        .eq(EduTimetable::getSemester, sc.getSemester())
        );
        Long teacherId = null;
        if (!timetables.isEmpty()) {
            teacherId = timetables.get(0).getTeacherId();
        }

        EduDropRequest req = new EduDropRequest();
        req.setStudentCourseId(sc.getId());
        req.setCourseId(sc.getCourseId());
        req.setStudentId(studentId);
        req.setTeacherId(teacherId);
        req.setClassName(sc.getClassName());
        req.setReason(dto.getReason());
        req.setStatus(0);
        dropRequestService.save(req);

        return Result.success();
    }

    @GetMapping("/drop-requests/pending")
    @Operation(summary = "教师查看待审批的退课申请")
    public Result<List<DropRequestVO>> pendingDropRequests(
            @Parameter(description = "学期") @RequestParam(required = false) String semester) {

        Long teacherId = StpUtil.getLoginIdAsLong();

        LambdaQueryWrapper<EduDropRequest> wrapper = new LambdaQueryWrapper<EduDropRequest>()
                .eq(EduDropRequest::getTeacherId, teacherId)
                .eq(EduDropRequest::getStatus, 0);
        if (semester != null) {
            // 通过关联的选课记录过滤学期
            List<EduStudentCourse> semesterSelections = studentCourseService.list(
                    new LambdaQueryWrapper<EduStudentCourse>()
                            .eq(EduStudentCourse::getSemester, semester)
            );
            Set<Long> semesterScIds = semesterSelections.stream()
                    .map(EduStudentCourse::getId).collect(Collectors.toSet());
            if (semesterScIds.isEmpty()) return Result.success(Collections.emptyList());
            wrapper.in(EduDropRequest::getStudentCourseId, semesterScIds);
        }

        List<EduDropRequest> requests = dropRequestService.list(wrapper);

        List<DropRequestVO> voList = requests.stream().map(req -> {
            DropRequestVO vo = new DropRequestVO();
            vo.setId(req.getId());
            vo.setStudentCourseId(req.getStudentCourseId());
            vo.setCourseId(req.getCourseId());
            vo.setCourseName(courseService.getById(req.getCourseId()).getCourseName());
            vo.setClassName(req.getClassName());
            vo.setReason(req.getReason());
            vo.setStatus(req.getStatus());
            vo.setCreateTime(req.getCreateTime());

            SysUser student = userService.getById(req.getStudentId());
            vo.setStudentName(student != null ? student.getRealName() : "");
            vo.setStudentNo(student != null ? student.getUsername() : "");

            return vo;
        }).collect(Collectors.toList());

        return Result.success(voList);
    }

    @PutMapping("/drop-request/{id}/approve")
    @Operation(summary = "教师审批退课申请")
    public Result<Void> approveDrop(
            @PathVariable Long id,
            @RequestParam Integer status,
            @RequestParam(required = false) String remark) {

        if (status != 1 && status != 2) {
            throw new BusinessException("审批状态只能为 1(通过) 或 2(驳回)");
        }

        Long teacherId = StpUtil.getLoginIdAsLong();
        EduDropRequest req = dropRequestService.getById(id);
        if (req == null || !req.getTeacherId().equals(teacherId)) {
            throw new BusinessException("申请记录不存在");
        }
        if (req.getStatus() != 0) {
            throw new BusinessException("该申请已处理");
        }

        req.setStatus(status);
        req.setApproveTime(LocalDateTime.now());
        req.setApproveRemark(remark);
        dropRequestService.updateById(req);

        // 审批通过 → 执行退课
        if (status == 1) {
            EduStudentCourse sc = studentCourseService.getById(req.getStudentCourseId());
            if (sc != null && sc.getStatus() == 0) {
                sc.setStatus(1);
                studentCourseService.updateById(sc);
            }
        }

        return Result.success();
    }

    @GetMapping("/my")
    @Operation(summary = "我的已选课程")
    public Result<List<SelectedCourseVO>> myCourses(
            @Parameter(description = "学期") @RequestParam String semester) {

        Long studentId = StpUtil.getLoginIdAsLong();

        List<EduStudentCourse> selections = studentCourseService.list(
                new LambdaQueryWrapper<EduStudentCourse>()
                        .eq(EduStudentCourse::getStudentId, studentId)
                        .eq(EduStudentCourse::getSemester, semester)
                        .eq(EduStudentCourse::getStatus, 0)
        );

        List<SelectedCourseVO> voList = selections.stream().map(sc -> {
            EduCourse course = courseService.getById(sc.getCourseId());
            SelectedCourseVO vo = new SelectedCourseVO();
            vo.setSelectionId(sc.getId());
            vo.setCourseId(sc.getCourseId());
            vo.setCourseName(course != null ? course.getCourseName() : "");
            vo.setCourseCode(course != null ? course.getCourseCode() : "");
            vo.setCredit(course != null ? course.getCredit() : null);
            vo.setSemester(sc.getSemester());
            vo.setClassName(sc.getClassName());

            // 查教师姓名
            List<EduCourseTeacher> ctList = courseTeacherMapper.selectList(
                    new LambdaQueryWrapper<EduCourseTeacher>().eq(EduCourseTeacher::getCourseId, sc.getCourseId()));
            if (!ctList.isEmpty()) {
                List<Long> tids = ctList.stream().map(EduCourseTeacher::getTeacherId).collect(Collectors.toList());
                List<SysUser> teachers = userService.listByIds(tids);
                vo.setTeacherNames(teachers.stream().map(SysUser::getRealName).collect(Collectors.toList()));
            } else {
                vo.setTeacherNames(Collections.emptyList());
            }

            // 用学生选择的班级查课表
            List<EduTimetable> timetables = timetableService.list(
                    new LambdaQueryWrapper<EduTimetable>()
                            .eq(EduTimetable::getCourseId, sc.getCourseId())
                            .eq(EduTimetable::getSemester, semester)
                            .eq(sc.getClassName() != null, EduTimetable::getClassName, sc.getClassName())
            );
            vo.setTimetables(timetables);

            // 查退课申请状态
            EduDropRequest dropReq = dropRequestService.getOne(
                    new LambdaQueryWrapper<EduDropRequest>()
                            .eq(EduDropRequest::getStudentCourseId, sc.getId())
                            .eq(EduDropRequest::getStatus, 0)
                            .last("LIMIT 1"),
                    false
            );
            vo.setHasDropRequest(dropReq != null);

            return vo;
        }).collect(Collectors.toList());

        return Result.success(voList);
    }

    @Data
    @Schema(name = "退课申请请求")
    public static class DropRequestDTO {
        @Schema(description = "选课记录ID")
        private Long studentCourseId;
        @Schema(description = "退课原因")
        private String reason;
    }

    @Data
    @Schema(name = "退课申请记录")
    public static class DropRequestVO {
        private Long id;
        private Long studentCourseId;
        private Long courseId;
        private String courseName;
        private String className;
        private String studentName;
        private String studentNo;
        private String reason;
        private Integer status;
        private java.time.LocalDateTime createTime;
    }

    private boolean isTimeConflict(EduTimetable a, EduTimetable b) {
        if (!a.getDayOfWeek().equals(b.getDayOfWeek())) return false;
        if (a.getStartWeek() > b.getEndWeek() || b.getStartWeek() > a.getEndWeek()) return false;
        return a.getStartSection() <= b.getEndSection() && b.getStartSection() <= a.getEndSection();
    }

    @Data
    @Schema(name = "可选课程", description = "学生可选课程信息")
    public static class AvailableCourseVO {
        private Long courseId;
        private String courseName;
        private String courseCode;
        private java.math.BigDecimal credit;
        private Integer hours;
        private String courseType;
        private String semester;
        private List<String> teacherNames;
        private List<AvailableClassVO> classOptions;
    }

    @Data
    @Schema(name = "可选班级", description = "课程下可选的班级及其课表")
    public static class AvailableClassVO {
        private String className;
        private List<EduTimetable> timetables;
    }

    @Data
    @Schema(name = "已选课程", description = "学生已选课程信息")
    public static class SelectedCourseVO {
        private Long selectionId;
        private Long courseId;
        private String courseName;
        private String courseCode;
        private java.math.BigDecimal credit;
        private String semester;
        private String className;
        private List<String> teacherNames;
        private List<EduTimetable> timetables;
        private Boolean hasDropRequest;
    }
}
