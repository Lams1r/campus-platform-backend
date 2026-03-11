package com.campus.system.modules.sys.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用户更新请求 DTO
 */
@Data
public class SysUserUpdateDTO {

    @NotNull(message = "用户ID不能为空")
    private Long id;

    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    private Integer gender;
    private String phone;
    private String email;
    private String deptName;
    private String className;
    private Integer userType;
    private String remark;

    /** 更新时同步绑定的角色ID列表 */
    private java.util.List<Long> roleIds;
}
