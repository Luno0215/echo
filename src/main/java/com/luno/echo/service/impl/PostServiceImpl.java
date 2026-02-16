package com.luno.echo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.luno.echo.model.Post;
import com.luno.echo.service.PostService;
import com.luno.echo.mapper.PostMapper;
import org.springframework.stereotype.Service;

/**
* @author Luno
* @description 针对表【tb_post(树洞帖子表)】的数据库操作Service实现
* @createDate 2026-02-16 16:25:23
*/
@Service
public class PostServiceImpl extends ServiceImpl<PostMapper, Post>
    implements PostService{

}




