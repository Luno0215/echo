package com.luno.echo.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 帖子视图对象
 * 作用：专门用于返回给前端展示，包含数据库里没有的字段（如 isLiked）
 */
@Data
public class PostVO implements Serializable {
    private static final long serialVersionUID = 1L;

    // --- 1. 原样拷贝 Post 的字段 ---
    private Long id;
    private Long userId;
    private String content;
    private String tag;
    private Integer likeCount;
    private Integer commentCount;
    private LocalDateTime createTime;

    // --- 2. 新增前端特有字段 ---
    
    /**
     * 当前登录用户是否点赞
     */
    private Boolean isLiked;

    // (可选) 未来扩展：发帖人的信息
    // private String userNickname;
    // private String userAvatar;
}