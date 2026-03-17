package com.campus.system.modules.svc.controller;

import com.campus.system.modules.edu.service.IEduAttendanceSessionService;
import com.campus.system.modules.edu.service.IEduCourseService;
import com.campus.system.modules.svc.entity.CampusDashboardSnapshot;
import com.campus.system.modules.svc.service.ICampusBookBorrowService;
import com.campus.system.modules.svc.service.ICampusBookService;
import com.campus.system.modules.svc.service.ICampusDashboardSnapshotService;
import com.campus.system.modules.svc.service.ICampusRepairOrderService;
import com.campus.system.modules.sys.service.ISysUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private ISysUserService userService;
    @Mock
    private IEduCourseService courseService;
    @Mock
    private IEduAttendanceSessionService attendanceSessionService;
    @Mock
    private ICampusRepairOrderService repairService;
    @Mock
    private ICampusBookService bookService;
    @Mock
    private ICampusBookBorrowService borrowService;
    @Mock
    private ICampusDashboardSnapshotService snapshotService;
    @InjectMocks
    private DashboardController controller;

    @Test
    void saveSnapshotUpdatesExistingSnapshotInsteadOfInsertingDuplicateKey() {
        when(userService.count()).thenReturn(10L);
        when(courseService.count()).thenReturn(5L);
        when(attendanceSessionService.count()).thenReturn(3L);
        when(repairService.count()).thenReturn(7L);
        when(repairService.count(any())).thenReturn(2L);
        when(bookService.count()).thenReturn(11L);
        when(borrowService.count(any())).thenReturn(4L);

        CampusDashboardSnapshot existing = new CampusDashboardSnapshot();
        existing.setId(9L);
        when(snapshotService.getOne(any(), eq(false))).thenReturn(existing);

        controller.saveSnapshot();

        ArgumentCaptor<CampusDashboardSnapshot> captor = ArgumentCaptor.forClass(CampusDashboardSnapshot.class);
        verify(snapshotService).updateById(captor.capture());
        verify(snapshotService, never()).save(any());
        assertEquals(9L, captor.getValue().getId());
        assertEquals("dashboard_overview", captor.getValue().getSnapshotKey());
        assertNotNull(captor.getValue().getSnapshotTime());
    }
}