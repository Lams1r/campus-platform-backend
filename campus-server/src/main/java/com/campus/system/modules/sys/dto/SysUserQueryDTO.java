package com.campus.system.modules.sys.dto;

import lombok.Data;

/**
 * 用户分页查询 DTO
 */
@Data
public class SysUserQueryDTO {

    /** 关键字（用户名/姓名模糊搜索） */
    private String keyword;

    /** 用户类型筛选 */
    private Integer userType;

    /** 账号状态筛选 */
    private Integer status;

    /** 院系/部门筛选 */
    private String deptName;

    /** 当前页码 */
    private Integer pageNum = 1;

    /** 每页条数 */
    private Integer pageSize = 10;
}
