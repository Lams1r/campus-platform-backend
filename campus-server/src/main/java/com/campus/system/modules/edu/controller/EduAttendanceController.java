package com.campus.system.modules.edu.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.system.annotation.LogRecord;
import com.campus.system.common.api.PageResult;
import com.campus.system.common.api.Result;
import com.campus.system.common.exception.BusinessException;
import com.campus.system.modules.edu.entity.EduAttendanceRecord;
import com.campus.system.modules.edu.entity.EduAttendanceSession;
import com.campus.system.modules.edu.entity.EduCourse;
import com.campus.system.modules.edu.entity.EduCourseClass;
import com.campus.system.modules.edu.entity.EduStudentCourse;
import com.campus.system.modules.edu.entity.EduTimetable;
import com.campus.system.modules.edu.mapper.EduCourseClassMapper;
import com.campus.system.modules.edu.service.IEduAttendanceRecordService;
import com.campus.system.modules.edu.service.IEduAttendanceSessionService;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 考勤管理控制器。
 */
@RestController
@RequestMapping("/edu/attendance")
@RequiredArgsConstructor
@Tag(name = "考勤管理", description = "课堂签到与考勤统计接口")
public class EduAttendanceController {

    private static final String ATTENDANCE_KEY_PREFIX = "attendance:session:";

    private final IEduAttendanceSessionService sessionService;
    private final IEduAttendanceRecordService recordService;
    private final StringRedisTemplate redisTemplate;
    private final EduCourseClassMapper courseClassMapper;
    private final ISysUserService userService;
    private final IEduCourseService courseService;
    private final IEduStudentCourseService studentCourseService;
    private final IEduTimetableService timetableService;

    @PostMapping("/session/create")
    @SaCheckPermission("edu:attendance:create")
    @LogRecord(module = "考勤管理", type = "发起签到")
    @Operation(summary = "发起签到场次")
    public Result<EduAttendanceSession> createSession(@RequestBody CreateSessionDTO dto) {
        Long teacherId = StpUtil.getLoginIdAsLong();
        String sessionCode = RandomUtil.randomNumbers(6);

        EduAttendanceSession session = new EduAttendanceSession();
        session.setCourseId(dto.getCourseId());
        session.setTeacherId(teacherId);
        session.setClassName(dto.getClassName());
        session.setSessionCode(sessionCode);
        session.setDurationMinutes(dto.getDurationMinutes() != null ? dto.getDurationMinutes() : 30);
        session.setStartTime(LocalDateTime.now());
        session.setEndTime(LocalDateTime.now().plusMinutes(session.getDurationMinutes()));
        session.setStatus(0);
        sessionService.save(session);

        redisTemplate.opsForValue().set(
                ATTENDANCE_KEY_PREFIX + sessionCode,
                session.getId().toString(),
                session.getDurationMinutes(),
                TimeUnit.MINUTES
        );

        return Result.success(session);
    }

    @GetMapping("/session/active")
    @Operation(summary = "获取当前教师的进行中考勤场次", description = "用于前端恢复倒计时状态")
    public Result<EduAttendanceSession> activeSession() {
        Long teacherId = StpUtil.getLoginIdAsLong();
        EduAttendanceSession session = sessionService.getOne(
                new LambdaQueryWrapper<EduAttendanceSession>()
                        .eq(EduAttendanceSession::getTeacherId, teacherId)
                        .eq(EduAttendanceSession::getStatus, 0)
                        .orderByDesc(EduAttendanceSession::getId)
                        .last("LIMIT 1"),
                false
        );
        return Result.success(session);
    }

