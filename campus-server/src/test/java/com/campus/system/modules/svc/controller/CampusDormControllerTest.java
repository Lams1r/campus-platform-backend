package com.campus.system.modules.svc.controller;

import com.campus.system.common.exception.BusinessException;
import com.campus.system.modules.svc.entity.CampusDormitoryAllocation;
import com.campus.system.modules.svc.entity.CampusDormitoryRoom;
import com.campus.system.modules.svc.service.ICampusDormitoryAllocationService;
import com.campus.system.modules.svc.service.ICampusDormitoryBuildingService;
import com.campus.system.modules.svc.service.ICampusDormitoryRoomService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampusDormControllerTest {

    @Mock
    private ICampusDormitoryBuildingService buildingService;
    @Mock
    private ICampusDormitoryRoomService roomService;
    @Mock
    private ICampusDormitoryAllocationService allocationService;
    @InjectMocks
    private CampusDormController controller;

    @Test
    void allocateRejectsOccupiedBedNumbers() {
        CampusDormitoryRoom room = new CampusDormitoryRoom();
        room.setId(1L);
        room.setBedCount(4);
        room.setUsedCount(1);
        room.setStatus(0);
        when(roomService.getById(1L)).thenReturn(room);
        when(allocationService.count(any())).thenReturn(1L);

        CampusDormitoryAllocation allocation = new CampusDormitoryAllocation();
        allocation.setRoomId(1L);
        allocation.setStudentId(300L);
        allocation.setBedNumber(1);

        assertThrows(BusinessException.class, () -> controller.allocate(allocation));
        verify(allocationService, never()).save(any());
    }
}
