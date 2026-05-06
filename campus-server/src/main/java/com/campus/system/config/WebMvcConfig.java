package com.campus.system.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 全局配置
 * 1. CORS 跨域配置（支持通过 campus.cors.allowed-origins 配置项限定允许的来源域名）
 * 2. 静态资源映射（文件上传目录）
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 允许的跨域来源，默认仅允许本地开发地址。
     * 生产环境请通过 application-prod.yml 或环境变量 CORS_ALLOWED_ORIGINS 配置实际域名，
     * 多个域名以逗号分隔，例如: https://campus.example.com,https://admin.example.com
     */
    @Value("${campus.cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private String allowedOrigins;

    /**
     * 全局跨域配置（前后端分离必备）
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = allowedOrigins.split(",");
        registry.addMapping("/**")
                .allowedOriginPatterns(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .exposedHeaders("Content-Disposition")
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
