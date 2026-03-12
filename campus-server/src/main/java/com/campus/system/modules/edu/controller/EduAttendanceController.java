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
import com.campus.system.modules.edu.service.IEduAttendanceRecordService;
import com.campus.system.modules.edu.service.IEduAttendanceSessionService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 考勤签到控制器
 * 核心：教师创建签到场次（Cache-Aside写入Redis）→ 学生快速签到（命中缓存校验）→ 异步持久化
 */
@RestController
@RequestMapping("/edu/attendance")
@RequiredArgsConstructor
public class EduAttendanceController {

    private final IEduAttendanceSessionService sessionService;
    private final IEduAttendanceRecordService recordService;
    private final StringRedisTemplate redisTemplate;

    private static final String ATTENDANCE_KEY_PREFIX = "attendance:session:";

    // ============ 教师端：考勤场次管理 ============

    /**
     * 教师发起签到场次
     */
    @PostMapping("/session/create")
    @SaCheckPermission("edu:attendance:create")
    @LogRecord(module = "考勤管理", type = "发起签到")
    public Result<EduAttendanceSession> createSession(@RequestBody CreateSessionDTO dto) {
        Long teacherId = StpUtil.getLoginIdAsLong();

        // 生成唯一签到码（6位数字）
        String sessionCode = RandomUtil.randomNumbers(6);

        EduAttendanceSession session = new EduAttendanceSession();
        session.setCourseId(dto.getCourseId());
        session.setTeacherId(teacherId);
        session.setClassName(dto.getClassName());
        session.setSessionCode(sessionCode);
        session.setDurationMinutes(dto.getDurationMinutes() != null ? dto.getDurationMinutes() : 30);
        session.setStartTime(LocalDateTime.now());
        session.setEndTime(LocalDateTime.now().plusMinutes(session.getDurationMinutes()));
        session.setStatus(0); // 进行中
        sessionService.save(session);

        // Cache-Aside: 将场次写入 Redis，设置过期时间
        redisTemplate.opsForValue().set(
                ATTENDANCE_KEY_PREFIX + sessionCode,
                session.getId().toString(),
                session.getDurationMinutes(),
                TimeUnit.MINUTES
        );

        return Result.success(session);
    }

    /**
     * 教师端：查看某场次签到统计
     */
    @GetMapping("/session/{sessionId}/stats")
    @SaCheckPermission("edu:attendance:list")
    public Result<AttendanceStatsVO> sessionStats(@PathVariable Long sessionId) {
        EduAttendanceSession session = sessionService.getById(sessionId);
        if (session == null) throw new BusinessException("场次不存在");

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

    /**
     * 教师端：结束签到场次
     */
    @PutMapping("/session/{sessionId}/finish")
    @SaCheckPermission("edu:attendance:create")
    public Result<Void> finishSession(@PathVariable Long sessionId) {
        EduAttendanceSession session = sessionService.getById(sessionId);
        if (session == null) throw new BusinessException("场次不存在");
        session.setStatus(1);
        session.setEndTime(LocalDateTime.now());
        sessionService.updateById(session);
        // 清除缓存
        redisTemplate.delete(ATTENDANCE_KEY_PREFIX + session.getSessionCode());
        return Result.success();
    }

    /**
     * 分页查询考勤场次
     */
    @GetMapping("/session/page")
    @SaCheckPermission("edu:attendance:list")
    public Result<PageResult<EduAttendanceSession>> sessionPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long courseId) {
        LambdaQueryWrapper<EduAttendanceSession> wrapper = new LambdaQueryWrapper<>();
        if (courseId != null) wrapper.eq(EduAttendanceSession::getCourseId, courseId);
        wrapper.orderByDesc(EduAttendanceSession::getId);
        Page<EduAttendanceSession> page = sessionService.page(new Page<>(pageNum, pageSize), wrapper);
        return Result.success(new PageResult<>(page.getTotal(), page.getRecords(), (long) pageNum, (long) pageSize));
    }

    // ============ 学生端：快速签到 ============

    /**
     * 学生签到（极速通道：先查Redis缓存校验场次有效性 → 防重复签到 → 入库）
     */
    @PostMapping("/sign")
    public Result<Void> sign(@RequestParam String sessionCode) {
        Long studentId = StpUtil.getLoginIdAsLong();

        // 1. Cache-Aside 命中：从 Redis 获取场次ID
        String sessionIdStr = redisTemplate.opsForValue().get(ATTENDANCE_KEY_PREFIX + sessionCode);
        if (StrUtil.isBlank(sessionIdStr)) {
            throw new BusinessException("签到码无效或签到已结束");
        }
        Long sessionId = Long.parseLong(sessionIdStr);

        // 2. 防重复签到：检查是否已签
        long existCount = recordService.count(
                new LambdaQueryWrapper<EduAttendanceRecord>()
                        .eq(EduAttendanceRecord::getSessionId, sessionId)
                        .eq(EduAttendanceRecord::getStudentId, studentId)
        );
        if (existCount > 0) throw new BusinessException("您已签到，请勿重复签到");

        // 3. 入库
        EduAttendanceRecord record = new EduAttendanceRecord();
        record.setSessionId(sessionId);
        record.setStudentId(studentId);
        record.setSignTime(LocalDateTime.now());
        record.setStatus(0); // 已签到
        recordService.save(record);

        return Result.success();
    }

    // ============ 内部 DTO/VO ============

    @Data
    public static class CreateSessionDTO {
        private Long courseId;
        private String className;
        private Integer durationMinutes;
    }

    @Data
    public static class AttendanceStatsVO {
        private EduAttendanceSession session;
        private Long signedCount;
        private List<EduAttendanceRecord> records;
    }
}
