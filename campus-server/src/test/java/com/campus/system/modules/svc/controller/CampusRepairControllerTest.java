package com.campus.system.modules.svc.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.campus.system.common.exception.BusinessException;
import com.campus.system.modules.svc.entity.CampusRepairOrder;
import com.campus.system.modules.svc.service.ICampusRepairOrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampusRepairControllerTest {

    @Mock
    private ICampusRepairOrderService repairService;
    @InjectMocks
    private CampusRepairController controller;

    @Test
    void verifyRejectsUsersWhoAreNotTheApplicant() {
        CampusRepairOrder order = new CampusRepairOrder();
        order.setId(1L);
        order.setApplicantId(200L);
        order.setStatus(2);
        when(repairService.getById(1L)).thenReturn(order);

        try (MockedStatic<StpUtil> stpUtil = Mockito.mockStatic(StpUtil.class)) {
            stpUtil.when(StpUtil::getLoginIdAsLong).thenReturn(100L);

            assertThrows(BusinessException.class, () -> controller.verify(1L, 5, "ok"));
            verify(repairService, never()).updateById(any());
        }
    }
}
