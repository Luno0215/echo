package com.luno.echo.model.vo;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 帖子视图对象 (返回给前端的最终数据)
 */
@Data
public class PostVO implements Serializable {
    private static final long serialVersionUID = 1L;

    // --- 1. 帖子基础信息 ---
    private Long id;
    private Long userId;
    private String content;
    private String tag;
    private Integer likeCount;
    private Integer commentCount;
    private LocalDateTime createTime;

    // --- 2. 核心用户信息 (本次新增 🔥) ---
    /**
     * 作者昵称
     */
    private String username;

    /**
     * 作者头像
     */
    private String userAvatar;

    // --- 3. 交互状态 (个性化字段) ---
    /**
     * 当前登录用户是否点赞
     */
    private Boolean isLiked;

    /**
     * 当前登录用户是否是楼主 (用于控制显示"删除"按钮)
     */
    private Boolean isOwner;
}