package com.luno.echo.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.luno.echo.common.ErrorCode;
import com.luno.echo.common.Result;
import com.luno.echo.common.exception.BusinessException;
import com.luno.echo.model.dto.PostAddRequest;
import com.luno.echo.model.dto.PostQueryRequest;
import com.luno.echo.model.vo.PostVO;
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
     * 接口：GET /post/list/page
     */
    @GetMapping("/list/page")
    public Result<Page<PostVO>> listPostByPage(PostQueryRequest postQueryRequest) {
        if (postQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 限制爬虫
        if (postQueryRequest.getPageSize() > 20) {
            postQueryRequest.setPageSize(20);
        }

        // 调用 Service 获取 VO 分页对象
        Page<PostVO> postVOPage = postService.listPostByPage(postQueryRequest);

        return Result.ok(postVOPage);
    }

    /**
     * 【新增】点赞 / 取消点赞
     * 接口：POST /post/like/{id}
     */
    @PostMapping("/like/{id}")
    public Result<String> likePost(@PathVariable("id") Long id) {
        // 1. 简单参数校验
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 2. 调用 Service (Service 里已经判断了是点赞还是取消)
        postService.likePost(id);

        return Result.ok("操作成功");
    }

}