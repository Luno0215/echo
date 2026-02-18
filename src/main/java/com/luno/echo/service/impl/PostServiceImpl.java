package com.luno.echo.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.luno.echo.common.ErrorCode;
import com.luno.echo.common.UserHolder;
import com.luno.echo.common.constant.RedisConstants;
import com.luno.echo.common.exception.BusinessException;
import com.luno.echo.mapper.CommentMapper;
import com.luno.echo.model.dto.PostAddRequest;
import com.luno.echo.model.dto.PostQueryRequest;
import com.luno.echo.model.entity.Comment;
import com.luno.echo.model.entity.Post;
import com.luno.echo.model.entity.User;
import com.luno.echo.model.vo.PostCommentVO;
import com.luno.echo.model.vo.PostDetailVO;
import com.luno.echo.model.vo.PostUserVO;
import com.luno.echo.model.vo.PostVO;
import com.luno.echo.service.PostService;
import com.luno.echo.mapper.PostMapper;
import com.luno.echo.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.luno.echo.common.constant.RedisConstants.*;

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

    @Resource
    private UserService userService;

    @Resource
    private CommentMapper commentMapper; // 假设你有这个 Mapper


    @Override
    public long addPost(PostAddRequest postAddRequest) {
        // 1. 获取当前登录用户 (从拦截器存的 ThreadLocal 里拿)
        User loginUser = UserHolder.getUser();
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

        // 2. 获取参数
        String content = postAddRequest.getContent();

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
                String key = POST_LIKED_KEY + post.getId();
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

    // 点赞帖子版本 1（没用定时任务）
    /*@Override
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
    }*/

    @Override
    public void likePost(Long postId) {
        // 1. 获取当前登录用户
        Long userId = UserHolder.getUser().getId();

        // 2. 定义 Redis Key
        String likeKey = POST_LIKED_KEY + postId;

        // 3. 判断用户是否点过赞 (Redis Set 操作)
        Boolean isMember = stringRedisTemplate.opsForSet().isMember(likeKey, userId.toString());

        if (Boolean.FALSE.equals(isMember)) {
            // --- 未点赞 -> 执行点赞 ---
            // A. 将用户 ID 加入 Set
            stringRedisTemplate.opsForSet().add(likeKey, userId.toString());
            // B. 点赞数 +1 (可选，如果直接用 SCARD 算总数，这里其实不用存 count)
            // stringRedisTemplate.opsForValue().increment("echo:post:like_count:" + postId);
        } else {
            // --- 已点赞 -> 执行取消点赞 ---
            // A. 将用户 ID 从 Set 移除
            stringRedisTemplate.opsForSet().remove(likeKey, userId.toString());
            // B. 点赞数 -1
            // stringRedisTemplate.opsForValue().decrement("echo:post:like_count:" + postId);
        }

        // 4. 【关键一步】将该帖子 ID 加入“脏数据集合”
        // 告诉定时任务：“喂，这个帖子的点赞数变了，等会儿记得同步到数据库！”
        stringRedisTemplate.opsForSet().add(POST_LIKE_DIRTY_KEY, postId.toString());

        // 5. 【清理缓存】
        // 因为点赞数变了，详情页的缓存(PostDetailVO)也脏了，删掉它让它重建
        stringRedisTemplate.delete(POST_DETAIL_KEY + postId);
    }

    @Override
    public PostDetailVO getPostDetail(Long id) {
        String cacheKey = POST_DETAIL_KEY + id;

        // 1. 【Redis 读取】公共数据
        String json = stringRedisTemplate.opsForValue().get(cacheKey);
        PostDetailVO vo = null;

        if (StrUtil.isNotBlank(json)) {
            vo = JSONUtil.toBean(json, PostDetailVO.class);
        } else {
            // 2. 【DB 查询】缓存未命中，开始组装
            vo = assemblePostDetail(id);
            // 3. 【Redis 写入】写入公共数据 (过期时间 30 分钟)
            stringRedisTemplate.opsForValue().set(cacheKey, JSONUtil.toJsonStr(vo), 30, TimeUnit.MINUTES);
        }

        // 【强制修正】去 Redis 查实时的点赞数，防止刚取消完点赞导致点赞数不一致
        String likeKey = POST_LIKED_KEY + id;

        // 获取 Redis 里的真实个数 (比如你取消了点赞，这里现在就是 8)
        Long realLikeCount = stringRedisTemplate.opsForSet().size(likeKey);

        // 只要 Redis 里有数，就以 Redis 为准！扔掉数据库那个旧的 9
        if (realLikeCount != null && realLikeCount > 0) {
            vo.setLikeCount(realLikeCount.intValue());
        }

        // 4. 【浏览量】Redis 原子自增 (独立于 VO 缓存)
        // 使用 String 结构单独存浏览量，避免每次改浏览量都要重写整个大 JSON
        Long viewCount = stringRedisTemplate.opsForValue().increment(POST_VIEW_KEY + id);
        vo.setViewCount(viewCount.intValue());

        // 5. 【个性化填充】这一步最关键！不能缓存！
        // 获取当前登录用户
        User loginUser = UserHolder.getUser();
        if (loginUser != null) {
            // 5.1 判断是不是楼主
            vo.setOwner(loginUser.getId().equals(vo.getAuthor().getId()));

            // 5.2 判断是否点过赞 (去查 Redis 的 Set 结构: echo:post:like:1)
            Boolean isLiked = stringRedisTemplate.opsForSet().isMember(likeKey, loginUser.getId().toString());
            vo.setLiked(Boolean.TRUE.equals(isLiked));

            // 5.3 判断评论列表中，哪些是自己发的 (可选)
            if (CollUtil.isNotEmpty(vo.getCommentList())) {
                for (PostCommentVO commentVO : vo.getCommentList()) {
                    commentVO.setOwner(loginUser.getId().equals(commentVO.getCommenter().getId()));
                }
            }
        }

        return vo;
    }

    /**
     * 🕵️‍♂️ 从数据库组装完整的 VO (只有缓存失效才走这里)
     */
    private PostDetailVO assemblePostDetail(Long postId) {
        // A. 查帖子
        Post post = this.getById(postId);
        if (post == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);

        PostDetailVO vo = new PostDetailVO();
        BeanUtil.copyProperties(post, vo);

        // B. 查楼主信息
        User author = userService.getById(post.getUserId());
        PostUserVO authorVO = new PostUserVO();
        if (author != null) {
            authorVO.setId(author.getId());
            authorVO.setNickname(author.getNickname()); // 用 nickname 而不是 username
            authorVO.setAvatar(author.getAvatar());
        }
        vo.setAuthor(authorVO);

        // C. 查评论列表 (一次性查出所有评论)
        List<Comment> comments = commentMapper.selectList(
                new QueryWrapper<Comment>().eq("post_id", postId).orderByDesc("create_time")
        );

        // D. 组装评论 VO (包含评论者信息)
        List<PostCommentVO> commentVOList = new ArrayList<>();
        if (CollUtil.isNotEmpty(comments)) {
            // D-1. 提取所有评论者的 ID (避免 N+1 查询)
            Set<Long> userIds = comments.stream().map(Comment::getUserId).collect(Collectors.toSet());

            // D-2. 批量查出所有用户
            List<User> users = userService.listByIds(userIds);
            Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));

            // D-3. 转换
            for (Comment c : comments) {
                PostCommentVO cVO = new PostCommentVO();
                cVO.setId(c.getId());
                cVO.setContent(c.getContent());
                cVO.setCreateTime(c.getCreateTime());

                // 填充评论者
                User u = userMap.get(c.getUserId());
                if (u != null) {
                    PostUserVO uVO = new PostUserVO();
                    uVO.setId(u.getId());
                    uVO.setNickname(u.getNickname());
                    uVO.setAvatar(u.getAvatar());
                    cVO.setCommenter(uVO);
                }
                commentVOList.add(cVO);
            }
        }
        vo.setCommentList(commentVOList);

        return vo;
    }
}




