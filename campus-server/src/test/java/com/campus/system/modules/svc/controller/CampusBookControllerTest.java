package com.campus.system.modules.svc.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.campus.system.modules.svc.service.ICampusBookBorrowService;
import com.campus.system.modules.svc.service.ICampusBookService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CampusBookControllerTest {

    @Mock
    private ICampusBookService bookService;
    @Mock
    private ICampusBookBorrowService borrowService;
    @InjectMocks
    private CampusBookController controller;

    @Test
    void borrowDelegatesToBookServiceWithCurrentUser() {
        try (MockedStatic<StpUtil> stpUtil = Mockito.mockStatic(StpUtil.class)) {
            stpUtil.when(StpUtil::getLoginIdAsLong).thenReturn(100L);

            controller.borrow(1L);

            verify(bookService).borrow(1L, 100L);
        }
    }

    @Test
    void returnBookDelegatesToBookServiceWithCurrentUserAndAdminFlag() {
        try (MockedStatic<StpUtil> stpUtil = Mockito.mockStatic(StpUtil.class)) {
            stpUtil.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
            stpUtil.when(() -> StpUtil.hasRole("admin")).thenReturn(false);

            controller.returnBook(5L);

            verify(bookService).returnBook(5L, 100L, false);
        }
    }
}
