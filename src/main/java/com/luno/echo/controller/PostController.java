package com.luno.echo.controller;

import com.luno.echo.common.ErrorCode;
import com.luno.echo.common.Result;
import com.luno.echo.common.exception.BusinessException;
import com.luno.echo.model.dto.PostAddRequest;
import com.luno.echo.service.PostService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/post")
public class PostController {

    @Resource
    private PostService postService;

    /**
     * 发布帖子
     */
    @PostMapping("/add")
    public Result<Long> addPost(@RequestBody PostAddRequest postAddRequest) {
        if (postAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long postId = postService.addPost(postAddRequest);
        return Result.ok(postId);
    }

    /**
     * 删除帖子
     * 对应需求: DELETE /post/delete/{id}
     */
    @DeleteMapping("/delete/{id}")
    public Result<String> deletePost(@PathVariable Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        postService.deletePost(id);
        return Result.ok("删除成功");
    }

}