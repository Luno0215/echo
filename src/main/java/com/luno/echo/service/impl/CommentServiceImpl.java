package com.luno.echo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.luno.echo.common.ErrorCode;
import com.luno.echo.common.UserHolder;
import com.luno.echo.common.exception.BusinessException;
import com.luno.echo.model.dto.CommentAddRequest;
import com.luno.echo.model.entity.Comment;
import com.luno.echo.model.entity.User;
import com.luno.echo.service.CommentService;
import com.luno.echo.mapper.CommentMapper;
import com.luno.echo.service.PostService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
* @author Luno
* @description 针对表【tb_comment(评论表)】的数据库操作Service实现
* @createDate 2026-02-16 16:25:23
*/
@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment>
    implements CommentService{

    @Resource
    private PostService postService; // 注入 PostService，方便操作帖子表

    @Transactional(rollbackFor = Exception.class)
    @Override
    public long addComment(CommentAddRequest commentAddRequest) {
        // 1. 登录校验
        User loginUser = UserHolder.getUser();
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

        // 2. 帖子是否存在校验
        Long postId = commentAddRequest.getPostId();
        if (postId == null || postService.getById(postId) == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "帖子不存在");
        }

        // 3. 动作A：插入评论
        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setUserId(loginUser.getId());
        comment.setContent(commentAddRequest.getContent());

        boolean saveResult = this.save(comment);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "评论保存失败");
        }

        // 4. 动作B：更新帖子评论数 (+1)
        // update tb_post set comment_count = comment_count + 1 where id = ?
        boolean updateResult = postService.update()
                .setSql("comment_count = comment_count + 1")
                .eq("id", postId)
                .update();

        if (!updateResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新评论数失败");
        }

        return comment.getId();
    }
}




