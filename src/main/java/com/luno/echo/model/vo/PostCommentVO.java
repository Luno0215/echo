package com.luno.echo.model.vo;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评论展示对象
 * 展示单条评论，包含评论内容 + 评论者信息 + 是否是本人评论
 */
@Data
public class PostCommentVO implements Serializable {
    private Long id;
    private String content;           // 评论内容
    private LocalDateTime createTime; // 评论时间
    
    // 💡 嵌套对象：评论人信息
    private PostUserVO commenter;     
    
    // 💡 权限标识：是不是当前登录用户发的评论？(前端用来判断是否显示删除按钮)
    private boolean isOwner;          
}