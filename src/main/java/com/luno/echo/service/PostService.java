package com.luno.echo.service;

import com.luno.echo.model.dto.PostAddRequest;
import com.luno.echo.model.entity.Post;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author Luno
* @description 针对表【tb_post(树洞帖子表)】的数据库操作Service
* @createDate 2026-02-16 16:25:23
*/
public interface PostService extends IService<Post> {

    /**
     * 添加帖子
     * @param postAddRequest 帖子信息
     * @return 帖子 ID
     */
    long addPost(PostAddRequest postAddRequest);

    /**
     * 删除帖子
     * @param postId 帖子 ID
     * @return 是否成功
     */
    boolean deletePost(Long postId);
}
