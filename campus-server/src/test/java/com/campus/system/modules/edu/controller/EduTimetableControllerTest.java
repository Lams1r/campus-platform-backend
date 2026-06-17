package com.campus.system.modules.edu.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.campus.system.common.api.Result;
import com.campus.system.modules.edu.entity.EduTimetable;
import com.campus.system.modules.edu.service.IEduCourseService;
import com.campus.system.modules.edu.service.IEduStudentCourseService;
import com.campus.system.modules.edu.service.IEduTimetableService;
import com.campus.system.modules.sys.service.ISysUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EduTimetableControllerTest {

    @Mock
    private IEduTimetableService timetableService;
    @Mock
    private IEduCourseService courseService;
    @Mock
    private IEduStudentCourseService studentCourseService;
    @Mock
    private ISysUserService userService;
    @InjectMocks
    private EduTimetableController controller;

    @Test
    void myTimetableRejectsBlankSemesterBeforeQuerying() {
        Result<List<EduTimetableController.TimetableVO>> result = controller.myTimetable(" ");

        assertEquals(400, result.getCode());
        verify(timetableService, never()).list(Mockito.<Wrapper<EduTimetable>>any());
    }

    @Test
    void myTimetableReturnsEmptyListWhenTeacherHasNoRows() {
        when(timetableService.list(Mockito.<Wrapper<EduTimetable>>any())).thenReturn(Collections.emptyList());

        try (MockedStatic<StpUtil> stpUtil = Mockito.mockStatic(StpUtil.class)) {
            stpUtil.when(StpUtil::getLoginIdAsLong).thenReturn(100L);

            stpUtil.when(() -> StpUtil.hasRole("student")).thenReturn(false);

            Result<List<EduTimetableController.TimetableVO>> result = controller.myTimetable("2025-2026-2");

            assertEquals(200, result.getCode());
            assertTrue(result.getData().isEmpty());
        }
    }
}
