package com.luno.echo.model.vo;

import lombok.Data;
import java.io.Serializable;


/**
 * 通用用户信息组件
 * 无论是帖子作者，还是评论者，都用这个展示。只暴露昵称和头像
 */
@Data
public class PostUserVO implements Serializable {
    private Long id;        // 用户ID
    private String nickname;// 昵称 (显示名)
    private String avatar;  // 头像
}