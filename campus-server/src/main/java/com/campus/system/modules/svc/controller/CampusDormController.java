package com.campus.system.modules.svc.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.system.annotation.LogRecord;
import com.campus.system.common.api.Result;
import com.campus.system.common.exception.BusinessException;
import com.campus.system.modules.svc.entity.CampusDormitoryAllocation;
import com.campus.system.modules.svc.entity.CampusDormitoryBuilding;
import com.campus.system.modules.svc.entity.CampusDormitoryRoom;
import com.campus.system.modules.svc.entity.CampusDormSwapRequest;
import com.campus.system.modules.svc.service.ICampusDormitoryAllocationService;
import com.campus.system.modules.svc.service.ICampusDormitoryBuildingService;
import com.campus.system.modules.svc.service.ICampusDormitoryRoomService;
import com.campus.system.modules.svc.service.ICampusDormSwapRequestService;
import com.campus.system.modules.sys.entity.SysUser;
import com.campus.system.modules.sys.service.ISysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/svc/dorm")
@RequiredArgsConstructor
@Tag(name = "宿舍管理", description = "宿舍楼、房间与分配管理接口")
public class CampusDormController {

    private final ICampusDormitoryBuildingService buildingService;
    private final ICampusDormitoryRoomService roomService;
    private final ICampusDormitoryAllocationService allocationService;
    private final ICampusDormSwapRequestService swapService;
    private final ISysUserService userService;

    // ========== 楼栋 ==========

    @GetMapping("/building/list")
    @Operation(summary = "查询宿舍楼列表")
    public Result<List<CampusDormitoryBuilding>> buildingList() {
        return Result.success(buildingService.list());
    }

    @PostMapping("/building")
    @SaCheckPermission("svc:dorm:add")
    @LogRecord(module = "宿舍管理", type = "新增楼栋")
    @Operation(summary = "新增宿舍楼")
    public Result<Void> addBuilding(@RequestBody CampusDormitoryBuilding building) {
        buildingService.save(building);
        return Result.success();
    }

    @PutMapping("/building")
    @SaCheckPermission("svc:dorm:edit")
    @Operation(summary = "更新宿舍楼")
    public Result<Void> updateBuilding(@RequestBody CampusDormitoryBuilding building) {
        buildingService.updateById(building);
        return Result.success();
    }

    @DeleteMapping("/building/{id}")
    @SaCheckPermission("svc:dorm:delete")
    @Operation(summary = "删除宿舍楼")
    public Result<Void> deleteBuilding(@PathVariable Long id) {
        buildingService.removeById(id);
        return Result.success();
    }

    // ========== 房间 ==========

    @GetMapping("/room/list")
    @Operation(summary = "查询房间列表")
    public Result<List<CampusDormitoryRoom>> roomList(
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<CampusDormitoryRoom> wrapper = new LambdaQueryWrapper<>();
        if (buildingId != null) wrapper.eq(CampusDormitoryRoom::getBuildingId, buildingId);
        if (status != null) wrapper.eq(CampusDormitoryRoom::getStatus, status);
        wrapper.orderByAsc(CampusDormitoryRoom::getRoomCode);
        return Result.success(roomService.list(wrapper));
    }

    @PostMapping("/room")
    @SaCheckPermission("svc:dorm:add")
    @Operation(summary = "新增房间")
    public Result<Void> addRoom(@RequestBody CampusDormitoryRoom room) {
        room.setUsedCount(0);
        room.setStatus(0);
        roomService.save(room);
        return Result.success();
    }

    @PutMapping("/room")
    @SaCheckPermission("svc:dorm:edit")
    @Operation(summary = "更新房间")
    public Result<Void> updateRoom(@RequestBody CampusDormitoryRoom room) {
        roomService.updateById(room);
        return Result.success();
    }

    // ========== 分配 ==========

    @GetMapping("/allocation/list")
    @SaCheckPermission("svc:dorm:list")
    @Operation(summary = "管理员查看所有分配记录")
    public Result<List<AllocationVO>> allAllocations() {
        List<CampusDormitoryAllocation> list = allocationService.list(
                new LambdaQueryWrapper<CampusDormitoryAllocation>()
                        .eq(CampusDormitoryAllocation::getStatus, 0)
                        .orderByDesc(CampusDormitoryAllocation::getId));
        return Result.success(list.stream().map(this::toAllocationVO).collect(Collectors.toList()));
    }

