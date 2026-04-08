package com.campus.system.config;

import com.alicp.jetcache.anno.support.GlobalCacheConfig;
import com.campus.system.CampusSystemApplication;
import com.campus.system.modules.auth.service.AuthService;
import com.campus.system.modules.edu.mapper.EduCourseClassMapper;
import com.campus.system.modules.edu.mapper.EduCourseTeacherMapper;
import com.campus.system.modules.edu.service.IEduAttendanceRecordService;
import com.campus.system.modules.edu.service.IEduAttendanceSessionService;
import com.campus.system.modules.edu.service.IEduCourseEvaluationService;
import com.campus.system.modules.edu.service.IEduCourseMaterialService;
import com.campus.system.modules.edu.service.IEduCourseService;
import com.campus.system.modules.edu.service.IEduLeaveRequestService;
import com.campus.system.modules.edu.service.IEduScoreAppealService;
import com.campus.system.modules.edu.service.IEduScoreService;
import com.campus.system.modules.edu.service.IEduTimetableService;
import com.campus.system.modules.svc.controller.DashboardController;
import com.campus.system.modules.svc.service.ICampusBookBorrowService;
import com.campus.system.modules.svc.service.ICampusBookService;
import com.campus.system.modules.svc.service.ICampusCardLossService;
import com.campus.system.modules.svc.service.ICampusCardRecordService;
import com.campus.system.modules.svc.service.ICampusDashboardSnapshotService;
import com.campus.system.modules.svc.service.ICampusDormitoryAllocationService;
import com.campus.system.modules.svc.service.ICampusDormitoryBuildingService;
import com.campus.system.modules.svc.service.ICampusDormitoryRoomService;
import com.campus.system.modules.svc.service.ICampusNoticeReadService;
import com.campus.system.modules.svc.service.ICampusNoticeService;
import com.campus.system.modules.svc.service.ICampusRepairOrderService;
import com.campus.system.modules.sys.mapper.SysRoleMenuMapper;
import com.campus.system.modules.sys.service.ISysDictDataService;
import com.campus.system.modules.sys.service.ISysDictTypeService;
import com.campus.system.modules.sys.service.ISysLoginLogService;
import com.campus.system.modules.sys.service.ISysMenuService;
import com.campus.system.modules.sys.service.ISysOperateLogService;
import com.campus.system.modules.sys.service.ISysRoleService;
import com.campus.system.modules.sys.service.ISysUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CampusSystemApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "server.servlet.context-path=",
        "spring.data.redis.repositories.enabled=false",
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,"
                + "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration,"
                + "com.alicp.jetcache.autoconfigure.JetCacheAutoConfiguration"
})
@MockBean(classes = {
        StpInterfaceImpl.class,
        GlobalCacheConfig.class,
        DashboardController.class,
        AuthService.class,
        StringRedisTemplate.class,
        EduCourseClassMapper.class,
        EduCourseTeacherMapper.class,
        SysRoleMenuMapper.class,
        ISysUserService.class,
        ISysRoleService.class,
        ISysOperateLogService.class,
        ISysMenuService.class,
        ISysLoginLogService.class,
        ISysDictTypeService.class,
        ISysDictDataService.class,
        IEduTimetableService.class,
        IEduScoreService.class,
        IEduScoreAppealService.class,
        IEduLeaveRequestService.class,
        IEduCourseService.class,
        IEduCourseMaterialService.class,
        IEduCourseEvaluationService.class,
        IEduAttendanceSessionService.class,
        IEduAttendanceRecordService.class,
        ICampusRepairOrderService.class,
        ICampusNoticeService.class,
        ICampusNoticeReadService.class,
        ICampusDormitoryRoomService.class,
        ICampusDormitoryBuildingService.class,
        ICampusDormitoryAllocationService.class,
        ICampusDashboardSnapshotService.class,
        ICampusCardRecordService.class,
        ICampusCardLossService.class,
        ICampusBookService.class,
        ICampusBookBorrowService.class
})
class OpenApiDocumentationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiDocsExposeChineseMetadataForFrontendUsage() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(content, containsString("一体化智慧校园系统 API"));
        assertThat(content, containsString("认证管理"));
        assertThat(content, containsString("用户登录"));
        assertThat(content, containsString("登录结果"));
        assertThat(content, containsString("验证码图片"));
        assertThat(content, containsString("用户名"));
        assertThat(content, containsString("用户管理"));
        assertThat(content, containsString("分页查询用户列表"));
        assertThat(content, containsString("新增用户"));
        assertThat(content, containsString("重置用户密码"));
        assertThat(content, containsString("关键字"));
        assertThat(content, containsString("角色详情"));
        assertThat(content, containsString("菜单树节点"));
        assertThat(content, containsString("图书管理"));
        assertThat(content, containsString("分页查询图书列表"));
        assertThat(content, containsString("书名"));
        assertThat(content, containsString("新增图书"));
        assertThat(content, containsString("查询我的借阅记录"));
        assertThat(content, containsString("分页查询借阅记录"));
        assertThat(content, containsString("查询我的校园卡流水"));
        assertThat(content, containsString("校园卡流水"));
        assertThat(content, containsString("提交报修工单"));
        assertThat(content, containsString("报修工单"));
        assertThat(content, containsString("课程管理"));
        assertThat(content, containsString("分页查询课程列表"));
        assertThat(content, containsString("课程名称"));
        assertThat(content, containsString("获取课程详情"));
        assertThat(content, containsString("新增课程"));
        assertThat(content, containsString("课程新增请求"));
        assertThat(content, containsString("课程详情"));
        assertThat(content, containsString("发起签到场次"));
        assertThat(content, containsString("考勤场次"));
        assertThat(content, containsString("分页查询成绩"));
        assertThat(content, containsString("成绩记录"));
        assertThat(content, containsString("响应码"));
    }
}
