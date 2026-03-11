package com.campus.system.config;

import com.alicp.jetcache.anno.config.EnableCreateCacheAnnotation;
import com.alicp.jetcache.anno.config.EnableMethodCache;
import org.springframework.context.annotation.Configuration;

/**
 * JetCache 多级缓存配置启用
 * 开启方法级缓存注解 (@Cached, @CacheUpdate, @CacheInvalidate)
 * 开启 @CreateCache 手动缓存注解
 */
@Configuration
@EnableMethodCache(basePackages = "com.campus.system")
@EnableCreateCacheAnnotation
public class JetCacheConfig {
    // JetCache 的具体连接参数已在 application.yml 中配置
    // 此类仅负责开启注解扫描
}
