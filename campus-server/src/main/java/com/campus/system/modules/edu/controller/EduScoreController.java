package com.campus.system.modules.edu.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.system.annotation.LogRecord;
import com.campus.system.common.api.PageResult;
import com.campus.system.common.api.Result;
import com.campus.system.common.exception.BusinessException;
import com.campus.system.modules.edu.entity.EduCourse;
import com.campus.system.modules.edu.entity.EduScore;
import com.campus.system.modules.edu.entity.EduScoreAppeal;
import com.campus.system.modules.edu.entity.EduStudentCourse;
import com.campus.system.modules.edu.service.IEduCourseService;
import com.campus.system.modules.edu.service.IEduScoreAppealService;
import com.campus.system.modules.edu.service.IEduScoreService;
import com.campus.system.modules.edu.service.IEduStudentCourseService;
import com.campus.system.modules.sys.entity.SysUser;
import com.campus.system.modules.sys.service.ISysUserService;
import com.campus.system.util.SecurityUtils;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 成绩管理控制器。
 */
@RestController
@RequestMapping("/edu/score")
@RequiredArgsConstructor
@Tag(name = "成绩管理", description = "成绩录入、审核与申诉处理接口")
public class EduScoreController {

    private final IEduScoreService scoreService;
    private final IEduScoreAppealService appealService;
    private final IEduCourseService courseService;
    private final IEduStudentCourseService studentCourseService;
    private final ISysUserService userService;

    @GetMapping("/page")
    @SaCheckPermission("edu:score:list")
    @Operation(summary = "分页查询成绩")
    public Result<PageResult<ScorePageVO>> page(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") Integer pageSize,
            @Parameter(description = "课程ID") @RequestParam(required = false) Long courseId,
            @Parameter(description = "学生ID") @RequestParam(required = false) Long studentId,
            @Parameter(description = "学期") @RequestParam(required = false) String semester,
            @Parameter(description = "审核状态") @RequestParam(required = false) Integer status) {

        LambdaQueryWrapper<EduScore> wrapper = new LambdaQueryWrapper<>();
        if (courseId != null) wrapper.eq(EduScore::getCourseId, courseId);
        if (SecurityUtils.hasRole("student")) {
            wrapper.eq(EduScore::getStudentId, SecurityUtils.getCurrentUserId());
        } else if (studentId != null) {
            wrapper.eq(EduScore::getStudentId, studentId);
        }
        if (StrUtil.isNotBlank(semester)) wrapper.eq(EduScore::getSemester, semester);
        if (status != null) wrapper.eq(EduScore::getStatus, status);
        wrapper.orderByDesc(EduScore::getId);

        Page<EduScore> page = scoreService.page(new Page<>(pageNum, pageSize), wrapper);

        List<ScorePageVO> voList = page.getRecords().stream().map(s -> {
            ScorePageVO vo = new ScorePageVO();
            vo.setId(s.getId());
            vo.setCourseId(s.getCourseId());
            vo.setStudentId(s.getStudentId());
            vo.setRegularScore(s.getRegularScore());
            vo.setExamScore(s.getExamScore());
            vo.setTotalScore(s.getTotalScore());
            vo.setGradeLevel(s.getGradeLevel());
            vo.setSemester(s.getSemester());
            vo.setStatus(s.getStatus());
            vo.setCreateTime(s.getCreateTime());
            EduCourse course = courseService.getById(s.getCourseId());
            vo.setCourseName(course != null ? course.getCourseName() : "");
            vo.setCourseCode(course != null ? course.getCourseCode() : "");
            return vo;
        }).collect(Collectors.toList());

        return Result.success(new PageResult<>(page.getTotal(), voList, (long) pageNum, (long) pageSize));
    }

