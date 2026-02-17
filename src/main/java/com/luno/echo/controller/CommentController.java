package com.luno.echo.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.luno.echo.common.ErrorCode;
import com.luno.echo.common.Result;
import com.luno.echo.common.exception.BusinessException;
import com.luno.echo.model.dto.CommentAddRequest;
import com.luno.echo.model.dto.CommentQueryRequest;
import com.luno.echo.model.vo.CommentVO;
import com.luno.echo.service.CommentService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/comment")
public class CommentController {

    @Resource
    private CommentService commentService;

    /**
     * 发布评论
     * POST /comment/add
     */
    @PostMapping("/add")
    public Result<Long> addComment(@RequestBody CommentAddRequest commentAddRequest) {
        if (commentAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long commentId = commentService.addComment(commentAddRequest);
        return Result.ok(commentId);
    }

    /**
     * 分页获取评论列表
     * GET /comment/list/page?postId=1&current=1
     */
    @GetMapping("/list/page")
    public Result<Page<CommentVO>> listCommentByPage(CommentQueryRequest commentQueryRequest) {
        if (commentQueryRequest == null || commentQueryRequest.getPostId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "必须指定帖子ID");
        }

        // 限制爬虫
        if (commentQueryRequest.getPageSize() > 20) {
            commentQueryRequest.setPageSize(20);
        }

        Page<CommentVO> commentVOPage = commentService.listCommentByPage(commentQueryRequest);
        return Result.ok(commentVOPage);
    }
}