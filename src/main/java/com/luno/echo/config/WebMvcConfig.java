package com.luno.echo.config;

import com.luno.echo.interceptor.LoginInterceptor;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册拦截器，并把 RedisTemplate 传进去
        registry.addInterceptor(new LoginInterceptor(stringRedisTemplate))
                .addPathPatterns("/**") // 拦截所有请求
                .excludePathPatterns(   // 放行白名单
                        "/user/login",
                        "/user/register",
                        "/doc.html",    // Knife4j 接口文档
                        "/webjars/**",
                        "/v3/api-docs/**",
            			"/favicon.ico"
                );
    }
}