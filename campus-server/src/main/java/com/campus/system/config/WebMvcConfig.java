package com.campus.system.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 全局配置
 * 1. CORS 跨域配置
 * 2. 静态资源映射（文件上传目录）
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 全局跨域配置（前后端分离必备）
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 静态资源映射：将上传文件目录映射为可访问路径
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 文件上传目录映射 —— 后期可通过 application.yml 配置路径
        registry.addResourceHandler("/upload/**")
                .addResourceLocations("file:./upload-path/");
    }
}
