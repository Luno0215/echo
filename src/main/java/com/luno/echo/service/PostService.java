package com.luno.echo.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.luno.echo.model.dto.PostAddRequest;
import com.luno.echo.model.dto.PostQueryRequest;
import com.luno.echo.model.entity.Post;
import com.baomidou.mybatisplus.extension.service.IService;
import com.luno.echo.model.vo.PostDetailVO;
import com.luno.echo.model.vo.PostVO;

import java.util.List;

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

    /**
     * 分页获取帖子列表（返回 VO）
     */
    Page<PostVO> listPostByPage(PostQueryRequest postQueryRequest);

    /**
     * 点赞帖子
     * @param postId 帖子 ID
     */
    void likePost(Long postId);

    /**
     * 获取帖子详情
     * @param id
     * @return 帖子详情VO
     */
    PostDetailVO getPostDetail(Long id);

    /**
     * 从 MySQL 全量同步数据到 ES
     * @return 同步成功的条数
     */
    int syncAllToEs();

    /**
     * 获取热搜列表
     * @return 热门搜索
     */
    List<String> listHotSearch();
}
