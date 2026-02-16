package com.luno.echo.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.luno.echo.common.ErrorCode;
import com.luno.echo.common.UserHolder;
import com.luno.echo.common.exception.BusinessException;
import com.luno.echo.model.dto.PostAddRequest;
import com.luno.echo.model.entity.Post;
import com.luno.echo.model.entity.User;
import com.luno.echo.service.PostService;
import com.luno.echo.mapper.PostMapper;
import org.springframework.stereotype.Service;

/**
* @author Luno
* @description 针对表【tb_post(树洞帖子表)】的数据库操作Service实现
* @createDate 2026-02-16 16:25:23
*/
@Service
public class PostServiceImpl extends ServiceImpl<PostMapper, Post>
    implements PostService{

    @Override
    public long addPost(PostAddRequest postAddRequest) {
        // 1. 获取当前登录用户 (从拦截器存的 ThreadLocal 里拿)
        User loginUser = UserHolder.getUser();
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

        // 2. 校验参数
        String content = postAddRequest.getContent();
        if (StrUtil.isBlank(content)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容不能为空");
        }
        if (content.length() > 500) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容不能超过500字");
        }

        // 3. 封装 Post 对象
        Post post = new Post();
        post.setUserId(loginUser.getId()); // 关键：绑定当前用户
        post.setContent(content);
        // 如果前端没传 tag，给个默认值
        post.setTag(StrUtil.isBlank(postAddRequest.getTag()) ? "心情" : postAddRequest.getTag());
        post.setLikeCount(0);
        post.setCommentCount(0);

        // 4. 插入数据库 (createTime 会自动填充)
        boolean result = this.save(post);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统故障，发布失败");
        }

        // 5. 返回帖子 ID
        return post.getId();
    }
}




