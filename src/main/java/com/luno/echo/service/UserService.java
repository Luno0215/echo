package com.luno.echo.service;

import com.luno.echo.model.dto.UserUpdateRequest;
import com.luno.echo.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author Luno
* @description 针对表【tb_user(用户表)】的数据库操作Service
* @createDate 2026-02-16 16:25:23
*/
public interface UserService extends IService<User> {
    /**
     * 用户注册
     *
     * @param username      用户名
     * @param password      用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id (返回 ID 比返回 boolean 更实用，前端可能需要拿着 ID 跳转)
     */
    long userRegister(String username, String password, String checkPassword);

    /**
     * 用户注册
     *
     * @param username      用户名
     * @param password      用户密码
     * @return token        登录凭证
     */
    String userLogin(String username, String password);


    Boolean updateUser(UserUpdateRequest updateUser);
}
