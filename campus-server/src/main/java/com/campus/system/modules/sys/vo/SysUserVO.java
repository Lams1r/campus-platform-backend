package com.campus.system.modules.sys.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户详情视图对象。
 */
@Data
@Schema(name = "用户信息", description = "用户查询结果，不包含密码")
public class SysUserVO {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "头像地址")
    private String avatar;

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

    @Schema(description = "账号状态，0-正常，1-停用，2-锁定")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "角色名称列表")
    private List<String> roleNames;
}
