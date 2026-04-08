package com.campus.system.modules.sys.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.system.common.api.Result;
import com.campus.system.modules.sys.entity.SysMenu;
import com.campus.system.modules.sys.service.ISysMenuService;
import com.campus.system.modules.sys.vo.MenuTreeVO;
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
import java.util.stream.Collectors;

/**
 * 菜单管理控制器。
 */
@RestController
@RequestMapping("/sys/menu")
@RequiredArgsConstructor
@Tag(name = "菜单管理", description = "系统菜单与权限树接口")
public class SysMenuController {

    private final ISysMenuService menuService;

    @GetMapping("/tree")
    @SaCheckPermission("sys:menu:list")
    @Operation(summary = "获取菜单树", description = "返回完整的系统菜单树形结构")
    public Result<List<MenuTreeVO>> tree() {
        List<SysMenu> allMenus = menuService.list(
                new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getSortOrder)
        );
        return Result.success(buildTree(allMenus, 0L));
    }

    @GetMapping("/list")
    @SaCheckPermission("sys:menu:list")
    @Operation(summary = "查询菜单列表", description = "返回平铺结构的菜单列表")
    public Result<List<SysMenu>> list() {
        return Result.success(menuService.list(
                new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getSortOrder)
        ));
    }

    @PostMapping
    @SaCheckPermission("sys:menu:add")
    @Operation(summary = "新增菜单")
    public Result<Void> add(@Valid @RequestBody SysMenu menu) {
        menuService.save(menu);
        return Result.success();
    }

    @PutMapping
    @SaCheckPermission("sys:menu:edit")
    @Operation(summary = "更新菜单")
    public Result<Void> update(@Valid @RequestBody SysMenu menu) {
        menuService.updateById(menu);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("sys:menu:delete")
    @Operation(summary = "删除菜单", description = "删除前会校验是否存在子菜单")
    public Result<Void> delete(@Parameter(description = "菜单ID") @PathVariable Long id) {
        long childCount = menuService.count(
                new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, id)
        );
        if (childCount > 0) {
            return Result.error("该菜单下存在子节点，请先删除子节点");
        }
        menuService.removeById(id);
        return Result.success();
    }

    private List<MenuTreeVO> buildTree(List<SysMenu> all, Long parentId) {
        return all.stream()
                .filter(menu -> parentId.equals(menu.getParentId()))
                .map(menu -> {
                    MenuTreeVO vo = new MenuTreeVO();
                    BeanUtil.copyProperties(menu, vo);
                    vo.setChildren(buildTree(all, menu.getId()));
                    return vo;
                })
                .collect(Collectors.toList());
    }
}
