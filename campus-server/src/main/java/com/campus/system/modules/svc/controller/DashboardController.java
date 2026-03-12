package com.campus.system.modules.svc.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.system.common.api.Result;
import com.campus.system.modules.edu.service.*;
import com.campus.system.modules.svc.entity.CampusDashboardSnapshot;
import com.campus.system.modules.svc.service.*;
import com.campus.system.modules.sys.service.ISysUserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 大屏数据概览控制器
 * 提供首页仪表盘统计数据（实时计算 + 快照缓存）
 */
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final ISysUserService userService;
    private final IEduCourseService courseService;
    private final IEduAttendanceSessionService attendanceSessionService;
    private final ICampusRepairOrderService repairService;
    private final ICampusBookService bookService;
    private final ICampusBookBorrowService borrowService;
    private final ICampusDashboardSnapshotService snapshotService;

    /**
     * 实时统计首页概览数据
     */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        Map<String, Object> data = new LinkedHashMap<>();

        // 系统用户总数
        data.put("totalUsers", userService.count());

        // 课程总数
        data.put("totalCourses", courseService.count());

        // 考勤场次数
        data.put("totalAttendanceSessions", attendanceSessionService.count());

        // 报修工单统计
        data.put("totalRepairOrders", repairService.count());
        data.put("pendingRepairOrders", repairService.count(
                new LambdaQueryWrapper<com.campus.system.modules.svc.entity.CampusRepairOrder>()
                        .eq(com.campus.system.modules.svc.entity.CampusRepairOrder::getStatus, 0)
        ));

        // 图书馆统计
        data.put("totalBooks", bookService.count());
        data.put("borrowingCount", borrowService.count(
                new LambdaQueryWrapper<com.campus.system.modules.svc.entity.CampusBookBorrow>()
                        .eq(com.campus.system.modules.svc.entity.CampusBookBorrow::getStatus, 0)
        ));

        return Result.success(data);
    }

    /**
     * 保存大屏快照（定时任务或手动触发）
     */
    @PostMapping("/snapshot")
    @SaCheckPermission("dashboard:snapshot")
    public Result<Void> saveSnapshot() {
        Map<String, Object> data = overview().getData();

        CampusDashboardSnapshot snapshot = new CampusDashboardSnapshot();
        snapshot.setSnapshotKey("dashboard_overview");
        snapshot.setSnapshotData(JSONUtil.toJsonStr(data));
        snapshot.setSnapshotTime(LocalDateTime.now());
        snapshotService.save(snapshot);
        return Result.success();
    }

    /**
     * 获取最新快照
     */
    @GetMapping("/snapshot/latest")
    public Result<CampusDashboardSnapshot> latestSnapshot() {
        CampusDashboardSnapshot snapshot = snapshotService.getOne(
                new LambdaQueryWrapper<CampusDashboardSnapshot>()
                        .eq(CampusDashboardSnapshot::getSnapshotKey, "dashboard_overview")
                        .orderByDesc(CampusDashboardSnapshot::getSnapshotTime)
                        .last("LIMIT 1")
        );
        return Result.success(snapshot);
    }
}
