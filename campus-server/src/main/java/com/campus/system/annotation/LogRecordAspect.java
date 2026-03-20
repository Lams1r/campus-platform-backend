package com.campus.system.annotation;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.json.JSONUtil;
import com.campus.system.modules.sys.entity.SysUser;
import com.campus.system.modules.sys.service.ISysUserService;
import com.campus.system.modules.sys.service.impl.AsyncLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

/**
 * 操作日志 AOP 切面
 * 环绕通知：在目标方法执行前后捕获操作信息，通过 AsyncLogService 异步落盘
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LogRecordAspect {

    private final AsyncLogService asyncLogService;
    private final ISysUserService userService;

    @Around("@annotation(com.campus.system.annotation.LogRecord)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取注解信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        LogRecord logAnnotation = method.getAnnotation(LogRecord.class);

        // 获取请求信息
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attrs != null ? attrs.getRequest() : null;

        // 获取操作人信息（修正：通过查询用户表获取真实用户名，而非 loginId）
        Long userId = null;
        String username = null;
        try {
            if (StpUtil.isLogin()) {
                userId = StpUtil.getLoginIdAsLong();
                SysUser user = userService.getById(userId);
                username = (user != null) ? user.getUsername() : String.valueOf(userId);
            }
        } catch (Exception ignored) {}

        // 序列化入参（过滤不可序列化对象）
        String params = serializeArgs(joinPoint.getArgs());

        Object result = null;
        String errorMsg = null;
        int status = 0;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            status = 1;
            errorMsg = e.getMessage();
            throw e;
        } finally {
            long costTime = System.currentTimeMillis() - startTime;
            // 通过独立 Bean 异步保存日志（解决 @Async 自调用失效问题）
            try {
                asyncLogService.recordOperateLog(
                        logAnnotation.module(), logAnnotation.type(),
                        userId, username,
                        request != null ? request.getMethod() : "",
                        request != null ? request.getRequestURI() : "",
                        params,
                        result != null ? truncate(JSONUtil.toJsonStr(result), 2000) : null,
                        request != null ? getClientIp(request) : "",
                        status, truncate(errorMsg, 500), costTime);
            } catch (Exception e) {
                log.warn("操作日志记录异常: {}", e.getMessage());
            }
        }
    }

    /**
     * 序列化方法参数（跳过 Request/Response/MultipartFile 等不可序列化对象）
     */
    private String serializeArgs(Object[] args) {
        if (args == null || args.length == 0) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof HttpServletRequest
                    || args[i] instanceof HttpServletResponse
                    || args[i] instanceof MultipartFile) {
                sb.append("\"<filtered>\"");
            } else {
                try {
                    sb.append(JSONUtil.toJsonStr(args[i]));
                } catch (Exception e) {
                    sb.append("\"<unserializable>\"");
                }
            }
            if (i < args.length - 1) sb.append(",");
        }
        sb.append("]");
        return truncate(sb.toString(), 2000);
    }

    /**
     * 获取客户端真实IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return null;
        return str.length() > maxLen ? str.substring(0, maxLen) : str;
    }
}