    @GetMapping("/course/{courseId}/students")
    @SaCheckPermission("edu:score:list")
    @Operation(summary = "获取课程的学生成绩列表", description = "教师查看选了该课程的所有学生及其成绩")
    public Result<List<StudentScoreVO>> courseStudents(
            @PathVariable Long courseId,
            @RequestParam(required = false) String semester) {

        // 查选了该课的学生
        LambdaQueryWrapper<EduStudentCourse> wrapper = new LambdaQueryWrapper<EduStudentCourse>()
                .eq(EduStudentCourse::getCourseId, courseId)
                .eq(EduStudentCourse::getStatus, 0);
        if (StrUtil.isNotBlank(semester)) {
            wrapper.eq(EduStudentCourse::getSemester, semester);
        }
        List<EduStudentCourse> selections = studentCourseService.list(wrapper);

        EduCourse course = courseService.getById(courseId);
        int regularRatio = course != null && course.getRegularRatio() != null ? course.getRegularRatio() : 30;
        int examRatio = course != null && course.getExamRatio() != null ? course.getExamRatio() : 70;

        List<StudentScoreVO> voList = selections.stream().map(sel -> {
            SysUser student = userService.getById(sel.getStudentId());

            EduScore score = scoreService.getOne(
                    new LambdaQueryWrapper<EduScore>()
                            .eq(EduScore::getCourseId, courseId)
                            .eq(EduScore::getStudentId, sel.getStudentId())
                            .eq(EduScore::getSemester, sel.getSemester())
                            .last("LIMIT 1"),
                    false
            );

            StudentScoreVO vo = new StudentScoreVO();
            vo.setStudentId(sel.getStudentId());
            vo.setStudentNo(student != null ? student.getUsername() : "");
            vo.setStudentName(student != null ? student.getRealName() : "");
            vo.setClassName(sel.getClassName());
            vo.setRegularRatio(regularRatio);
            vo.setExamRatio(examRatio);

            if (score != null) {
                vo.setScoreId(score.getId());
                vo.setRegularScore(score.getRegularScore());
                vo.setExamScore(score.getExamScore());
                vo.setTotalScore(score.getTotalScore());
                vo.setGradeLevel(score.getGradeLevel());
                vo.setStatus(score.getStatus());
            }
            return vo;
        }).collect(Collectors.toList());

        return Result.success(voList);
    }

    @PostMapping
    @SaCheckPermission("edu:score:add")
    @LogRecord(module = "成绩管理", type = "录入")
    @Operation(summary = "录入/更新成绩", description = "教师填写学生的平时成绩和考试成绩，自动计算总成绩和等级")
    public Result<Void> saveScore(@RequestBody ScoreSubmitDTO dto) {
        Long teacherId = StpUtil.getLoginIdAsLong();

        EduCourse course = courseService.getById(dto.getCourseId());
        if (course == null) throw new BusinessException("课程不存在");

        int regularRatio = course.getRegularRatio() != null ? course.getRegularRatio() : 30;
        int examRatio = course.getExamRatio() != null ? course.getExamRatio() : 70;

        // 查是否已有成绩记录
        EduScore existing = scoreService.getOne(
                new LambdaQueryWrapper<EduScore>()
                        .eq(EduScore::getCourseId, dto.getCourseId())
                        .eq(EduScore::getStudentId, dto.getStudentId())
                        .eq(EduScore::getSemester, dto.getSemester())
                        .last("LIMIT 1"),
                false
        );

        if (existing != null) {
            if (existing.getStatus() == 2) {
                throw new BusinessException("该成绩已归档，禁止修改");
            }
            existing.setRegularScore(dto.getRegularScore());
            existing.setExamScore(dto.getExamScore());
            existing.setTotalScore(calcTotal(dto.getRegularScore(), dto.getExamScore(), regularRatio, examRatio));
            existing.setGradeLevel(calcGrade(existing.getTotalScore()));
            existing.setTeacherId(teacherId);
            existing.setStatus(0);
            scoreService.updateById(existing);
        } else {
            EduScore score = new EduScore();
            score.setCourseId(dto.getCourseId());
            score.setStudentId(dto.getStudentId());
            score.setSemester(dto.getSemester());
            score.setRegularScore(dto.getRegularScore());
            score.setExamScore(dto.getExamScore());
            score.setTotalScore(calcTotal(dto.getRegularScore(), dto.getExamScore(), regularRatio, examRatio));
            score.setGradeLevel(calcGrade(score.getTotalScore()));
            score.setTeacherId(teacherId);
            score.setStatus(0);
            scoreService.save(score);
        }

        return Result.success();
    }

    @PutMapping("/{id}/audit")
    @SaCheckRole("admin")
    @LogRecord(module = "成绩管理", type = "审核")
    @Operation(summary = "审核成绩")
    public Result<Void> audit(
            @PathVariable Long id,
            @RequestParam Integer status,
            @RequestParam(required = false) String remark) {

        if (status != 1 && status != 2) {
            throw new BusinessException("审核状态只能为 1(驳回) 或 2(归档)");
        }
        EduScore score = scoreService.getById(id);
        if (score == null) throw new BusinessException("成绩记录不存在");

        score.setStatus(status);
        score.setAuditUserId(StpUtil.getLoginIdAsLong());
        score.setAuditTime(LocalDateTime.now());
        score.setAuditRemark(remark);
        scoreService.updateById(score);
        return Result.success();
    }

