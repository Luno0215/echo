package com.luno.echo.controller;

import cn.hutool.core.util.StrUtil;
import com.luno.echo.common.ErrorCode;
import com.luno.echo.common.Result;
import com.luno.echo.common.UserHolder;
import com.luno.echo.common.exception.BusinessException;
import com.luno.echo.model.dto.UserLoginRequest;
import com.luno.echo.model.dto.UserRegisterRequest;
import com.luno.echo.model.dto.UserUpdateRequest;
import com.luno.echo.model.entity.User;
import com.luno.echo.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;


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

    /**
     * 用户登录接口
     *
     * @param userLoginRequest 登录请求体
     * @return 返回 Token (String)
     */
    @PostMapping("/login")
    public Result<String> userLogin(@RequestBody UserLoginRequest userLoginRequest) {

        String userAccount = userLoginRequest.getUsername();
        String userPassword = userLoginRequest.getPassword();
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 调用 Service，拿到 token
        String token = userService.userLogin(userAccount, userPassword);

        // 返回 token 给前端
        return Result.ok(token);
    }

    /**
     * 更新个人信息 (头像/昵称)
     */
    @PostMapping("/update")
    public Result<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {

        Boolean result = userService.updateUser(userUpdateRequest);
        return Result.ok(result);
    }

    /**
     * 获取当前用户 (测试拦截器是否生效)
     */
    @GetMapping("/current")
    public Result<User> getCurrentUser() {
        // 直接从 ThreadLocal 拿，拦截器已经帮我们放进去了
        User user = UserHolder.getUser();
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        return Result.ok(user);
    }

}