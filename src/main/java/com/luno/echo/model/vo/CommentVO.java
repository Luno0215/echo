package com.luno.echo.model.vo;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class CommentVO implements Serializable {
    private Long id;
    private Long postId; // 属于哪个帖子
    private Long userId; // 谁发的
    private String content; // 内容
    private LocalDateTime createTime;
    
    // --- 扩展字段 ---
    // private String userNickname; // 发评论人的昵称 (暂时先不填，后续教你关联查询)
    // private String userAvatar;   // 发评论人的头像
}