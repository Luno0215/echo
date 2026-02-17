package com.luno.echo.service;

import com.luno.echo.model.dto.CommentAddRequest;
import com.luno.echo.model.entity.Comment;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.transaction.annotation.Transactional;

/**
* @author Luno
* @description 针对表【tb_comment(评论表)】的数据库操作Service
* @createDate 2026-02-16 16:25:23
*/
public interface CommentService extends IService<Comment> {

    /**
     * 添加评论
     * @param commentAddRequest 评论信息
     * @return 评论 ID
     */
    @Transactional(rollbackFor = Exception.class) // 核心：开启事务，任何异常都回滚
    long addComment(CommentAddRequest commentAddRequest);
}
