package com.luno.echo.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.luno.echo.common.ErrorCode;
import com.luno.echo.common.UserHolder;
import com.luno.echo.common.exception.BusinessException;
import com.luno.echo.mapper.PostMapper;
import com.luno.echo.model.dto.CommentAddRequest;
import com.luno.echo.model.dto.CommentQueryRequest;
import com.luno.echo.model.entity.Comment;
import com.luno.echo.model.entity.User;
import com.luno.echo.model.vo.CommentVO;
import com.luno.echo.service.CommentService;
import com.luno.echo.mapper.CommentMapper;
import com.luno.echo.service.PostService;
import com.luno.echo.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.luno.echo.common.constant.RedisConstants.POST_DETAIL_KEY;

/**
* @author Luno
* @description 针对表【tb_comment(评论表)】的数据库操作Service实现
* @createDate 2026-02-16 16:25:23
*/
@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment>
    implements CommentService{

    @Resource
    private PostService postService; // 注入 PostService，方便操作帖子表

    @Resource
    private UserService userService; // 注入 UserService

    @Resource
    private CommentMapper commentMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private PostMapper postMapper;

    // 新增评论版本1 （没用Redis缓存）
    /*@Transactional(rollbackFor = Exception.class)
    @Override
    public long addComment(CommentAddRequest commentAddRequest) {
        // 1. 登录校验
        User loginUser = UserHolder.getUser();
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

        // 2. 帖子是否存在校验
        Long postId = commentAddRequest.getPostId();
        if (postId == null || postService.getById(postId) == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "帖子不存在");
        }

        // 3. 动作A：插入评论
        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setUserId(loginUser.getId());
        comment.setContent(commentAddRequest.getContent());

        boolean saveResult = this.save(comment);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "评论保存失败");
        }

        // 4. 动作B：更新帖子评论数 (+1)
        // update tb_post set comment_count = comment_count + 1 where id = ?
        boolean updateResult = postService.update()
                .setSql("comment_count = comment_count + 1")
                .eq("id", postId)
                .update();

        if (!updateResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新评论数失败");
        }

        return comment.getId();
    }*/

    @Transactional(rollbackFor = Exception.class)
    @Override
    public long addComment(CommentAddRequest commentAddRequest) {
        // 1. 登录校验
        Long userId = UserHolder.getUser().getId();

        // 2. 准备数据
        Comment comment = new Comment();
        BeanUtil.copyProperties(commentAddRequest, comment);
        comment.setUserId(userId);
        comment.setCreateTime(LocalDateTime.now());

        // 3. 插入数据库
        // 💡 重点：执行完这行，comment.getId() 就有值了！
        commentMapper.insert(comment);

        // 4. 更新帖子评论数
        postMapper.incrCommentCount(commentAddRequest.getPostId());

        // 5. 删除 Redis 缓存
        String cacheKey = POST_DETAIL_KEY + commentAddRequest.getPostId();
        stringRedisTemplate.delete(cacheKey);

        // 6. 返回生成的 ID
        return comment.getId();
    }

    @Override
    public Page<CommentVO> listCommentByPage(CommentQueryRequest commentQueryRequest) {
        long current = commentQueryRequest.getCurrent();
        long size = commentQueryRequest.getPageSize();
        Long postId = commentQueryRequest.getPostId();

        // 1. 基础查询：查评论表
        QueryWrapper<Comment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(postId != null, "post_id", postId); // 必须指定帖子ID
        queryWrapper.orderByDesc("create_time"); // 按时间倒序（最新的在上面）

        Page<Comment> commentPage = this.page(new Page<>(current, size), queryWrapper);

        // 2. 转换对象：Entity -> VO
        // 如果查不到数据，直接返回空页，防止下面报错
        if (commentPage.getRecords().isEmpty()) {
            return new Page<>(current, size, 0);
        }

        // 3. 【核心优化】收集所有发评人的 ID
        Set<Long> userIds = commentPage.getRecords().stream()
                .map(Comment::getUserId)
                .collect(Collectors.toSet());

        // 4. 【核心优化】一次性查询所有用户 (Map<UserId, User>)
        // 这里的 listByIds 是 MyBatis-Plus 提供的批量查询
        Map<Long, User> userMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        // 5. 组装 VO (填充用户信息)
        List<CommentVO> voList = commentPage.getRecords().stream().map(comment -> {
            CommentVO commentVO = new CommentVO();
            BeanUtil.copyProperties(comment, commentVO);

            // 从 Map 里拿用户，不用每次都查库
            Long userId = comment.getUserId();
            User user = userMap.get(userId);
            if (user != null) {
                commentVO.setUsername(user.getUsername());
                commentVO.setNickname(user.getNickname());
                commentVO.setAvatar(user.getAvatar());
            }
            return commentVO;
        }).collect(Collectors.toList());

        // 6. 返回 VO 分页
        Page<CommentVO> resultPage = new Page<>(commentPage.getCurrent(), commentPage.getSize(), commentPage.getTotal());
        resultPage.setRecords(voList);

        return resultPage;
    }
}




