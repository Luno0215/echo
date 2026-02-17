package com.luno.echo.model.vo;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class CommentVO implements Serializable {
    private Long id;
    private Long postId;
    private Long userId;
    private String content;
    private LocalDateTime createTime;
    
    // --- 关联用户信息 (由 Service 层填充) ---
    private String username;  // 用户名
    private String nickname;  // 昵称
    private String avatar;    // 头像
    
    // (可选) isLiked; // 如果你想做评论点赞，也可以加这个
}