package com.luno.echo.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.luno.echo.common.ErrorCode;
import com.luno.echo.common.exception.BusinessException;
import com.luno.echo.model.entity.User;
import com.luno.echo.service.UserService;
import com.luno.echo.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
* @author Luno
* @description 针对表【tb_user(用户表)】的数据库操作Service实现
* @createDate 2026-02-16 16:25:23
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{


    /**
     * 盐值，用于混淆密码 (随便写，越复杂越好)
     */
    private static final String SALT = "luno_code_is_good";

    @Override
    public long userRegister(String username, String password, String checkPassword) {
        // 1. 校验 (Guard Logic)
        // 1.1 非空校验
        if (StrUtil.hasBlank(username, password, checkPassword)) {
            // 使用自定义异常，抛出 "参数错误"
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }

        // 1.2 长度校验 (账号不小于4位，密码不小于6位)
        if (username.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号小于4位");
        }
        if (password.length() < 6 || checkPassword.length() < 6) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码小于6位");
        }

        // 1.3 密码和校验密码是否相同
        if (!password.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }

        // 1.4 校验账户是否包含特殊字符 (只允许数字、字母、下划线)
        // 使用正则校验
        String validPattern = "[a-zA-Z0-9_]+";
        Matcher matcher = Pattern.compile(validPattern).matcher(username);
        if (!matcher.find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账户包含特殊字符,只允许数字、字母、下划线");
        }

        // 2. 查重 (关键逻辑：去数据库查)
        // 使用 Lambda 的链式调用
        // 语义：查询 (lambdaQuery) -> 账号等于 userAccount (eq) -> 统计数量 (count)
        long count = this.lambdaQuery()
                .eq(User::getUsername, username)
                .count();
        if (count > 0) {
            // 这是一个典型的业务错误，用 40000 类的错误码
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号已存在");
        }

        // 3. 加密 (千万不能存明文密码！)
        // 使用 Hutool 的 MD5 工具，加盐加密
        String encryptPassword = DigestUtil.md5Hex(SALT + username);

        // 4. 插入数据 (这里字段较多，不用链式)
        User user = new User();
        user.setUsername(username);
        user.setPassword(encryptPassword);
        // 设置默认昵称
        user.setNickname("用户_" + username);

        // save 方法是 MyBatis-Plus 提供的，返回 boolean
        boolean saveResult = save(user);
        if (!saveResult) {
            // 如果数据库报错（比如挂了），抛出系统异常 50000
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
        }

        // 5. 返回新用户 ID
        return user.getId();
    }
}




