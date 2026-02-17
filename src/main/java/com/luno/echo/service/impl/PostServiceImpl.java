package com.luno.echo.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.luno.echo.common.ErrorCode;
import com.luno.echo.common.UserHolder;
import com.luno.echo.common.exception.BusinessException;
import com.luno.echo.model.dto.PostAddRequest;
import com.luno.echo.model.dto.PostQueryRequest;
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

    @Override
    public boolean deletePost(Long postId) {
        // 1. 获取当前用户
        User loginUser = UserHolder.getUser();
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

        // 2. 查询帖子是否存在
        Post post = this.getById(postId);
        if (post == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "帖子不存在");
        }

        // 3. 【核心权限校验】只能删除自己的帖子
        // 注意：Long 类型比较要用 equals，不能用 ==
        if (!post.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH, "你无权删除他人的树洞");
        }

        // 4. 执行删除
        return this.removeById(postId);
    }

    @Override
    public Page<Post> listPostByPage(PostQueryRequest postQueryRequest) {
        long current = postQueryRequest.getCurrent();
        long size = postQueryRequest.getPageSize();
        String searchText = postQueryRequest.getSearchText();

        // 1. 构建查询条件
        QueryWrapper<Post> queryWrapper = new QueryWrapper<>();

        // 1.1 如果有搜索词，就查 content 包含该词
        if (StrUtil.isNotBlank(searchText)) {
            queryWrapper.like("content", searchText);
        }

        // 1.2 按创建时间倒序 (新的在上面)
        queryWrapper.orderByDesc("create_time");

        // 1.3 排除已逻辑删除的 (MP配置了 TableLogic 会自动处理，这里可以不写，但为了保险)
        // queryWrapper.eq("is_delete", 0);

        // 2. 执行分页查询
        Page<Post> postPage = this.page(new Page<>(current, size), queryWrapper);

        return postPage;
    }
}




