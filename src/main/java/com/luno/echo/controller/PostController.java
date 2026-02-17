package com.luno.echo.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.luno.echo.common.ErrorCode;
import com.luno.echo.common.Result;
import com.luno.echo.common.exception.BusinessException;
import com.luno.echo.model.dto.PostAddRequest;
import com.luno.echo.model.dto.PostQueryRequest;
import com.luno.echo.model.entity.Post;
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

    /**
     * 分页获取帖子列表
     * GET /post/list/page?current=1&pageSize=10&searchText=哈哈
     */
    @GetMapping("/list/page")
    public Result<Page<Post>> listPostByPage(PostQueryRequest postQueryRequest) {
        if (postQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 限制爬虫：如果一次性要把 100 条以上，强制改成 20 条
        if (postQueryRequest.getPageSize() > 20) {
            postQueryRequest.setPageSize(20);
        }

        Page<Post> postPage = postService.listPostByPage(postQueryRequest);
        return Result.ok(postPage);
    }

}