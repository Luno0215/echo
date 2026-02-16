package com.luno.echo.controller;

import com.luno.echo.common.ErrorCode;
import com.luno.echo.common.Result;
import com.luno.echo.common.exception.BusinessException;
import com.luno.echo.model.dto.UserRegisterRequest;
import com.luno.echo.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 用户接口
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 用户注册接口
     *
     * @param userRegisterRequest 注册请求体
     * @return 注册成功用户的 ID
     */
    @PostMapping("/register")
    public Result<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        // 1. Controller 层只做最基础的非空判断，避免空指针
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 2. 解析数据
        String userAccount = userRegisterRequest.getUsername();
        String userPassword = userRegisterRequest.getPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();

        // 3. 调用 Service
        // 这里不需要 try-catch！
        // 如果 Service 里抛出了 "账号已存在" 的 BusinessException，会被 GlobalExceptionHandler 自动捕获
        long result = userService.userRegister(userAccount, userPassword, checkPassword);

        // 4. 返回统一结果
        return Result.ok(result);
    }
}