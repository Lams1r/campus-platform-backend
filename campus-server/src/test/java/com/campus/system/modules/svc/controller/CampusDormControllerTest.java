package com.campus.system.modules.svc.controller;

import com.campus.system.modules.svc.entity.CampusDormitoryAllocation;
import com.campus.system.modules.svc.service.ICampusDormitoryAllocationService;
import com.campus.system.modules.svc.service.ICampusDormitoryBuildingService;
import com.campus.system.modules.svc.service.ICampusDormitoryRoomService;
import com.campus.system.modules.svc.service.ICampusDormSwapRequestService;
import com.campus.system.modules.sys.service.ISysUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CampusDormControllerTest {

    @Mock
    private ICampusDormitoryBuildingService buildingService;
    @Mock
    private ICampusDormitoryRoomService roomService;
    @Mock
    private ICampusDormitoryAllocationService allocationService;
    @Mock
    private ICampusDormSwapRequestService swapService;
    @Mock
    private ISysUserService userService;
    @InjectMocks
    private CampusDormController controller;

    @Test
    void allocateDelegatesToAllocationService() {
        CampusDormitoryAllocation allocation = new CampusDormitoryAllocation();
        allocation.setRoomId(1L);
        allocation.setStudentId(300L);
        allocation.setBedNumber(1);

        controller.allocate(allocation);

        verify(allocationService).allocate(allocation);
    }

    @Test
    void deallocateDelegatesToAllocationService() {
        controller.deallocate(5L);

        verify(allocationService).deallocate(5L);
    }
}
