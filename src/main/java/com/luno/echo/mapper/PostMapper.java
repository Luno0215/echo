package com.luno.echo.mapper;

import com.luno.echo.model.entity.Post;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
* @author Luno
* @description 针对表【tb_post(树洞帖子表)】的数据库操作Mapper
* @createDate 2026-02-16 16:25:23
* @Entity com.luno.echo.model.entity.Post
*/
public interface PostMapper extends BaseMapper<Post> {

    /**
     * 评论数 +1
     * 使用 @Update 注解直接写 SQL，比 XML 更简单
     */
    @Update("UPDATE tb_post SET comment_count = comment_count + 1 WHERE id = #{postId}")
    void incrCommentCount(@Param("postId") Long postId);
}




