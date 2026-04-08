package com.campus.system.modules.svc.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alicp.jetcache.Cache;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.CreateCache;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.system.common.api.Result;
import com.campus.system.modules.edu.service.IEduAttendanceSessionService;
import com.campus.system.modules.edu.service.IEduCourseService;
import com.campus.system.modules.svc.entity.CampusBookBorrow;
import com.campus.system.modules.svc.entity.CampusDashboardSnapshot;
import com.campus.system.modules.svc.entity.CampusRepairOrder;
import com.campus.system.modules.svc.service.ICampusBookBorrowService;
import com.campus.system.modules.svc.service.ICampusBookService;
import com.campus.system.modules.svc.service.ICampusDashboardSnapshotService;
import com.campus.system.modules.svc.service.ICampusRepairOrderService;
import com.campus.system.modules.sys.service.ISysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 数据看板控制器。
 */
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "数据看板", description = "首页概览与统计快照接口")
public class DashboardController {

    private static final String OVERVIEW_SNAPSHOT_KEY = "dashboard_overview";

    private final ISysUserService userService;
    private final IEduCourseService courseService;
    private final IEduAttendanceSessionService attendanceSessionService;
    private final ICampusRepairOrderService repairService;
    private final ICampusBookService bookService;
    private final ICampusBookBorrowService borrowService;
    private final ICampusDashboardSnapshotService snapshotService;

    @CreateCache(name = "dashboard:overview:", expire = 660, cacheType = CacheType.BOTH)
    private Cache<String, CampusDashboardSnapshot> overviewSnapshotCache;

    @GetMapping("/overview")
    @Operation(summary = "获取看板概览")
    public Result<Map<String, Object>> overview() {
        CampusDashboardSnapshot snapshot = getCachedOverviewSnapshot();
        if (snapshot == null) {
            snapshot = findOverviewSnapshot();
            if (snapshot != null) {
                cacheOverviewSnapshot(snapshot);
            } else {
                snapshot = refreshOverviewSnapshotInternal();
            }
        }
        return Result.success(parseSnapshotData(snapshot));
    }

    @PostMapping("/snapshot")
    @SaCheckPermission("dashboard:snapshot")
    @Operation(summary = "生成看板快照")
    public Result<Void> saveSnapshot() {
        refreshOverviewSnapshotInternal();
        return Result.success();
    }

    @GetMapping("/snapshot/latest")
    @Operation(summary = "获取最新看板快照")
    public Result<CampusDashboardSnapshot> latestSnapshot() {
        CampusDashboardSnapshot snapshot = getCachedOverviewSnapshot();
        if (snapshot == null) {
            snapshot = findOverviewSnapshot();
            if (snapshot != null) {
                cacheOverviewSnapshot(snapshot);
            }
        }
        return Result.success(snapshot);
    }

    @Scheduled(cron = "0 0/10 * * * *")
    public void refreshOverviewSnapshot() {
        refreshOverviewSnapshotInternal();
    }

    private CampusDashboardSnapshot refreshOverviewSnapshotInternal() {
        CampusDashboardSnapshot snapshot = findOverviewSnapshot();
        if (snapshot == null) {
            snapshot = new CampusDashboardSnapshot();
            snapshot.setSnapshotKey(OVERVIEW_SNAPSHOT_KEY);
            applySnapshotData(snapshot);
            snapshotService.save(snapshot);
        } else {
            applySnapshotData(snapshot);
            snapshotService.updateById(snapshot);
        }
        cacheOverviewSnapshot(snapshot);
        return snapshot;
    }

    private Map<String, Object> buildOverviewData() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalUsers", userService.count());
        data.put("totalCourses", courseService.count());
        data.put("totalAttendanceSessions", attendanceSessionService.count());
        data.put("totalRepairOrders", repairService.count());
        data.put("pendingRepairOrders", repairService.count(
                new LambdaQueryWrapper<CampusRepairOrder>().eq(CampusRepairOrder::getStatus, 0)
        ));
        data.put("totalBooks", bookService.count());
        data.put("borrowingCount", borrowService.count(
                new LambdaQueryWrapper<CampusBookBorrow>().eq(CampusBookBorrow::getStatus, 0)
        ));
        return data;
    }

    private CampusDashboardSnapshot findOverviewSnapshot() {
        return snapshotService.getOne(new LambdaQueryWrapper<CampusDashboardSnapshot>()
                .eq(CampusDashboardSnapshot::getSnapshotKey, OVERVIEW_SNAPSHOT_KEY)
                .last("LIMIT 1"), false);
    }

    private void applySnapshotData(CampusDashboardSnapshot snapshot) {
        snapshot.setSnapshotKey(OVERVIEW_SNAPSHOT_KEY);
        snapshot.setSnapshotData(JSONUtil.toJsonStr(buildOverviewData()));
        snapshot.setSnapshotTime(LocalDateTime.now());
    }

    private CampusDashboardSnapshot getCachedOverviewSnapshot() {
        return overviewSnapshotCache == null ? null : overviewSnapshotCache.get(OVERVIEW_SNAPSHOT_KEY);
    }

    private void cacheOverviewSnapshot(CampusDashboardSnapshot snapshot) {
        if (overviewSnapshotCache != null && snapshot != null) {
            overviewSnapshotCache.put(OVERVIEW_SNAPSHOT_KEY, snapshot);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseSnapshotData(CampusDashboardSnapshot snapshot) {
        if (snapshot == null || StrUtil.isBlank(snapshot.getSnapshotData())) {
            return new LinkedHashMap<>();
        }
        return JSONUtil.toBean(snapshot.getSnapshotData(), LinkedHashMap.class);
    }
}