    @GetMapping("/allocation/my")
    @Operation(summary = "查看我的宿舍分配")
    public Result<AllocationVO> myAllocation() {
        Long userId = StpUtil.getLoginIdAsLong();
        CampusDormitoryAllocation allocation = allocationService.getOne(
                new LambdaQueryWrapper<CampusDormitoryAllocation>()
                        .eq(CampusDormitoryAllocation::getStudentId, userId)
                        .eq(CampusDormitoryAllocation::getStatus, 0)
                        .last("LIMIT 1"), false);
        if (allocation == null) return Result.success(null);
        return Result.success(toAllocationVO(allocation));
    }

    @PostMapping("/allocation")
    @SaCheckPermission("svc:dorm:edit")
    @LogRecord(module = "宿舍管理", type = "入住分配")
    @Operation(summary = "分配宿舍床位")
    public Result<Void> allocate(@RequestBody CampusDormitoryAllocation allocation) {
        allocationService.allocate(allocation);
        return Result.success();
    }

    @DeleteMapping("/allocation/{id}")
    @SaCheckPermission("svc:dorm:edit")
    @LogRecord(module = "宿舍管理", type = "退宿")
    @Operation(summary = "办理退宿")
    public Result<Void> deallocate(@PathVariable Long id) {
        allocationService.deallocate(id);
        return Result.success();
    }

    // ========== 换宿申请 ==========

    @PostMapping("/swap")
    @Operation(summary = "提交换宿舍申请")
    public Result<Void> submitSwap(@RequestBody CampusDormSwapRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        request.setStudentId(userId);
        request.setStatus(0);
        swapService.save(request);
        return Result.success();
    }

