package com.luno.echo.interceptor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.luno.echo.common.UserHolder;
import com.luno.echo.common.constant.RedisConstants;
import com.luno.echo.model.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

public class LoginInterceptor implements HandlerInterceptor {

    // 拦截器不是 Bean，不能直接 @Resource，需要通过构造函数传入
    private StringRedisTemplate stringRedisTemplate;

    public LoginInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 获取请求头中的 token (前端通常放在 authorization 字段)
        String token = request.getHeader("authorization");
        if (StrUtil.isBlank(token)) {
            // 没有 Token，直接拦截，返回 401 状态码
            response.setStatus(401);
            return false;
        }

        // 2. 基于 token 获取 Redis 中的用户
        String key = RedisConstants.LOGIN_USER_KEY + token;
        String userJson = stringRedisTemplate.opsForValue().get(key);

        // 3. 判断 Redis 里是否有数据
        if (StrUtil.isBlank(userJson)) {
            // Token 过期或无效，拦截
            response.setStatus(401);
            return false;
        }

        // 4. 将 JSON 转为 User 对象
        User user = JSONUtil.toBean(userJson, User.class);

        // 5. 存入 ThreadLocal (UserHolder)
        UserHolder.saveUser(user);

        // 6. 【核心】刷新 Token 有效期
        // 只要用户在操作，就给他再续 30 分钟，实现“永不掉线”
        stringRedisTemplate.expire(key, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);

        // 7. 放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 8. 请求结束，移除用户，防止内存泄漏
        UserHolder.removeUser();
    }
}