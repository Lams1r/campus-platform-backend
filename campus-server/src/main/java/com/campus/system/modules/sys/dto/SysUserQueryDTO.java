package com.campus.system.modules.sys.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户分页查询 DTO
 */
@Data
@Schema(description = "用户分页查询条件")
public class SysUserQueryDTO {

    /** 关键字（用户名/姓名模糊搜索） */
    @Schema(description = "关键字")
    private String keyword;

    /** 用户类型筛选 */
    @Schema(description = "用户类型")
    private Integer userType;

    /** 账号状态筛选 */
    @Schema(description = "账号状态")
    private Integer status;

    /** 院系/部门筛选 */
    @Schema(description = "院系或部门")
    private String deptName;

    /** 当前页码 */
    @Schema(description = "当前页码")
    private Integer pageNum = 1;

    /** 每页条数 */
    @Schema(description = "每页条数")
    private Integer pageSize = 10;
}
