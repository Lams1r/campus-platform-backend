package com.campus.system.modules.svc.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.campus.system.common.exception.BusinessException;
import com.campus.system.modules.svc.entity.CampusBook;
import com.campus.system.modules.svc.entity.CampusBookBorrow;
import com.campus.system.modules.svc.service.ICampusBookBorrowService;
import com.campus.system.modules.svc.service.ICampusBookService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampusBookControllerTest {

    @Mock
    private ICampusBookService bookService;
    @Mock
    private ICampusBookBorrowService borrowService;
    @InjectMocks
    private CampusBookController controller;

    @Test
    void borrowFailsWhenInventoryUpdateDidNotSucceed() {
        CampusBook book = new CampusBook();
        book.setId(1L);
        book.setAvailableCount(1);
        when(bookService.getById(1L)).thenReturn(book);
        when(bookService.update(any())).thenReturn(false);
        when(borrowService.count(any())).thenReturn(0L);

        try (MockedStatic<StpUtil> stpUtil = Mockito.mockStatic(StpUtil.class)) {
            stpUtil.when(StpUtil::getLoginIdAsLong).thenReturn(100L);

            assertThrows(BusinessException.class, () -> controller.borrow(1L));
            verify(borrowService, never()).save(any());
        }
    }

    @Test
    void returnBookRejectsUsersWhoDoNotOwnTheBorrowRecord() {
        CampusBookBorrow borrow = new CampusBookBorrow();
        borrow.setId(5L);
        borrow.setBookId(1L);
        borrow.setStudentId(200L);
        borrow.setStatus(0);
        borrow.setDueTime(LocalDateTime.now().plusDays(1));
        when(borrowService.getById(5L)).thenReturn(borrow);

        try (MockedStatic<StpUtil> stpUtil = Mockito.mockStatic(StpUtil.class)) {
            stpUtil.when(StpUtil::getLoginIdAsLong).thenReturn(100L);

            assertThrows(BusinessException.class, () -> controller.returnBook(5L));
            verify(borrowService, never()).updateById(any());
        }
    }
}
