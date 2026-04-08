package com.campus.system.modules.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录成功后的返回信息。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "登录结果", description = "登录成功后的用户信息与令牌")
public class LoginVO {

    @Schema(description = "访问令牌")
    private String token;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "用户类型，0-学生，1-教师，2-管理员")
    private Integer userType;

    @Schema(description = "头像地址")
    private String avatar;
}
