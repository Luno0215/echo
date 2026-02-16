package com.luno.echo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.luno.echo.model.entity.User;
import com.luno.echo.service.UserService;
import com.luno.echo.mapper.UserMapper;
import org.springframework.stereotype.Service;

/**
* @author Luno
* @description 针对表【tb_user(用户表)】的数据库操作Service实现
* @createDate 2026-02-16 16:25:23
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

}




