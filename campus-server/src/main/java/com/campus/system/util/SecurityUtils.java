package com.campus.system.util;

import cn.dev33.satoken.stp.StpUtil;

/**
 * 当前登录用户信息工具类
 * 基于 Sa-Token 封装，避免各处直接调用 StpUtil
 */
public class SecurityUtils {

    /**
     * 获取当前登录用户ID
     */
    public static Long getCurrentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    /**
     * 获取当前登录用户的 Token
     */
    public static String getCurrentToken() {
        return StpUtil.getTokenValue();
    }

    /**
     * 判断当前是否已登录
     */
    public static boolean isLogin() {
        return StpUtil.isLogin();
    }

    /**
     * 判断当前用户是否具有某个角色
     */
    public static boolean hasRole(String role) {
        return StpUtil.hasRole(role);
    }

    /**
     * 判断当前用户是否具有某个权限
     */
    public static boolean hasPermission(String permission) {
        return StpUtil.hasPermission(permission);
    }
}
