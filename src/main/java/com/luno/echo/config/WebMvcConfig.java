package com.luno.echo.config;

import com.luno.echo.interceptor.LoginInterceptor;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 拦截器配置 （登录拦截器）
     */
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

    /**
     *  跨域配置 (CORS)
     * 作用：允许前端 (Vue/React) 访问后端接口
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 覆盖所有请求路径
        registry.addMapping("/**")
                // 允许发送 Cookie (非常重要！否则前端登录后拿不到 Session/Token)
                .allowCredentials(true)
                // 允许所有的请求域名 (生产环境建议换成具体的域名，如 http://localhost:5173)
                .allowedOriginPatterns("*")
                // 允许的方法 (GET, POST, etc)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                // 允许所有的请求头
                .allowedHeaders("*")
                // 跨域允许时间
                .maxAge(3600);
    }
}