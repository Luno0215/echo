package com.luno.echo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.luno.echo.model.entity.Comment;
import com.luno.echo.service.CommentService;
import com.luno.echo.mapper.CommentMapper;
import org.springframework.stereotype.Service;

/**
* @author Luno
* @description 针对表【tb_comment(评论表)】的数据库操作Service实现
* @createDate 2026-02-16 16:25:23
*/
@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment>
    implements CommentService{

}




