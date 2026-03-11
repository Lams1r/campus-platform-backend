package com.campus.system.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 全局路由拦截器配置
 * 白名单放行 + 全局 Token 校验
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Sa-Token 拦截器
        registry.addInterceptor(new SaInterceptor(handle -> {
            // 拦截所有路由
            SaRouter.match("/**")
                    // 白名单放行
                    .notMatch(
                            "/auth/login",
                            "/auth/captcha",
                            "/auth/logout",
                            "/doc.html",
                            "/webjars/**",
                            "/swagger-resources/**",
                            "/v3/api-docs/**",
                            "/favicon.ico",
                            "/error"
                    )
                    .check(r -> StpUtil.checkLogin());
        })).addPathPatterns("/**");
    }
}
