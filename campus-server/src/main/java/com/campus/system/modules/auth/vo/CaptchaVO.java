package com.campus.system.modules.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 图形验证码返回结果。
 */
@Data
@AllArgsConstructor
@Schema(name = "验证码结果", description = "图形验证码的唯一标识与图片内容")
public class CaptchaVO {

    @Schema(description = "验证码标识，用于登录时回传校验")
    private String captchaKey;

    @Schema(description = "验证码图片，Base64 编码")
    private String captchaImage;
}
