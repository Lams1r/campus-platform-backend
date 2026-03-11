package com.campus.system.modules.sys.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户创建请求 DTO
 */
@Data
public class SysUserCreateDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名长度不能超过50")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度需在6-20位之间")
    private String password;

    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    /** 性别 0-未知 1-男 2-女 */
    private Integer gender;

    private String phone;
    private String email;
    private String deptName;
    private String className;

    /** 用户类型 0-学生 1-教师 2-管理员 */
    private Integer userType;

    /** 创建时同步绑定的角色ID列表 */
    private java.util.List<Long> roleIds;

    private String remark;
}
