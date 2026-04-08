package com.campus.system.modules.svc.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.system.annotation.LogRecord;
import com.campus.system.common.api.PageResult;
import com.campus.system.common.api.Result;
import com.campus.system.common.exception.BusinessException;
import com.campus.system.modules.svc.entity.CampusCardLoss;
import com.campus.system.modules.svc.entity.CampusCardRecord;
import com.campus.system.modules.svc.service.ICampusCardLossService;
import com.campus.system.modules.svc.service.ICampusCardRecordService;
import com.campus.system.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 校园卡管理控制器。
 */
@RestController
@RequestMapping("/svc/card")
@RequiredArgsConstructor
@Tag(name = "校园卡管理", description = "校园卡充值、挂失与流水查询接口")
public class CampusCardController {

    private final ICampusCardRecordService cardRecordService;
    private final ICampusCardLossService cardLossService;

    @GetMapping("/record/my")
    @Operation(summary = "查询我的校园卡流水")
    public Result<PageResult<CampusCardRecord>> myRecords(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") Integer pageSize,
            @Parameter(description = "交易类型，0-消费，1-充值") @RequestParam(required = false) Integer transactionType) {
        Long studentId = SecurityUtils.getCurrentUserId();
        LambdaQueryWrapper<CampusCardRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CampusCardRecord::getStudentId, studentId);
        if (transactionType != null) {
            wrapper.eq(CampusCardRecord::getTransactionType, transactionType);
        }
        wrapper.orderByDesc(CampusCardRecord::getTransactionTime);

        Page<CampusCardRecord> page = cardRecordService.page(new Page<>(pageNum, pageSize), wrapper);
        return Result.success(new PageResult<>(page.getTotal(), page.getRecords(), (long) pageNum, (long) pageSize));
    }

    @GetMapping("/record/page")
    @SaCheckPermission("svc:card:list")
    @Operation(summary = "分页查询校园卡流水", description = "管理员按学生维度查询全部校园卡流水")
    public Result<PageResult<CampusCardRecord>> recordPage(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") Integer pageSize,
            @Parameter(description = "学生ID") @RequestParam(required = false) Long studentId) {
        LambdaQueryWrapper<CampusCardRecord> wrapper = new LambdaQueryWrapper<>();
        if (studentId != null) {
            wrapper.eq(CampusCardRecord::getStudentId, studentId);
        }
        wrapper.orderByDesc(CampusCardRecord::getTransactionTime);
        Page<CampusCardRecord> page = cardRecordService.page(new Page<>(pageNum, pageSize), wrapper);
        return Result.success(new PageResult<>(page.getTotal(), page.getRecords(), (long) pageNum, (long) pageSize));
    }

    @PostMapping("/recharge")
    @SaCheckPermission("svc:card:recharge")
    @LogRecord(module = "校园卡管理", type = "充值拦截")
    @Operation(summary = "同步外部充值流水", description = "系统内不允许直接充值，调用该接口会返回业务拦截提示")
    public Result<Void> recharge(
            @Parameter(description = "学生ID") @RequestParam Long studentId,
            @Parameter(description = "校园卡号") @RequestParam String cardNo,
            @Parameter(description = "充值金额") @RequestParam BigDecimal amount) {
        throw new BusinessException("校园卡金额仅支持同步外部流水，禁止在本系统内发起充值");
    }

    @PostMapping("/loss/report")
    @Operation(summary = "挂失校园卡")
    public Result<Void> reportLoss(@Parameter(description = "校园卡号") @RequestParam String cardNo) {
        Long studentId = SecurityUtils.getCurrentUserId();
        long activeLossCount = cardLossService.count(new LambdaQueryWrapper<CampusCardLoss>()
                .eq(CampusCardLoss::getStudentId, studentId)
                .eq(CampusCardLoss::getCardNo, cardNo)
                .eq(CampusCardLoss::getStatus, 0));
        if (activeLossCount > 0) {
            throw new BusinessException("该校园卡已处于挂失状态");
        }

        CampusCardLoss loss = new CampusCardLoss();
        loss.setStudentId(studentId);
        loss.setCardNo(cardNo);
        loss.setStatus(0);
        loss.setLossTime(LocalDateTime.now());
        cardLossService.save(loss);
        return Result.success();
    }

    @PutMapping("/loss/{id}/unlock")
    @SaCheckPermission("svc:card:edit")
    @Operation(summary = "解除校园卡挂失")
    public Result<Void> unlockLoss(@Parameter(description = "挂失记录ID") @PathVariable Long id) {
        CampusCardLoss loss = cardLossService.getById(id);
        if (loss == null) {
            throw new BusinessException("挂失记录不存在");
        }
        if (!Integer.valueOf(0).equals(loss.getStatus())) {
            throw new BusinessException("该挂失记录不可解挂");
        }
        loss.setStatus(1);
        loss.setUnlockTime(LocalDateTime.now());
        cardLossService.updateById(loss);
        return Result.success();
    }
}
