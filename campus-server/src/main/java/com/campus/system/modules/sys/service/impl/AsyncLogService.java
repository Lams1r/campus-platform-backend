package com.campus.system.modules.sys.service.impl;

import com.campus.system.modules.sys.entity.SysLoginLog;
import com.campus.system.modules.sys.entity.SysOperateLog;
import com.campus.system.modules.sys.service.ISysLoginLogService;
import com.campus.system.modules.sys.service.ISysOperateLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 异步日志记录服务
 * 独立为单独的 Bean，解决 @Async 自调用失效问题
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncLogService {

    private final ISysLoginLogService loginLogService;
    private final ISysOperateLogService operateLogService;

    /**
     * 异步记录登录/登出日志
     */
    @Async
    public void recordLoginLog(Long userId, String username, String message,
                               Integer loginType, Integer status, String ip, String userAgent) {
        try {
            SysLoginLog loginLog = new SysLoginLog();
            loginLog.setUserId(userId);
            loginLog.setUsername(username);
            loginLog.setMsg(message);
            loginLog.setLoginType(loginType);
            loginLog.setStatus(status);
            loginLog.setIp(ip);
            loginLog.setUserAgent(userAgent);
            loginLogService.save(loginLog);
        } catch (Exception e) {
            log.warn("登录日志记录异常: {}", e.getMessage());
        }
    }

    /**
     * 异步保存操作日志
     */
    @Async
    public void recordOperateLog(String module, String operateType,
                                 Long userId, String username,
                                 String requestMethod, String requestUrl,
                                 String requestParams, String responseResult,
                                 String ip, int status, String errorMsg, long costTime) {
        try {
            SysOperateLog operateLog = new SysOperateLog();
            operateLog.setModule(module);
            operateLog.setOperateType(operateType);
            operateLog.setOperateUserId(userId);
            operateLog.setOperateUserName(username);
            operateLog.setRequestMethod(requestMethod);
            operateLog.setRequestUrl(requestUrl);
            operateLog.setRequestParams(requestParams);
            operateLog.setResponseResult(responseResult);
            operateLog.setIp(ip);
            operateLog.setStatus(status);
            operateLog.setErrorMsg(errorMsg);
            operateLog.setCostTime(costTime);
            operateLogService.save(operateLog);
        } catch (Exception e) {
            log.warn("操作日志记录异常: {}", e.getMessage());
        }
    }
}
