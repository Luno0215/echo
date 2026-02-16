package com.luno.echo.common;


import com.luno.echo.model.entity.User;

/**
 * 用户上下文持有者
 * 作用：利用 ThreadLocal 在当前线程中共享用户信息
 */
public class UserHolder {

    // 核心：ThreadLocal 就像每个线程的“私有口袋”
    private static final ThreadLocal<User> tl = new ThreadLocal<>();

    // 存用户
    public static void saveUser(User user){
        tl.set(user);
    }

    // 取用户
    public static User getUser(){
        return tl.get();
    }

    // 删用户 (非常重要！防止内存泄漏)
    public static void removeUser(){
        tl.remove();
    }
}