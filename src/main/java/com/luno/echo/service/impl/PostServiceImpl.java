package com.luno.echo.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.luno.echo.common.ErrorCode;
import com.luno.echo.common.UserHolder;
import com.luno.echo.common.constant.RedisConstants;
import com.luno.echo.common.exception.BusinessException;
import com.luno.echo.model.dto.PostAddRequest;
import com.luno.echo.model.dto.PostQueryRequest;
import com.luno.echo.model.entity.Post;
import com.luno.echo.model.entity.User;
import com.luno.echo.model.vo.PostVO;
import com.luno.echo.service.PostService;
import com.luno.echo.mapper.PostMapper;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
* @author Luno
* @description 针对表【tb_post(树洞帖子表)】的数据库操作Service实现
* @createDate 2026-02-16 16:25:23
*/
@Service
public class PostServiceImpl extends ServiceImpl<PostMapper, Post>
    implements PostService{

    @Resource
    public StringRedisTemplate stringRedisTemplate;

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
    public Page<PostVO> listPostByPage(PostQueryRequest postQueryRequest) {
        long current = postQueryRequest.getCurrent();
        long size = postQueryRequest.getPageSize();
        String searchText = postQueryRequest.getSearchText();

        // 1. 构建数据库查询条件
        QueryWrapper<Post> queryWrapper = new QueryWrapper<>();
        if (StrUtil.isNotBlank(searchText)) {
            queryWrapper.like("content", searchText);
        }
        queryWrapper.orderByDesc("create_time");

        // 2. 查询数据库 (查到的是 Entity)
        Page<Post> postPage = this.page(new Page<>(current, size), queryWrapper);

        // 3. 准备 VO 分页对象 (用来装最终结果)
        Page<PostVO> postVOPage = new Page<>(postPage.getCurrent(), postPage.getSize(), postPage.getTotal());

        // 4. 获取当前登录用户 (可能为空)
        User loginUser = UserHolder.getUser();

        // 5. 【核心转换】 Entity List -> VO List
        List<PostVO> voList = postPage.getRecords().stream().map(post -> {
            // 5.1 创建 VO 并拷贝基础属性
            PostVO postVO = new PostVO();
            BeanUtil.copyProperties(post, postVO);

            // 5.2 处理 "是否点赞" 逻辑
            if (loginUser != null) {
                // 如果用户已登录，去 Redis 查 Set
                String key = RedisConstants.POST_LIKED_KEY + post.getId();
                Boolean isMember = stringRedisTemplate.opsForSet().isMember(key, loginUser.getId().toString());
                postVO.setIsLiked(Boolean.TRUE.equals(isMember));
            } else {
                // 没登录当然是 false
                postVO.setIsLiked(false);
            }

            return postVO;
        }).collect(Collectors.toList());

        // 6. 填充回 VO 分页对象
        postVOPage.setRecords(voList);

        return postVOPage;
    }

    /**
     * 点赞 / 取消点赞 (核心业务)
     */
    @Override
    public void likePost(Long postId) {
        // 1. 获取当前登录用户
        User loginUser = UserHolder.getUser();
        if (loginUser == null) {
            // 点赞操作必须登录
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        Long userId = loginUser.getId();

        // 2. 判断当前用户是否已经点赞
        // Key 格式: echo:post:like:1 (1是帖子id)
        String key = RedisConstants.POST_LIKED_KEY + postId;
        Boolean isMember = stringRedisTemplate.opsForSet().isMember(key, userId.toString());

        if (Boolean.TRUE.equals(isMember)) {
            // 3. 如果已点赞，则是【取消点赞】
            // 3.1 数据库点赞数 -1
            // SQL: update tb_post set like_count = like_count - 1 where id = ?
            boolean isSuccess = this.update()
                    .setSql("like_count = like_count - 1")
                    .eq("id", postId)
                    .update();

            // 3.2 如果DB更新成功，Redis 移除用户
            if (isSuccess) {
                stringRedisTemplate.opsForSet().remove(key, userId.toString());
            }
        } else {
            // 4. 如果未点赞，则是【点赞】
            // 4.1 数据库点赞数 +1
            // SQL: update tb_post set like_count = like_count + 1 where id = ?
            boolean isSuccess = this.update()
                    .setSql("like_count = like_count + 1")
                    .eq("id", postId)
                    .update();

            // 4.2 如果DB更新成功，Redis 添加用户
            if (isSuccess) {
                stringRedisTemplate.opsForSet().add(key, userId.toString());
            }
        }
    }
}