    @PostMapping("/appeal")
    @Operation(summary = "提交成绩申诉")
    public Result<Void> submitAppeal(@RequestBody EduScoreAppeal appeal) {
        Long studentId = StpUtil.getLoginIdAsLong();
        appeal.setStudentId(studentId);
        appeal.setStatus(0);
        if (StrUtil.isBlank(appeal.getAttachmentPath())) {
            throw new BusinessException("申诉必须上传佐证图片凭证");
        }
        long count = appealService.count(
                new LambdaQueryWrapper<EduScoreAppeal>()
                        .eq(EduScoreAppeal::getScoreId, appeal.getScoreId())
                        .eq(EduScoreAppeal::getStudentId, studentId)
                        .ne(EduScoreAppeal::getStatus, 2));
        if (count > 0) throw new BusinessException("该成绩已有待处理的申诉工单");

        appealService.save(appeal);
        return Result.success();
    }

    @GetMapping("/appeal/page")
    @SaCheckPermission("edu:score:list")
    @Operation(summary = "分页查询成绩申诉")
    public Result<PageResult<EduScoreAppeal>> appealPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status) {

        LambdaQueryWrapper<EduScoreAppeal> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(EduScoreAppeal::getStatus, status);
        wrapper.orderByDesc(EduScoreAppeal::getId);
        Page<EduScoreAppeal> page = appealService.page(new Page<>(pageNum, pageSize), wrapper);
        return Result.success(new PageResult<>(page.getTotal(), page.getRecords(), (long) pageNum, (long) pageSize));
    }

    @PutMapping("/appeal/{id}/handle")
    @SaCheckPermission("edu:score:edit")
    @LogRecord(module = "成绩管理", type = "处理申诉")
    @Operation(summary = "处理成绩申诉")
    public Result<Void> handleAppeal(
            @PathVariable Long id,
            @RequestParam Integer status,
            @RequestParam(required = false) String result) {

        if (status != 1 && status != 2) throw new BusinessException("处理状态只能为 1(受理) 或 2(驳回)");
        EduScoreAppeal appeal = appealService.getById(id);
        if (appeal == null || appeal.getStatus() != 0) throw new BusinessException("申诉记录不存在或已处理");

        appeal.setStatus(status);
        appeal.setHandlerId(StpUtil.getLoginIdAsLong());
        appeal.setHandleTime(LocalDateTime.now());
        appeal.setHandleResult(result);
        appealService.updateById(appeal);

        if (status == 1) {
            EduScore score = scoreService.getById(appeal.getScoreId());
            if (score != null && score.getStatus() == 2) {
                score.setStatus(1);
                score.setAuditRemark("因申诉受理，成绩已解锁");
                scoreService.updateById(score);
            }
        }
        return Result.success();
    }

    private BigDecimal calcTotal(BigDecimal regular, BigDecimal exam, int regularRatio, int examRatio) {
        if (regular == null || exam == null) return null;
        BigDecimal r = regular.multiply(BigDecimal.valueOf(regularRatio)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal e = exam.multiply(BigDecimal.valueOf(examRatio)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return r.add(e).setScale(2, RoundingMode.HALF_UP);
    }

    private String calcGrade(BigDecimal total) {
        if (total == null) return null;
        int score = total.intValue();
        if (score < 60) return "不及格";
        if (score <= 89) return "及格";
        return "优秀";
    }

    @Data
    @Schema(name = "成绩提交请求")
    public static class ScoreSubmitDTO {
        @Schema(description = "课程ID")
        private Long courseId;
        @Schema(description = "学生ID")
        private Long studentId;
        @Schema(description = "学期")
        private String semester;
        @Schema(description = "平时成绩")
        private BigDecimal regularScore;
        @Schema(description = "考试成绩")
        private BigDecimal examScore;
    }

    @Data
    @Schema(name = "学生成绩信息")
    public static class StudentScoreVO {
        private Long scoreId;
        private Long studentId;
        private String studentNo;
        private String studentName;
        private String className;
        private BigDecimal regularScore;
        private BigDecimal examScore;
        private BigDecimal totalScore;
        private String gradeLevel;
        private Integer regularRatio;
        private Integer examRatio;
        private Integer status;
    }

    @Data
    @Schema(name = "成绩分页项")
    public static class ScorePageVO {
        private Long id;
        private Long courseId;
        private String courseName;
        private String courseCode;
        private Long studentId;
        private BigDecimal regularScore;
        private BigDecimal examScore;
        private BigDecimal totalScore;
        private String gradeLevel;
        private String semester;
        private Integer status;
        private java.time.LocalDateTime createTime;
    }
}
