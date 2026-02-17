package com.luno.echo.model.vo;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 帖子详情全集
 */
@Data
public class PostDetailVO implements Serializable {
    // --- 帖子基础信息 ---
    private Long id;
    private String content;
    private String tag;
    private LocalDateTime createTime;

    // --- 统计数据 ---
    private Integer likeCount;    // 点赞数
    private Integer commentCount; // 评论数
    private Integer viewCount;    // 浏览量 (来自 Redis)

    // --- 核心关联信息 ---
    private PostUserVO author;    // 楼主信息
    private List<PostCommentVO> commentList; // 评论列表

    // --- 🔥 当前用户交互状态 (这些字段不存 Redis，每次实时计算) ---
    private boolean isLiked;      // 我是否点赞过？
    private boolean isOwner;      // 这帖子是不是我发的？
}