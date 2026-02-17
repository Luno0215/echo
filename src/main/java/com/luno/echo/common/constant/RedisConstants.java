package com.luno.echo.common.constant;

public class RedisConstants {
    /**
     * 登录 Token 的 Key 前缀
     * 例子：login:token:5a2b3c...
     */
    public static final String LOGIN_USER_KEY = "login:token:";

    /**
     * Token 有效期：30 分钟
     * 只要用户在 30 分钟内有操作，就会自动刷新
     */
    public static final Long LOGIN_USER_TTL = 30L;

    /**
     * 帖子点赞的 Key 前缀
     * 例子：echo:post:like:5a2b3c...
     */
    public static final String POST_LIKED_KEY = "echo:post:like:";
}