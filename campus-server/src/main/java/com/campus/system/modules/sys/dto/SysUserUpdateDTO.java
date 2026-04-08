package com.campus.system.modules.sys.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 更新用户请求。
 */
@Data
@Schema(name = "用户更新请求", description = "后台修改用户信息时提交的参数")
public class SysUserUpdateDTO {

    @NotNull(message = "用户ID不能为空")
    @Schema(description = "用户ID")
    private Long id;

    @NotBlank(message = "真实姓名不能为空")
    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "性别，0-未知，1-男，2-女")
    private Integer gender;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "院系或部门")
    private String deptName;

    @Schema(description = "班级名称")
    private String className;

    @Schema(description = "用户类型，0-学生，1-教师，2-管理员")
    private Integer userType;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "角色ID列表")
    private List<Long> roleIds;
}