    @GetMapping("/swap/my")
    @Operation(summary = "我的换宿申请")
    public Result<List<CampusDormSwapRequest>> mySwaps() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(swapService.list(
                new LambdaQueryWrapper<CampusDormSwapRequest>()
                        .eq(CampusDormSwapRequest::getStudentId, userId)
                        .orderByDesc(CampusDormSwapRequest::getId)));
    }

    @GetMapping("/swap/page")
    @SaCheckPermission("svc:dorm:list")
    @Operation(summary = "管理员查看换宿申请")
    public Result<List<SwapRequestVO>> swapPage(@RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<CampusDormSwapRequest> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(CampusDormSwapRequest::getStatus, status);
        wrapper.orderByDesc(CampusDormSwapRequest::getId);
        List<CampusDormSwapRequest> list = swapService.list(wrapper);
        return Result.success(list.stream().map(req -> {
            SwapRequestVO vo = new SwapRequestVO();
            vo.setId(req.getId());
            vo.setStudentId(req.getStudentId());
            vo.setCurrentRoomId(req.getCurrentRoomId());
            vo.setTargetRoomId(req.getTargetRoomId());
            vo.setReason(req.getReason());
            vo.setStatus(req.getStatus());
            vo.setApproveRemark(req.getApproveRemark());
            vo.setCreateTime(req.getCreateTime());
            SysUser user = userService.getById(req.getStudentId());
            vo.setUserName(user != null ? user.getRealName() : "");
            vo.setUserNo(user != null ? user.getUsername() : "");
            vo.setUserType(user != null ? user.getUserType() : null);
            // 当前宿舍信息
            if (req.getCurrentRoomId() != null) {
                CampusDormitoryRoom room = roomService.getById(req.getCurrentRoomId());
                if (room != null) {
                    CampusDormitoryBuilding building = buildingService.getById(room.getBuildingId());
                    vo.setCurrentRoom(building.getBuildingName() + " " + room.getRoomCode() + "室");
                }
            }
            // 目标宿舍信息
            if (req.getTargetRoomId() != null) {
                CampusDormitoryRoom room = roomService.getById(req.getTargetRoomId());
                if (room != null) {
                    CampusDormitoryBuilding building = buildingService.getById(room.getBuildingId());
                    vo.setTargetRoom(building.getBuildingName() + " " + room.getRoomCode() + "室");
                }
            }
            return vo;
        }).collect(Collectors.toList()));
    }

    @PutMapping("/swap/{id}/approve")
    @SaCheckPermission("svc:dorm:edit")
    @Operation(summary = "审批换宿申请")
    public Result<Void> approveSwap(
            @PathVariable Long id,
            @RequestParam Integer status,
            @RequestParam(required = false) String remark) {
        if (status != 1 && status != 2) throw new BusinessException("状态只能为 1(通过) 或 2(驳回)");
        CampusDormSwapRequest req = swapService.getById(id);
        if (req == null || req.getStatus() != 0) throw new BusinessException("申请不存在或已处理");

        req.setStatus(status);
        req.setApproverId(StpUtil.getLoginIdAsLong());
        req.setApproveTime(LocalDateTime.now());
        req.setApproveRemark(remark);
        swapService.updateById(req);

        // 审批通过 → 执行实际宿舍调换
        if (status == 1 && req.getCurrentRoomId() != null && req.getTargetRoomId() != null) {
            // 1. 退宿：将当前分配标记为已退宿
            CampusDormitoryAllocation currentAllocation = allocationService.getOne(
                    new LambdaQueryWrapper<CampusDormitoryAllocation>()
                            .eq(CampusDormitoryAllocation::getStudentId, req.getStudentId())
                            .eq(CampusDormitoryAllocation::getRoomId, req.getCurrentRoomId())
                            .eq(CampusDormitoryAllocation::getStatus, 0)
                            .last("LIMIT 1"), false);

            if (currentAllocation != null) {
                allocationService.deallocate(currentAllocation.getId());
            }

            // 2. 分配到目标房间
            CampusDormitoryRoom targetRoom = roomService.getById(req.getTargetRoomId());
            if (targetRoom != null && targetRoom.getUsedCount() < targetRoom.getBedCount()) {
                CampusDormitoryAllocation newAllocation = new CampusDormitoryAllocation();
                newAllocation.setStudentId(req.getStudentId());
                newAllocation.setRoomId(req.getTargetRoomId());
                newAllocation.setBedNumber(targetRoom.getUsedCount() + 1);
                newAllocation.setCheckInDate(java.time.LocalDate.now());
                allocationService.allocate(newAllocation);
            }
        }

        return Result.success();
    }

    // ========== VO ==========

    private AllocationVO toAllocationVO(CampusDormitoryAllocation a) {
        AllocationVO vo = new AllocationVO();
        vo.setId(a.getId());
        vo.setStudentId(a.getStudentId());
        vo.setRoomId(a.getRoomId());
        vo.setBedNumber(a.getBedNumber());
        vo.setCheckInDate(a.getCheckInDate());
        vo.setStatus(a.getStatus());

        SysUser user = userService.getById(a.getStudentId());
        vo.setUserName(user != null ? user.getRealName() : "");
        vo.setUserNo(user != null ? user.getUsername() : "");
        vo.setUserType(user != null ? user.getUserType() : null);

        CampusDormitoryRoom room = roomService.getById(a.getRoomId());
        if (room != null) {
            vo.setRoomCode(room.getRoomCode());
            vo.setFloor(room.getFloor());
            CampusDormitoryBuilding building = buildingService.getById(room.getBuildingId());
            vo.setBuildingName(building != null ? building.getBuildingName() : "");
        }
        return vo;
    }

    @lombok.Data
    public static class AllocationVO {
        private Long id;
        private Long studentId;
        private String userName;
        private String userNo;
        private Integer userType;
        private Long roomId;
        private String buildingName;
        private String roomCode;
        private Integer floor;
        private Integer bedNumber;
        private java.time.LocalDate checkInDate;
        private Integer status;
    }

    @lombok.Data
    public static class SwapRequestVO {
        private Long id;
        private Long studentId;
        private String userName;
        private String userNo;
        private Integer userType;
        private Long currentRoomId;
        private String currentRoom;
        private Long targetRoomId;
        private String targetRoom;
        private String reason;
        private Integer status;
        private String approveRemark;
        private java.time.LocalDateTime createTime;
    }
}
