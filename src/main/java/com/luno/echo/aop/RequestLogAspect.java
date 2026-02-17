package com.luno.echo.aop;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 统一请求日志切面
 * 功能清单：
 * 1. 自动记录接口的入参、出参、IP、耗时。
 * 2. 生成 TraceID，方便排查链路。
 * 3. 自动过滤文件流等无法序列化的对象，防止报错。
 * 4. 敏感字段 (password, token) 自动脱敏为 ******。
 * 5. 返回值过长自动截断，防止日志刷屏。
 * </p>
 */
@Aspect
@Component
@Slf4j
public class RequestLogAspect {

    /**
     * 【配置点】定义拦截规则
     * execution(* com.luno.echo.controller.*.*(..))
     * 含义：拦截 com.luno.echo.controller 包下所有类的所有方法
     */
    @Around("execution(* com.luno.echo.controller.*.*(..))")
    public Object doInterceptor(ProceedingJoinPoint point) throws Throwable {
        // 1. 【计时开始】用于统计接口耗时
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // 2. 【获取上下文】拿到当前的 HTTP 请求对象
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();

        // 3. 【生成 TraceID】全链路追踪 ID，建议在日志配置文件中也加上 [%X{requestId}]
        String requestId = UUID.randomUUID().toString();

        // 4. 【获取基础信息】URL、Method、IP
        String url = request.getRequestURI();
        String method = request.getMethod();
        String ip = getClientIp(request); // 使用自定义方法获取真实 IP

        // 5. 【参数处理】(核心逻辑：过滤 + 脱敏)
        Object[] args = point.getArgs();
        List<Object> logArgs = new ArrayList<>();

        for (Object arg : args) {
            // 🚫 过滤掉危险对象：文件流、Response、BindingResult 等
            // 这些对象转 JSON 会导致 StackOverflowError 或报错
            if (arg instanceof MultipartFile || arg instanceof HttpServletRequest
                    || arg instanceof jakarta.servlet.http.HttpServletResponse
                    || arg instanceof BindingResult) {
                continue;
            }
            logArgs.add(arg);
        }

        // 先转 JSON，再进行正则脱敏
        String paramsJson = JSONUtil.toJsonStr(logArgs);
        String safeParams = maskSensitiveData(paramsJson);

        // 📝 【打印请求日志】
        log.info("[{}] Request Start: {} {}, IP: {}, Params: {}",
                requestId, method, url, ip, safeParams);

        // 6. 【执行目标方法】(真正的业务逻辑在这里执行)
        Object result = point.proceed();

        // 7. 【计时结束】
        stopWatch.stop();
        long cost = stopWatch.getTotalTimeMillis();

        // 8. 【返回值处理】(截断 + 脱敏)
        String resultJson = JSONUtil.toJsonStr(result);

        // ✂️ 截断：如果返回了 10 万字的文章，日志只要前 1000 字用于调试即可
        if (resultJson != null && resultJson.length() > 1000) {
            resultJson = resultJson.substring(0, 1000) + "...(Too Long)";
        }

        // 🛡️ 脱敏：防止 token 等敏感信息泄露
        String safeResult = maskSensitiveData(resultJson);

        // 📝 【打印响应日志】
        log.info("[{}] Request End: Cost: {}ms, Result: {}", requestId, cost, safeResult);

        return result;
    }

    /**
     * 🛡️ 敏感数据脱敏工具
     * 作用：将 JSON 中的 password, token, secret 等字段替换为 ******
     */
    private String maskSensitiveData(String content) {
        if (StrUtil.isBlank(content)) {
            return content;
        }
        try {
            // 正则说明：匹配 "key":"value" 格式，其中 key 是敏感词
            // (password|pwd|token|secret) 是关键词列表，用 | 分隔
            String regex = "(\"(password|pwd|token|secret)\":\")([^\"]+)(\")";
            return content.replaceAll(regex, "$1******$4");
        } catch (Exception e) {
            return content; // 如果正则匹配失败，为了不影响主流程，返回原内容
        }
    }

    /**
     * 🌐 获取客户端真实 IP 地址
     * 作用：处理 Nginx 等反向代理的情况，直接 getRemoteAddr 可能拿到的是 127.0.0.1
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 对于通过多个代理的情况，第一个 IP 为客户端真实 IP，多个 IP 按照 ',' 分割
        if (ip != null && ip.length() > 15 && ip.indexOf(",") > 0) {
            ip = ip.substring(0, ip.indexOf(","));
        }
        return ip;
    }
}