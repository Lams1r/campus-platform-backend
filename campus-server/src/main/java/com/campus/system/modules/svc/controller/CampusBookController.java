package com.campus.system.modules.svc.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.system.annotation.LogRecord;
import com.campus.system.common.api.PageResult;
import com.campus.system.common.api.Result;
import com.campus.system.common.exception.BusinessException;
import com.campus.system.modules.svc.entity.CampusBook;
import com.campus.system.modules.svc.entity.CampusBookBorrow;
import com.campus.system.modules.svc.service.ICampusBookBorrowService;
import com.campus.system.modules.svc.service.ICampusBookService;
import com.campus.system.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 图书管理控制器（图书台账 + 借阅/归还）
 */
@RestController
@RequestMapping("/svc/book")
@RequiredArgsConstructor
@Tag(name = "图书管理", description = "图书台账与借阅归还接口")
public class CampusBookController {

    private final ICampusBookService bookService;
    private final ICampusBookBorrowService borrowService;

    @GetMapping("/page")
    @Operation(summary = "分页查询图书列表")
    public Result<PageResult<CampusBook>> page(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "图书分类") @RequestParam(required = false) String category) {

        LambdaQueryWrapper<CampusBook> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(CampusBook::getBookName, keyword)
                    .or().like(CampusBook::getAuthor, keyword)
                    .or().like(CampusBook::getIsbn, keyword));
        }
        if (StrUtil.isNotBlank(category)) wrapper.eq(CampusBook::getCategory, category);
        wrapper.orderByDesc(CampusBook::getId);

        Page<CampusBook> page = bookService.page(new Page<>(pageNum, pageSize), wrapper);
        return Result.success(new PageResult<>(page.getTotal(), page.getRecords(), (long) pageNum, (long) pageSize));
    }

    @PostMapping
    @SaCheckPermission("svc:book:add")
    @LogRecord(module = "图书管理", type = "新增")
    @Operation(summary = "新增图书")
    public Result<Void> add(@RequestBody CampusBook book) {
        book.setAvailableCount(book.getTotalCount());
        bookService.save(book);
        return Result.success();
    }

    @PutMapping
    @SaCheckPermission("svc:book:edit")
    @Operation(summary = "更新图书")
    public Result<Void> update(@RequestBody CampusBook book) {
        bookService.updateById(book);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("svc:book:delete")
    @Operation(summary = "删除图书")
    public Result<Void> delete(@Parameter(description = "图书ID") @PathVariable Long id) {
        bookService.removeById(id);
        return Result.success();
    }

    @PostMapping("/borrow")
    @Transactional(rollbackFor = Exception.class)
    @Operation(summary = "提交图书借阅")
    public Result<Void> borrow(@Parameter(description = "图书ID") @RequestParam Long bookId) {
        Long studentId = SecurityUtils.getCurrentUserId();

        long borrowing = borrowService.count(
                new LambdaQueryWrapper<CampusBookBorrow>()
                        .eq(CampusBookBorrow::getBookId, bookId)
                        .eq(CampusBookBorrow::getStudentId, studentId)
                        .eq(CampusBookBorrow::getStatus, 0)
        );
        if (borrowing > 0) throw new BusinessException("您已借阅此书且未归还");

        CampusBook book = bookService.getById(bookId);
        if (book == null) throw new BusinessException("图书不存在");
        if (book.getAvailableCount() <= 0) throw new BusinessException("该图书暂无可借库存");

        boolean inventoryUpdated = bookService.update(new LambdaUpdateWrapper<CampusBook>()
                .eq(CampusBook::getId, bookId)
                .gt(CampusBook::getAvailableCount, 0)
                .setSql("available_count = available_count - 1"));
        if (!inventoryUpdated) {
            throw new BusinessException("该图书暂无可借库存");
        }

        CampusBookBorrow borrow = new CampusBookBorrow();
        borrow.setBookId(bookId);
        borrow.setStudentId(studentId);
        borrow.setBorrowTime(LocalDateTime.now());
        borrow.setDueTime(LocalDateTime.now().plusDays(30));
        borrow.setStatus(0);
        borrow.setOverdueDays(0);
        borrowService.save(borrow);
        return Result.success();
    }

    @PutMapping("/return/{borrowId}")
    @Transactional(rollbackFor = Exception.class)
    @Operation(summary = "归还图书")
    public Result<Void> returnBook(@Parameter(description = "借阅记录ID") @PathVariable Long borrowId) {
        CampusBookBorrow borrow = borrowService.getById(borrowId);
        if (borrow == null) throw new BusinessException("借阅记录不存在");
        if (borrow.getStatus() != 0 && borrow.getStatus() != 2) throw new BusinessException("该记录已归还");

        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (!currentUserId.equals(borrow.getStudentId()) && !SecurityUtils.hasRole("admin")) {
            throw new BusinessException("无权归还该借阅记录");
        }

        borrow.setReturnTime(LocalDateTime.now());
        borrow.setStatus(1);

        if (LocalDateTime.now().isAfter(borrow.getDueTime())) {
            long days = ChronoUnit.DAYS.between(borrow.getDueTime(), LocalDateTime.now());
            borrow.setOverdueDays((int) days);
        }
        borrowService.updateById(borrow);

        bookService.update(new LambdaUpdateWrapper<CampusBook>()
                .eq(CampusBook::getId, borrow.getBookId())
                .setSql("available_count = available_count + 1"));
        return Result.success();
    }

    @GetMapping("/borrow/my")
    @Operation(summary = "查询我的借阅记录")
    public Result<PageResult<CampusBookBorrow>> myBorrows(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize) {
        Long studentId = SecurityUtils.getCurrentUserId();
        Page<CampusBookBorrow> page = borrowService.page(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<CampusBookBorrow>()
                        .eq(CampusBookBorrow::getStudentId, studentId)
                        .orderByDesc(CampusBookBorrow::getId)
        );
        return Result.success(new PageResult<>(page.getTotal(), page.getRecords(), (long) pageNum, (long) pageSize));
    }

    @GetMapping("/borrow/page")
    @SaCheckPermission("svc:book:list")
    @Operation(summary = "分页查询借阅记录")
    public Result<PageResult<CampusBookBorrow>> borrowPage(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") Integer pageSize,
            @Parameter(description = "借阅状态") @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<CampusBookBorrow> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(CampusBookBorrow::getStatus, status);
        wrapper.orderByDesc(CampusBookBorrow::getId);
        Page<CampusBookBorrow> page = borrowService.page(new Page<>(pageNum, pageSize), wrapper);
        return Result.success(new PageResult<>(page.getTotal(), page.getRecords(), (long) pageNum, (long) pageSize));
    }
}
