package com.campus.system.common.constants;

/**
 * 系统级通用常量
 */
public class SystemConstants {

    /** 默认分页页码 */
    public static final int DEFAULT_PAGE_NUM = 1;
    /** 默认分页大小 */
    public static final int DEFAULT_PAGE_SIZE = 10;

    /** 逻辑删除标记 - 正常 */
    public static final int NOT_DELETED = 0;
    /** 逻辑删除标记 - 已删除 */
    public static final int DELETED = 1;

    /** 缓存前缀 */
    public static final String CACHE_PREFIX = "campus:";
    /** 验证码缓存前缀 */
    public static final String CAPTCHA_PREFIX = CACHE_PREFIX + "captcha:";
    /** 登录失败计数器前缀 */
    public static final String LOGIN_FAIL_PREFIX = CACHE_PREFIX + "login_fail:";

    /** 文件上传最大体积 (10MB) */
    public static final long MAX_UPLOAD_SIZE = 10 * 1024 * 1024;

    /** 账号锁定阈值（连续失败次数） */
    public static final int LOGIN_FAIL_LOCK_COUNT = 3;
    /** 账号锁定时长（小时） */
    public static final int LOGIN_LOCK_HOURS = 1;
}
