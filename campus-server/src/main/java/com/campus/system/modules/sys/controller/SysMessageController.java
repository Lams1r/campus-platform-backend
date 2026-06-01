package com.campus.system.modules.sys.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.system.common.api.Result;
import com.campus.system.common.exception.BusinessException;
import com.campus.system.modules.edu.entity.EduCourse;
import com.campus.system.modules.edu.entity.EduCourseClass;
import com.campus.system.modules.edu.entity.EduCourseTeacher;
import com.campus.system.modules.edu.entity.EduStudentCourse;
import com.campus.system.modules.edu.entity.EduTimetable;
import com.campus.system.modules.edu.mapper.EduCourseClassMapper;
import com.campus.system.modules.edu.mapper.EduCourseTeacherMapper;
import com.campus.system.modules.edu.service.IEduCourseService;
import com.campus.system.modules.edu.service.IEduStudentCourseService;
import com.campus.system.modules.edu.service.IEduTimetableService;
import com.campus.system.modules.sys.entity.SysMessage;
import com.campus.system.modules.sys.entity.SysUser;
import com.campus.system.modules.sys.service.ISysMessageService;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/sys/message")
@RequiredArgsConstructor
@Tag(name = "消息通知", description = "站内消息收发接口")
public class SysMessageController {

    private final ISysMessageService messageService;
    private final ISysUserService userService;
    private final IEduStudentCourseService studentCourseService;
    private final IEduTimetableService timetableService;
    private final IEduCourseService courseService;
    private final EduCourseTeacherMapper courseTeacherMapper;
    private final EduCourseClassMapper courseClassMapper;

    @PostMapping("/send")
    @Operation(summary = "发送消息")
    public Result<Void> send(@RequestBody SendMessageDTO dto) {
        Long senderId = StpUtil.getLoginIdAsLong();

        if (dto.getReceiverIds() == null || dto.getReceiverIds().isEmpty()) {
            throw new BusinessException("请选择接收人");
        }

        for (Long receiverId : dto.getReceiverIds()) {
            SysMessage msg = new SysMessage();
            msg.setSenderId(senderId);
            msg.setReceiverId(receiverId);
            msg.setTitle(dto.getTitle());
            msg.setContent(dto.getContent());
            msg.setMsgType(dto.getMsgType() != null ? dto.getMsgType() : 0);
            msg.setIsRead(0);
            messageService.save(msg);
        }

        return Result.success();
    }

    @PostMapping("/send-to-course")
    @Operation(summary = "向课程学生发送消息", description = "教师向指定课程+班级的学生发送消息")
    public Result<Void> sendToCourse(@RequestBody SendCourseMessageDTO dto) {
        Long senderId = StpUtil.getLoginIdAsLong();

        // 查选了该课程的学生（可选按班级过滤）
        LambdaQueryWrapper<EduStudentCourse> wrapper = new LambdaQueryWrapper<EduStudentCourse>()
                .eq(EduStudentCourse::getCourseId, dto.getCourseId())
                .eq(EduStudentCourse::getStatus, 0);
        if (dto.getClassNames() != null && !dto.getClassNames().isEmpty()) {
            wrapper.in(EduStudentCourse::getClassName, dto.getClassNames());
        }
        List<EduStudentCourse> selections = studentCourseService.list(wrapper);

        if (selections.isEmpty()) throw new BusinessException("该课程暂无学生");

        for (EduStudentCourse sel : selections) {
            SysMessage msg = new SysMessage();
            msg.setSenderId(senderId);
            msg.setReceiverId(sel.getStudentId());
            msg.setTitle(dto.getTitle());
            msg.setContent(dto.getContent());
            msg.setMsgType(0);
            msg.setIsRead(0);
            messageService.save(msg);
        }

        return Result.success();
    }

    @GetMapping("/inbox")
    @Operation(summary = "我的收件箱")
    public Result<List<MessageVO>> inbox() {
        Long userId = StpUtil.getLoginIdAsLong();

        List<SysMessage> messages = messageService.list(
                new LambdaQueryWrapper<SysMessage>()
                        .eq(SysMessage::getReceiverId, userId)
                        .eq(SysMessage::getIsDeleted, 0)
                        .orderByDesc(SysMessage::getCreateTime)
        );

        return Result.success(messages.stream().map(this::toVO).collect(Collectors.toList()));
    }

    @GetMapping("/sent")
    @Operation(summary = "我的发件箱")
    public Result<List<MessageVO>> sent() {
        Long userId = StpUtil.getLoginIdAsLong();

        List<SysMessage> messages = messageService.list(
                new LambdaQueryWrapper<SysMessage>()
                        .eq(SysMessage::getSenderId, userId)
                        .eq(SysMessage::getIsDeleted, 0)
                        .orderByDesc(SysMessage::getCreateTime)
        );

        return Result.success(messages.stream().map(this::toVO).collect(Collectors.toList()));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "标记已读")
    public Result<Void> markRead(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        messageService.update(new LambdaUpdateWrapper<SysMessage>()
                .eq(SysMessage::getId, id)
                .eq(SysMessage::getReceiverId, userId)
                .set(SysMessage::getIsRead, 1)
                .set(SysMessage::getReadTime, LocalDateTime.now()));
        return Result.success();
    }

    @GetMapping("/unread-count")
    @Operation(summary = "未读消息数")
    public Result<Long> unreadCount() {
        Long userId = StpUtil.getLoginIdAsLong();
        long count = messageService.count(
                new LambdaQueryWrapper<SysMessage>()
                        .eq(SysMessage::getReceiverId, userId)
                        .eq(SysMessage::getIsRead, 0)
                        .eq(SysMessage::getIsDeleted, 0));
        return Result.success(count);
    }

