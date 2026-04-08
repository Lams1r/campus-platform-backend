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
import com.campus.system.modules.edu.entity.EduCourseClass;
import com.campus.system.modules.edu.mapper.EduCourseClassMapper;
import com.campus.system.modules.edu.service.IEduAttendanceRecordService;
import com.campus.system.modules.edu.service.IEduAttendanceSessionService;
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
import java.util.List;
import java.util.concurrent.TimeUnit;

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

    @GetMapping("/session/{sessionId}/stats")
    @SaCheckPermission("edu:attendance:list")
    @Operation(summary = "查看签到统计")
    public Result<AttendanceStatsVO> sessionStats(@Parameter(description = "考勤场次ID") @PathVariable Long sessionId) {
        EduAttendanceSession session = sessionService.getById(sessionId);
        if (session == null) {
            throw new BusinessException("场次不存在");
        }

        long signedCount = recordService.count(
                new LambdaQueryWrapper<EduAttendanceRecord>()
                        .eq(EduAttendanceRecord::getSessionId, sessionId)
                        .eq(EduAttendanceRecord::getStatus, 0)
        );
        List<EduAttendanceRecord> records = recordService.list(
                new LambdaQueryWrapper<EduAttendanceRecord>()
                        .eq(EduAttendanceRecord::getSessionId, sessionId)
                        .orderByAsc(EduAttendanceRecord::getSignTime)
        );

        AttendanceStatsVO vo = new AttendanceStatsVO();
        vo.setSession(session);
        vo.setSignedCount(signedCount);
        vo.setRecords(records);
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
    public Result<PageResult<EduAttendanceSession>> sessionPage(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "课程ID") @RequestParam(required = false) Long courseId) {
        LambdaQueryWrapper<EduAttendanceSession> wrapper = new LambdaQueryWrapper<>();
        if (courseId != null) {
            wrapper.eq(EduAttendanceSession::getCourseId, courseId);
        }
        wrapper.orderByDesc(EduAttendanceSession::getId);
        Page<EduAttendanceSession> page = sessionService.page(new Page<>(pageNum, pageSize), wrapper);
        return Result.success(new PageResult<>(page.getTotal(), page.getRecords(), (long) pageNum, (long) pageSize));
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
            SysUser student = userService.getById(studentId);
            if (student != null && student.getClassName() != null) {
                long classBound = courseClassMapper.selectCount(
                        new LambdaQueryWrapper<EduCourseClass>()
                                .eq(EduCourseClass::getCourseId, session.getCourseId())
                                .eq(EduCourseClass::getClassName, student.getClassName())
                );
                if (classBound == 0) {
                    throw new BusinessException("您不属于该课程的考勤班级，无法签到");
                }
            } else {
                throw new BusinessException("无法获取您的班级信息，无法签到");
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

        @Schema(description = "已签到人数")
        private Long signedCount;

        @Schema(description = "签到记录列表")
        private List<EduAttendanceRecord> records;
    }
}
