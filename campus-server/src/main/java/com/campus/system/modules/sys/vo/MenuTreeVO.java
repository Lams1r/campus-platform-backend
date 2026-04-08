package com.campus.system.modules.sys.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 菜单树节点。
 */
@Data
@Schema(name = "菜单树节点", description = "系统菜单树形结构节点")
public class MenuTreeVO {

    @Schema(description = "菜单ID")
    private Long id;

    @Schema(description = "父级菜单ID")
    private Long parentId;

    @Schema(description = "菜单名称")
    private String menuName;

    @Schema(description = "菜单类型")
    private String menuType;

    @Schema(description = "路由地址")
    private String path;

    @Schema(description = "前端组件路径")
    private String component;

    @Schema(description = "权限标识")
    private String perms;

    @Schema(description = "菜单图标")
    private String icon;

    @Schema(description = "排序值")
    private Integer sortOrder;

    @Schema(description = "是否隐藏，0-显示，1-隐藏")
    private Integer visible;

    @Schema(description = "状态，0-正常，1-停用")
    private Integer status;

    @Schema(description = "子节点列表")
    private List<MenuTreeVO> children;
}
