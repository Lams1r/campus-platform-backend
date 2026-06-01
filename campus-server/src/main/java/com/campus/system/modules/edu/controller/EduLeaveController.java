package com.campus.system.modules.edu.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.system.annotation.LogRecord;
import com.campus.system.common.api.PageResult;
import com.campus.system.common.api.Result;
import com.campus.system.common.exception.BusinessException;
import com.campus.system.modules.edu.entity.EduCourse;
import com.campus.system.modules.edu.entity.EduLeaveRequest;
import com.campus.system.modules.edu.entity.EduStudentCourse;
import com.campus.system.modules.edu.entity.EduTimetable;
import com.campus.system.modules.edu.service.IEduCourseService;
import com.campus.system.modules.edu.service.IEduLeaveRequestService;
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
import java.util.stream.Collectors;

/**
 * 请假管理控制器。
 */
@RestController
@RequestMapping("/edu/leave")
@RequiredArgsConstructor
@Tag(name = "请假管理", description = "学生请假与审批接口")
public class EduLeaveController {

    private final IEduLeaveRequestService leaveService;
    private final IEduStudentCourseService studentCourseService;
    private final IEduTimetableService timetableService;
    private final IEduCourseService courseService;
    private final ISysUserService userService;

    @PostMapping("/submit")
    @Operation(summary = "提交请假申请", description = "学生选择已选课程提交请假，自动关联到该课程的教师")
    public Result<Void> submit(@RequestBody LeaveSubmitDTO dto) {
        Long studentId = StpUtil.getLoginIdAsLong();

        // 校验学生是否选了该课程
        EduStudentCourse sc = studentCourseService.getOne(
                new LambdaQueryWrapper<EduStudentCourse>()
                        .eq(EduStudentCourse::getStudentId, studentId)
                        .eq(EduStudentCourse::getCourseId, dto.getCourseId())
                        .eq(EduStudentCourse::getStatus, 0)
                        .last("LIMIT 1"),
                false
        );
        if (sc == null) throw new BusinessException("您未选该课程，无法请假");

        // 查找该课程的教师
        Long teacherId = null;
        List<EduTimetable> timetables = timetableService.list(
                new LambdaQueryWrapper<EduTimetable>()
                        .eq(EduTimetable::getCourseId, dto.getCourseId())
                        .eq(sc.getClassName() != null, EduTimetable::getClassName, sc.getClassName())
        );
        if (!timetables.isEmpty()) {
            teacherId = timetables.get(0).getTeacherId();
        }

        EduLeaveRequest leave = new EduLeaveRequest();
        leave.setStudentId(studentId);
        leave.setCourseId(dto.getCourseId());
        leave.setTeacherId(teacherId);
        leave.setLeaveType(dto.getLeaveType());
        leave.setReason(dto.getReason());
        leave.setStartTime(dto.getStartTime());
        leave.setEndTime(dto.getEndTime());
        leave.setStatus(0);
        leaveService.save(leave);

        return Result.success();
    }

    @GetMapping("/my")
    @Operation(summary = "我的请假记录")
    public Result<List<LeaveVO>> myLeaves(
            @Parameter(description = "学期") @RequestParam(required = false) String semester) {

        Long studentId = StpUtil.getLoginIdAsLong();

        List<EduLeaveRequest> list = leaveService.list(
                new LambdaQueryWrapper<EduLeaveRequest>()
                        .eq(EduLeaveRequest::getStudentId, studentId)
                        .orderByDesc(EduLeaveRequest::getId)
        );

        List<LeaveVO> voList = list.stream().map(this::toVO).collect(Collectors.toList());
        return Result.success(voList);
    }

    @GetMapping("/page")
    @SaCheckPermission("edu:leave:list")
    @Operation(summary = "教师查看待审批的请假列表", description = "教师只看到自己课程的请假申请")
    public Result<PageResult<LeaveVO>> page(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "审批状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "课程ID") @RequestParam(required = false) Long courseId) {

        Long teacherId = StpUtil.getLoginIdAsLong();
        boolean isTeacher = StpUtil.hasRole("teacher");

        LambdaQueryWrapper<EduLeaveRequest> wrapper = new LambdaQueryWrapper<>();

        if (isTeacher) {
            // 教师只看自己课程的请假
            wrapper.eq(EduLeaveRequest::getTeacherId, teacherId);
        }

        if (status != null) wrapper.eq(EduLeaveRequest::getStatus, status);
        if (courseId != null) wrapper.eq(EduLeaveRequest::getCourseId, courseId);
        wrapper.orderByDesc(EduLeaveRequest::getId);

        Page<EduLeaveRequest> page = leaveService.page(new Page<>(pageNum, pageSize), wrapper);

        List<LeaveVO> voList = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return Result.success(new PageResult<>(page.getTotal(), voList, (long) pageNum, (long) pageSize));
    }

    @PutMapping("/{id}/approve")
    @SaCheckPermission("edu:leave:approve")
    @LogRecord(module = "请假管理", type = "审批")
    @Operation(summary = "审批请假申请")
    public Result<Void> approve(
            @PathVariable Long id,
            @RequestParam Integer status,
            @RequestParam(required = false) String remark) {

        if (status != 1 && status != 2) {
            throw new BusinessException("审批状态只能为 1(通过) 或 2(驳回)");
        }

        EduLeaveRequest leave = leaveService.getById(id);
        if (leave == null) throw new BusinessException("请假记录不存在");
        if (leave.getStatus() != 0) throw new BusinessException("该请假单已处理，不可重复审批");

        leave.setStatus(status);
        leave.setApproverId(StpUtil.getLoginIdAsLong());
        leave.setApproveTime(LocalDateTime.now());
        leave.setApproveRemark(remark);
        leaveService.updateById(leave);

        return Result.success();
    }

    private LeaveVO toVO(EduLeaveRequest leave) {
        LeaveVO vo = new LeaveVO();
        vo.setId(leave.getId());
        vo.setStudentId(leave.getStudentId());
        vo.setCourseId(leave.getCourseId());
        vo.setTeacherId(leave.getTeacherId());
        vo.setLeaveType(leave.getLeaveType());
        vo.setReason(leave.getReason());
        vo.setStartTime(leave.getStartTime());
        vo.setEndTime(leave.getEndTime());
        vo.setStatus(leave.getStatus());
        vo.setApproveRemark(leave.getApproveRemark());
        vo.setCreateTime(leave.getCreateTime());

        // 学生信息
        SysUser student = userService.getById(leave.getStudentId());
        vo.setStudentName(student != null ? student.getRealName() : "");
        vo.setStudentNo(student != null ? student.getUsername() : "");

        // 课程信息
        EduCourse course = courseService.getById(leave.getCourseId());
        vo.setCourseName(course != null ? course.getCourseName() : "");

        return vo;
    }

    @Data
    @Schema(name = "请假提交请求")
    public static class LeaveSubmitDTO {
        @Schema(description = "课程ID")
        private Long courseId;
        @Schema(description = "请假类型，0-事假，1-病假，2-其他")
        private Integer leaveType;
        @Schema(description = "请假原因")
        private String reason;
        @Schema(description = "开始时间")
        private LocalDateTime startTime;
        @Schema(description = "结束时间")
        private LocalDateTime endTime;
    }

    @Data
    @Schema(name = "请假记录")
    public static class LeaveVO {
        private Long id;
        private Long studentId;
        private String studentNo;
        private String studentName;
        private Long courseId;
        private String courseName;
        private Long teacherId;
        private Integer leaveType;
        private String reason;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Integer status;
        private String approveRemark;
        private LocalDateTime createTime;
    }
}