    @GetMapping("/session/{sessionId}/stats")
    @SaCheckPermission("edu:attendance:list")
    @Operation(summary = "查看签到统计", description = "返回已签到和未签到的学生列表")
    public Result<AttendanceStatsVO> sessionStats(@Parameter(description = "考勤场次ID") @PathVariable Long sessionId) {
        EduAttendanceSession session = sessionService.getById(sessionId);
        if (session == null) {
            throw new BusinessException("场次不存在");
        }

        // 查询已签到记录
        List<EduAttendanceRecord> records = recordService.list(
                new LambdaQueryWrapper<EduAttendanceRecord>()
                        .eq(EduAttendanceRecord::getSessionId, sessionId)
                        .orderByAsc(EduAttendanceRecord::getSignTime)
        );

        // 已签到学生ID集合
        Set<Long> signedStudentIds = records.stream()
                .map(EduAttendanceRecord::getStudentId)
                .collect(Collectors.toSet());

        // 查询该考勤班级的全部学生（从选课记录查，而非 sys_user.class_name）
        List<EduStudentCourse> classSelections = studentCourseService.list(
                new LambdaQueryWrapper<EduStudentCourse>()
                        .eq(EduStudentCourse::getCourseId, session.getCourseId())
                        .eq(EduStudentCourse::getClassName, session.getClassName())
                        .eq(EduStudentCourse::getStatus, 0)
        );
        List<Long> classStudentIds = classSelections.stream()
                .map(EduStudentCourse::getStudentId)
                .collect(Collectors.toList());
        List<SysUser> classStudents = classStudentIds.isEmpty()
                ? Collections.emptyList()
                : userService.listByIds(classStudentIds);

        // 已签到学生详情
        List<StudentSignVO> signedList = records.stream().map(r -> {
            StudentSignVO vo = new StudentSignVO();
            vo.setStudentId(r.getStudentId());
            SysUser student = userService.getById(r.getStudentId());
            vo.setUsername(student != null ? student.getUsername() : "");
            vo.setRealName(student != null ? student.getRealName() : "");
            vo.setSignTime(r.getSignTime());
            vo.setStatus(r.getStatus());
            return vo;
        }).collect(Collectors.toList());

        // 未签到学生详情
        List<StudentSignVO> unsignedList = classStudents.stream()
                .filter(s -> !signedStudentIds.contains(s.getId()))
                .map(s -> {
                    StudentSignVO vo = new StudentSignVO();
                    vo.setStudentId(s.getId());
                    vo.setUsername(s.getUsername());
                    vo.setRealName(s.getRealName());
                    vo.setSignTime(null);
                    vo.setStatus(1); // 缺勤
                    return vo;
                })
                .collect(Collectors.toList());

        AttendanceStatsVO vo = new AttendanceStatsVO();
        vo.setSession(session);
        vo.setTotalCount((long) classStudents.size());
        vo.setSignedCount((long) signedList.size());
        vo.setUnsignedCount((long) unsignedList.size());
        vo.setSignedList(signedList);
        vo.setUnsignedList(unsignedList);
        return Result.success(vo);
    }

    @PutMapping("/session/{sessionId}/finish")
    @SaCheckPermission("edu:attendance:create")
    @Operation(summary = "结束签到场次")
    public Result<Void> finishSession(@Parameter(description = "考勤场次ID") @PathVariable Long sessionId) {
        EduAttendanceSession session = sessionService.getById(sessionId);
        if (session == null) {
            throw new BusinessException("场次不存在");
        }
        session.setStatus(1);
        session.setEndTime(LocalDateTime.now());
        sessionService.updateById(session);
        redisTemplate.delete(ATTENDANCE_KEY_PREFIX + session.getSessionCode());
        return Result.success();
    }

