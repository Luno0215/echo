package com.luno.echo.aop;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * 统一请求日志切面
 */
@Aspect
@Component
@Slf4j
public class RequestLogAspect {

    /**
     * 拦截 Controller 包下的所有方法
     */
    @Around("execution(* com.luno.echo.controller.*.*(..))")
    public Object doInterceptor(ProceedingJoinPoint point) throws Throwable {
        // 1. 开始计时
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // 2. 获取请求路径
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        
        // 生成一个唯一的请求 ID，方便排查链路
        String requestId = UUID.randomUUID().toString();
        String url = request.getRequestURI();

        // 3. 获取请求参数
        Object[] args = point.getArgs();
        String reqParam = "[" + StrUtil.join(",", args) + "]";

        // 4. 输出【请求日志】
        log.info("Request Start  id: {}, path: {}, ip: {}, params: {}", 
                 requestId, url, request.getRemoteHost(), reqParam);

        // 5. 执行原方法 (最关键的一步！)
        Object result = point.proceed();

        // 6. 停止计时
        stopWatch.stop();
        long totalTimeMillis = stopWatch.getTotalTimeMillis();

        // 7. 输出【响应日志】
        log.info("Request End    id: {}, cost: {}ms", requestId, totalTimeMillis);

        return result;
    }
}