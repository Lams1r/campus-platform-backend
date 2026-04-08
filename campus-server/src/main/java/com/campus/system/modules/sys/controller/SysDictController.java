package com.campus.system.modules.sys.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.system.common.api.Result;
import com.campus.system.common.exception.BusinessException;
import com.campus.system.modules.sys.entity.SysDictData;
import com.campus.system.modules.sys.entity.SysDictType;
import com.campus.system.modules.sys.service.ISysDictDataService;
import com.campus.system.modules.sys.service.ISysDictTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 字典管理控制器。
 */
@RestController
@RequestMapping("/sys/dict")
@RequiredArgsConstructor
@Tag(name = "字典管理", description = "字典类型与字典数据维护接口")
public class SysDictController {

    private final ISysDictTypeService dictTypeService;
    private final ISysDictDataService dictDataService;

    @GetMapping("/type/list")
    @SaCheckPermission("sys:dict:list")
    @Operation(summary = "查询字典类型列表")
    public Result<List<SysDictType>> typeList() {
        return Result.success(dictTypeService.list(
                new LambdaQueryWrapper<SysDictType>().orderByAsc(SysDictType::getId)
        ));
    }

    @PostMapping("/type")
    @SaCheckPermission("sys:dict:add")
    @Operation(summary = "新增字典类型")
    public Result<Void> addType(@Valid @RequestBody SysDictType dictType) {
        long count = dictTypeService.count(
                new LambdaQueryWrapper<SysDictType>().eq(SysDictType::getDictType, dictType.getDictType())
        );
        if (count > 0) {
            throw new BusinessException("字典类型标识 '" + dictType.getDictType() + "' 已存在");
        }
        dictTypeService.save(dictType);
        return Result.success();
    }

    @PutMapping("/type")
    @SaCheckPermission("sys:dict:edit")
    @Operation(summary = "更新字典类型")
    public Result<Void> updateType(@Valid @RequestBody SysDictType dictType) {
        dictTypeService.updateById(dictType);
        return Result.success();
    }

    @DeleteMapping("/type/{id}")
    @SaCheckPermission("sys:dict:delete")
    @Operation(summary = "删除字典类型", description = "删除字典类型并同步删除对应字典数据")
    public Result<Void> deleteType(@Parameter(description = "字典类型ID") @PathVariable Long id) {
        SysDictType type = dictTypeService.getById(id);
        if (type == null) {
            throw new BusinessException("字典类型不存在");
        }
        dictTypeService.removeById(id);
        dictDataService.remove(
                new LambdaQueryWrapper<SysDictData>().eq(SysDictData::getDictType, type.getDictType())
        );
        return Result.success();
    }

    @GetMapping("/data/{dictType}")
    @Operation(summary = "按类型查询字典数据")
    public Result<List<SysDictData>> dataByType(@Parameter(description = "字典类型标识") @PathVariable String dictType) {
        return Result.success(dictDataService.list(
                new LambdaQueryWrapper<SysDictData>()
                        .eq(SysDictData::getDictType, dictType)
                        .eq(SysDictData::getStatus, 0)
                        .orderByAsc(SysDictData::getSortOrder)
        ));
    }

    @PostMapping("/data")
    @SaCheckPermission("sys:dict:add")
    @Operation(summary = "新增字典数据")
    public Result<Void> addData(@Valid @RequestBody SysDictData dictData) {
        dictDataService.save(dictData);
        return Result.success();
    }

    @PutMapping("/data")
    @SaCheckPermission("sys:dict:edit")
    @Operation(summary = "更新字典数据")
    public Result<Void> updateData(@Valid @RequestBody SysDictData dictData) {
        dictDataService.updateById(dictData);
        return Result.success();
    }

    @DeleteMapping("/data/{id}")
    @SaCheckPermission("sys:dict:delete")
    @Operation(summary = "删除字典数据")
    public Result<Void> deleteData(@Parameter(description = "字典数据ID") @PathVariable Long id) {
        dictDataService.removeById(id);
        return Result.success();
    }
}