    @GetMapping("/session/page")
    @SaCheckPermission("edu:attendance:list")
    @Operation(summary = "分页查询考勤场次")
    public Result<PageResult<SessionListVO>> sessionPage(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "课程ID") @RequestParam(required = false) Long courseId) {

        Long currentUserId = StpUtil.getLoginIdAsLong();
        boolean isStudent = StpUtil.hasRole("student");

        LambdaQueryWrapper<EduAttendanceSession> wrapper = new LambdaQueryWrapper<>();
        if (isStudent) {
            // 学生只看自己班级相关的场次
            SysUser student = userService.getById(currentUserId);
            if (student != null && student.getClassName() != null && !student.getClassName().isEmpty()) {
                wrapper.eq(EduAttendanceSession::getClassName, student.getClassName());
            }
        } else if (!StpUtil.hasRole("admin")) {
            // 教师只看自己发起的考勤
            wrapper.eq(EduAttendanceSession::getTeacherId, currentUserId);
        }
        if (courseId != null) {
            wrapper.eq(EduAttendanceSession::getCourseId, courseId);
        }
        wrapper.orderByDesc(EduAttendanceSession::getId);

        Page<EduAttendanceSession> page = sessionService.page(new Page<>(pageNum, pageSize), wrapper);

        List<SessionListVO> voList = page.getRecords().stream().map(session -> {
            SessionListVO vo = new SessionListVO();
            vo.setId(session.getId());
            vo.setCourseId(session.getCourseId());
            vo.setClassName(session.getClassName());
            vo.setSessionCode(session.getSessionCode());
            vo.setStartTime(session.getStartTime());
            vo.setEndTime(session.getEndTime());
            vo.setStatus(session.getStatus());
            vo.setDurationMinutes(session.getDurationMinutes());

            // 课程名称
            EduCourse course = courseService.getById(session.getCourseId());
            vo.setCourseName(course != null ? course.getCourseName() : "");

            // 签到统计（从选课表统计班级人数，而非 sys_user.class_name）
            long signed = recordService.count(
                    new LambdaQueryWrapper<EduAttendanceRecord>()
                            .eq(EduAttendanceRecord::getSessionId, session.getId())
                            .eq(EduAttendanceRecord::getStatus, 0));
            long total = studentCourseService.count(
                    new LambdaQueryWrapper<EduStudentCourse>()
                            .eq(EduStudentCourse::getCourseId, session.getCourseId())
                            .eq(EduStudentCourse::getClassName, session.getClassName())
                            .eq(EduStudentCourse::getStatus, 0));
            vo.setSignedCount(signed);
            vo.setTotalCount(total);

            return vo;
        }).collect(Collectors.toList());

        return Result.success(new PageResult<>(page.getTotal(), voList, (long) pageNum, (long) pageSize));
    }

    @PostMapping("/sign")
    @Operation(summary = "学生签到")
    public Result<Void> sign(@Parameter(description = "签到码") @RequestParam String sessionCode) {
        Long studentId = StpUtil.getLoginIdAsLong();

        String sessionIdStr = redisTemplate.opsForValue().get(ATTENDANCE_KEY_PREFIX + sessionCode);
        if (StrUtil.isBlank(sessionIdStr)) {
            throw new BusinessException("签到码无效或签到已结束");
        }
        Long sessionId = Long.parseLong(sessionIdStr);

        EduAttendanceSession session = sessionService.getById(sessionId);
        if (session != null) {
            // 校验学生是否选了该课程且班级匹配
            EduStudentCourse sc = studentCourseService.getOne(
                    new LambdaQueryWrapper<EduStudentCourse>()
                            .eq(EduStudentCourse::getStudentId, studentId)
                            .eq(EduStudentCourse::getCourseId, session.getCourseId())
                            .eq(EduStudentCourse::getStatus, 0)
                            .last("LIMIT 1"),
                    false
            );
            if (sc == null || sc.getClassName() == null) {
                throw new BusinessException("您未选该课程，无法签到");
            }
            if (!sc.getClassName().equals(session.getClassName())) {
                throw new BusinessException("您不属于该考勤班级，无法签到");
            }
        }

        long existCount = recordService.count(
                new LambdaQueryWrapper<EduAttendanceRecord>()
                        .eq(EduAttendanceRecord::getSessionId, sessionId)
                        .eq(EduAttendanceRecord::getStudentId, studentId)
        );
        if (existCount > 0) {
            throw new BusinessException("您已签到，请勿重复签到");
        }

        EduAttendanceRecord record = new EduAttendanceRecord();
        record.setSessionId(sessionId);
        record.setStudentId(studentId);
        record.setSignTime(LocalDateTime.now());
        record.setStatus(0);
        recordService.save(record);

        return Result.success();
    }

    @GetMapping("/my")
    @Operation(summary = "我的考勤记录", description = "学生查看自己的签到记录")
    public Result<List<MyAttendanceVO>> myRecords(
            @Parameter(description = "学期") @RequestParam(required = false) String semester) {

        Long studentId = StpUtil.getLoginIdAsLong();

        // 查学生的签到记录
        LambdaQueryWrapper<EduAttendanceRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EduAttendanceRecord::getStudentId, studentId);
        wrapper.orderByDesc(EduAttendanceRecord::getId);
        List<EduAttendanceRecord> records = recordService.list(wrapper);

        List<MyAttendanceVO> voList = records.stream().map(r -> {
            EduAttendanceSession session = sessionService.getById(r.getSessionId());
            MyAttendanceVO vo = new MyAttendanceVO();
            vo.setRecordId(r.getId());
            vo.setSessionId(r.getSessionId());
            vo.setSignTime(r.getSignTime());
            vo.setRecordStatus(r.getStatus());

            if (session != null) {
                vo.setSessionCode(session.getSessionCode());
                vo.setStartTime(session.getStartTime());
                vo.setEndTime(session.getEndTime());
                vo.setSessionStatus(session.getStatus());
                vo.setClassName(session.getClassName());

                EduCourse course = courseService.getById(session.getCourseId());
                vo.setCourseName(course != null ? course.getCourseName() : "");
            }
            return vo;
        }).collect(Collectors.toList());

        // 按学期过滤（通过课表的学期判断）
        if (StrUtil.isNotBlank(semester)) {
            voList = voList.stream()
                    .filter(vo -> {
                        if (vo.getSessionId() == null) return false;
                        EduAttendanceSession s = sessionService.getById(vo.getSessionId());
                        if (s == null) return false;
                        // 查该课程+班级的课表学期
                        List<EduTimetable> tts = timetableService.list(
                                new LambdaQueryWrapper<EduTimetable>()
                                        .eq(EduTimetable::getCourseId, s.getCourseId())
                                        .eq(EduTimetable::getClassName, s.getClassName())
                                        .eq(EduTimetable::getSemester, semester)
                        );
                        return !tts.isEmpty();
                    })
                    .collect(Collectors.toList());
        }

        return Result.success(voList);
    }

    @Data
    @Schema(name = "发起签到请求", description = "教师发起签到场次时提交的参数")
    public static class CreateSessionDTO {

        @Schema(description = "课程ID")
        private Long courseId;

        @Schema(description = "班级名称")
        private String className;

        @Schema(description = "签到有效时长，单位分钟")
        private Integer durationMinutes;
    }

    @Data
    @Schema(name = "签到统计", description = "签到场次及签到明细统计结果")
    public static class AttendanceStatsVO {

        @Schema(description = "考勤场次")
        private EduAttendanceSession session;

        @Schema(description = "班级总人数")
        private Long totalCount;

        @Schema(description = "已签到人数")
        private Long signedCount;

        @Schema(description = "未签到人数")
        private Long unsignedCount;

        @Schema(description = "已签到学生列表")
        private List<StudentSignVO> signedList;

        @Schema(description = "未签到学生列表")
        private List<StudentSignVO> unsignedList;
    }

    @Data
    @Schema(name = "学生签到信息", description = "单个学生的签到状态")
    public static class StudentSignVO {

        @Schema(description = "学生用户ID")
        private Long studentId;

        @Schema(description = "学号")
        private String username;

        @Schema(description = "姓名")
        private String realName;

        @Schema(description = "签到时间")
        private LocalDateTime signTime;

        @Schema(description = "状态，0-已签到，1-缺勤")
        private Integer status;
    }

    @Data
    @Schema(name = "考勤场次列表项", description = "分页查询考勤场次时的单条记录")
    public static class SessionListVO {

        @Schema(description = "场次ID")
        private Long id;

        @Schema(description = "课程ID")
        private Long courseId;

        @Schema(description = "课程名称")
        private String courseName;

        @Schema(description = "考勤班级")
        private String className;

        @Schema(description = "签到码")
        private String sessionCode;

        @Schema(description = "签到有效时长")
        private Integer durationMinutes;

        @Schema(description = "开始时间")
        private LocalDateTime startTime;

        @Schema(description = "结束时间")
        private LocalDateTime endTime;

        @Schema(description = "状态，0-进行中，1-已结束")
        private Integer status;

        @Schema(description = "已签到人数")
        private Long signedCount;

        @Schema(description = "班级总人数")
        private Long totalCount;
    }

    @Data
    @Schema(name = "我的考勤记录", description = "学生个人的签到记录")
    public static class MyAttendanceVO {

        @Schema(description = "记录ID")
        private Long recordId;

        @Schema(description = "场次ID")
        private Long sessionId;

        @Schema(description = "课程名称")
        private String courseName;

        @Schema(description = "考勤班级")
        private String className;

        @Schema(description = "签到码")
        private String sessionCode;

        @Schema(description = "签到时间")
        private LocalDateTime signTime;

        @Schema(description = "场次开始时间")
        private LocalDateTime startTime;

        @Schema(description = "场次结束时间")
        private LocalDateTime endTime;

        @Schema(description = "签到状态，0-已签到，1-缺勤，2-请假，3-补签")
        private Integer recordStatus;

        @Schema(description = "场次状态，0-进行中，1-已结束")
        private Integer sessionStatus;
    }
}