    @GetMapping("/course-students/{courseId}")
    @Operation(summary = "获取课程学生列表", description = "教师发消息时选择课程的学生")
    public Result<List<StudentVO>> courseStudents(@PathVariable Long courseId) {
        List<EduStudentCourse> selections = studentCourseService.list(
                new LambdaQueryWrapper<EduStudentCourse>()
                        .eq(EduStudentCourse::getCourseId, courseId)
                        .eq(EduStudentCourse::getStatus, 0));

        List<StudentVO> voList = selections.stream().map(sel -> {
            SysUser student = userService.getById(sel.getStudentId());
            StudentVO vo = new StudentVO();
            vo.setUserId(sel.getStudentId());
            vo.setUsername(student != null ? student.getUsername() : "");
            vo.setRealName(student != null ? student.getRealName() : "");
            vo.setClassName(sel.getClassName());
            return vo;
        }).collect(Collectors.toList());

        return Result.success(voList);
    }

    @GetMapping("/my-courses")
    @Operation(summary = "教师获取自己的课程列表", description = "教师发消息时选择自己教的课程")
    public Result<List<TeacherCourseVO>> myCourses() {
        Long teacherId = StpUtil.getLoginIdAsLong();

        // 查教师关联的课程
        List<EduCourseTeacher> ctList = courseTeacherMapper.selectList(
                new LambdaQueryWrapper<EduCourseTeacher>().eq(EduCourseTeacher::getTeacherId, teacherId));

        List<TeacherCourseVO> voList = ctList.stream().map(ct -> {
            EduCourse course = courseService.getById(ct.getCourseId());
            // 查该课程绑定的班级
            List<EduCourseClass> classList = courseClassMapper.selectList(
                    new LambdaQueryWrapper<EduCourseClass>().eq(EduCourseClass::getCourseId, ct.getCourseId()));
            List<String> classNames = classList.stream().map(EduCourseClass::getClassName).collect(Collectors.toList());

            TeacherCourseVO vo = new TeacherCourseVO();
            vo.setCourseId(ct.getCourseId());
            vo.setCourseName(course != null ? course.getCourseName() : "");
            vo.setCourseCode(course != null ? course.getCourseCode() : "");
            vo.setSemester(course != null ? course.getSemester() : "");
            vo.setClassNames(classNames);
            return vo;
        }).collect(Collectors.toList());

        return Result.success(voList);
    }

    @GetMapping("/my-teachers")
    @Operation(summary = "获取我的课程教师列表", description = "学生发消息时选择已选课程的教师")
    public Result<List<TeacherVO>> myTeachers() {
        Long studentId = StpUtil.getLoginIdAsLong();

        // 学生已选课程
        List<EduStudentCourse> selections = studentCourseService.list(
                new LambdaQueryWrapper<EduStudentCourse>()
                        .eq(EduStudentCourse::getStudentId, studentId)
                        .eq(EduStudentCourse::getStatus, 0));

        if (selections.isEmpty()) return Result.success(Collections.emptyList());

        // 查每个课程的教师（去重）
        Set<Long> teacherIds = new java.util.LinkedHashSet<>();
        Map<Long, String> courseNameMap = new java.util.HashMap<>();
        for (EduStudentCourse sel : selections) {
            List<EduCourseTeacher> ctList = courseTeacherMapper.selectList(
                    new LambdaQueryWrapper<EduCourseTeacher>().eq(EduCourseTeacher::getCourseId, sel.getCourseId()));
            ctList.forEach(ct -> teacherIds.add(ct.getTeacherId()));
        }

        List<TeacherVO> voList = teacherIds.stream().map(tid -> {
            SysUser teacher = userService.getById(tid);
            TeacherVO vo = new TeacherVO();
            vo.setUserId(tid);
            vo.setUsername(teacher != null ? teacher.getUsername() : "");
            vo.setRealName(teacher != null ? teacher.getRealName() : "");
            return vo;
        }).collect(Collectors.toList());

        return Result.success(voList);
    }

    private MessageVO toVO(SysMessage msg) {
        MessageVO vo = new MessageVO();
        vo.setId(msg.getId());
        vo.setSenderId(msg.getSenderId());
        vo.setReceiverId(msg.getReceiverId());
        vo.setTitle(msg.getTitle());
        vo.setContent(msg.getContent());
        vo.setMsgType(msg.getMsgType());
        vo.setIsRead(msg.getIsRead());
        vo.setCreateTime(msg.getCreateTime());

        SysUser sender = userService.getById(msg.getSenderId());
        vo.setSenderName(sender != null ? sender.getRealName() : "");

        SysUser receiver = userService.getById(msg.getReceiverId());
        vo.setReceiverName(receiver != null ? receiver.getRealName() : "");

        return vo;
    }

    @Data
    public static class SendMessageDTO {
        private List<Long> receiverIds;
        private String title;
        private String content;
        private Integer msgType;
    }

    @Data
    public static class SendCourseMessageDTO {
        private Long courseId;
        private List<String> classNames;
        private String title;
        private String content;
    }

    @Data
    public static class MessageVO {
        private Long id;
        private Long senderId;
        private String senderName;
        private Long receiverId;
        private String receiverName;
        private String title;
        private String content;
        private Integer msgType;
        private Integer isRead;
        private LocalDateTime createTime;
    }

    @Data
    public static class StudentVO {
        private Long userId;
        private String username;
        private String realName;
        private String className;
    }

    @Data
    public static class TeacherVO {
        private Long userId;
        private String username;
        private String realName;
    }

    @Data
    public static class TeacherCourseVO {
        private Long courseId;
        private String courseName;
        private String courseCode;
        private String semester;
        private List<String> classNames;
    }
}
