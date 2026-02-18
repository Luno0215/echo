package com.luno.echo.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.luno.echo.common.ErrorCode;
import com.luno.echo.common.UserHolder;
import com.luno.echo.common.exception.BusinessException;
import com.luno.echo.mapper.CommentMapper;
import com.luno.echo.model.dto.PostAddRequest;
import com.luno.echo.model.dto.PostQueryRequest;
import com.luno.echo.model.entity.Comment;
import com.luno.echo.model.entity.Post;
import com.luno.echo.model.entity.User;
import com.luno.echo.model.es.PostEsDTO;
import com.luno.echo.model.es.repository.PostEsRepository;
import com.luno.echo.model.vo.PostCommentVO;
import com.luno.echo.model.vo.PostDetailVO;
import com.luno.echo.model.vo.PostUserVO;
import com.luno.echo.model.vo.PostVO;
import com.luno.echo.service.PostService;
import com.luno.echo.mapper.PostMapper;
import com.luno.echo.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightFieldParameters;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
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

    @Resource
    private PostEsRepository postEsRepository;

    // 注入 ES 模板
    @Resource
    private ElasticsearchOperations elasticsearchOperations;

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

    // 分页查询帖子普通版
    /*@Override
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
    }*/

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

    /**
     * 🔍 复合搜索核心方法 (ES + MySQL 双剑合璧)
     * 流程：
     * 1. 在 ES 中根据 关键词(高亮) 和 标签(过滤) 搜索，拿到 ID 列表。
     * 2. 根据 ID 去 MySQL 查询完整的帖子数据。
     * 3. 将 ES 返回的高亮文本，覆盖到 MySQL 的普通文本上。
     */
    private Page<PostVO> searchByEs(PostQueryRequest postQueryRequest) {
        // 获取请求参数
        long current = postQueryRequest.getCurrent();
        long size = postQueryRequest.getPageSize();
        String searchText = postQueryRequest.getSearchText();
        String tag = postQueryRequest.getTag();

        // ============================================================
        // 第一阶段：准备高亮配置 (告诉 ES 怎么给匹配词上色)
        // ============================================================

        // 1. 构建高亮参数 (样式定义)
        HighlightFieldParameters fieldParam = HighlightFieldParameters.builder()
                .withPreTags("<span style='color:red'>") // 高亮前缀：红字开始
                .withPostTags("</span>")                 // 高亮后缀：标签闭合
                // requireFieldMatch(false) 的意思是：
                // 哪怕我搜的是 content 字段匹配到了，如果 tag 字段里也有这个词，tag 也要高亮。
                // (如果不设置 false，通常只有参与匹配的那个字段才会被高亮)
                .withRequireFieldMatch(false)
                .build();

        // 2. 定义哪些字段需要高亮
        HighlightField contentField = new HighlightField("content", fieldParam);
        HighlightField tagField = new HighlightField("tag", fieldParam);

        // 3. 封装成 Highlight 对象 (Spring Data 的包装类)
        Highlight highlight = new Highlight(Arrays.asList(contentField, tagField));

        // ============================================================
        // 第二阶段：构建查询语句 (NativeQuery 是 Spring Boot 3 的核心构建器)
        // ============================================================

        NativeQuery query = NativeQuery.builder()
                // .withQuery 定义核心查询逻辑 (使用 lambda 表达式构建 bool 查询)
                .withQuery(q -> q.bool(b -> {

                    // A. 处理搜索词 (全文检索，计算相关度分数)
                    // 逻辑：如果传了 searchText，则必须 (must) 满足：内容包含 OR 标签包含
                    if (StrUtil.isNotBlank(searchText)) {
                        b.must(m -> m.bool(sub -> sub
                                // should 相当于 SQL 中的 OR
                                // 只要 content 或 tag 其中一个字段包含 searchText 即可
                                .should(s -> s.match(ma -> ma.field("content").query(searchText)))
                                .should(s -> s.match(ma -> ma.field("tag").query(searchText)))
                        ));
                    }

                    // B. 处理标签过滤 (精确匹配，不计算分数)
                    // 逻辑：如果传了 tag，则必须过滤 (filter) 出该标签的数据
                    // 💡 知识点：filter 比 must 性能更好，因为它不涉及评分算法，且结果会被缓存。
                    if (StrUtil.isNotBlank(tag)) {
                        b.filter(f -> f.term(t -> t.field("tag").value(tag)));
                    }

                    return b;
                }))
                // 设置分页 (注意：ES 页码从 0 开始，而前端传的 current 通常从 1 开始，所以要减 1)
                .withPageable(PageRequest.of((int) (current - 1), (int) size))
                // 注入刚才定义的高亮配置
                .withHighlightQuery(new HighlightQuery(highlight, PostEsDTO.class))
                .build();

        // ============================================================
        // 第三阶段：执行搜索 & 解析结果 (ES -> ID List)
        // ============================================================

        // 4. 发送请求给 ES
        SearchHits<PostEsDTO> searchHits = elasticsearchOperations.search(query, PostEsDTO.class);

        // 5. 如果没查到数据，直接返回空页，避免后续空指针或无效查询
        if (!searchHits.hasSearchHits()) {
            return new Page<>(current, size, 0);
        }

        // 6. 准备容器
        List<Long> postIds = new ArrayList<>(); // 存 ID，用于回表查 MySQL
        Map<Long, String> contentHighlightMap = new HashMap<>(); // 存内容的高亮片段
        Map<Long, String> tagHighlightMap = new HashMap<>();     // 存标签的高亮片段

        // 7. 遍历 ES 返回的每一个“命中”(Hit)对象
        for (SearchHit<PostEsDTO> hit : searchHits) {
            Long id = hit.getContent().getId();
            postIds.add(id);

            // 提取 content 字段的高亮 (结果是一个 List，通常取第 0 个片段即可)
            List<String> contentHighlights = hit.getHighlightField("content");
            if (CollUtil.isNotEmpty(contentHighlights)) {
                // 放入 Map，Key 是帖子 ID，Value 是带 <span...> 的高亮文本
                contentHighlightMap.put(id, contentHighlights.get(0));
            }

            // 提取 tag 字段的高亮
            List<String> tagHighlights = hit.getHighlightField("tag");
            if (CollUtil.isNotEmpty(tagHighlights)) {
                tagHighlightMap.put(id, tagHighlights.get(0));
            }
        }

        // ============================================================
        // 第四阶段：回表查询 & 组装最终结果 (MySQL + Redis + ES Merge)
        // ============================================================

        // 8. 根据 ID 列表去 MySQL 查询完整数据
        // 为什么要回表？因为 ES 为了性能通常只存索引字段，最新的头像、昵称、实时点赞数最好查 DB/Redis
        List<Post> postList = this.listByIds(postIds);

        // 防御性判断：万一 ES 有 ID，但 MySQL 删了，这里要判空
        if (CollUtil.isEmpty(postList)) {
            return new Page<>(current, size, 0);
        }

        // 9. 【关键】内存排序
        // MySQL 的 listByIds 返回顺序是乱的(或按主键排)，但 ES 返回的 ID 是按“相关度”排好序的。
        // 我们必须把 postList 重新排序，让它和 postIds 的顺序保持一致，否则搜索结果的相关性就乱了。
        postList.sort(Comparator.comparingInt(p -> postIds.indexOf(p.getId())));

        // 10. 构建 MyBatis-Plus 的分页对象
        Page<Post> postPage = new Page<>(current, size, searchHits.getTotalHits());
        postPage.setRecords(postList);

        // 11. 转换为 VO 对象 (这一步会填充头像、昵称、Redis里的点赞数)
        Page<PostVO> voPage = getPostVOPage(postPage);

        // 12. 【注入高亮】画龙点睛
        // 遍历最终结果，检查 Map 里有没有该 ID 的高亮文本。如果有，覆盖掉普通的文本。
        for (PostVO vo : voPage.getRecords()) {
            String highContent = contentHighlightMap.get(vo.getId());
            if (highContent != null) vo.setContent(highContent);

            String highTag = tagHighlightMap.get(vo.getId());
            if (highTag != null) vo.setTag(highTag);
        }

        return voPage;
    }

    @Override
    public Page<PostVO> listPostByPage(PostQueryRequest postQueryRequest) {
        long current = postQueryRequest.getCurrent();
        long size = postQueryRequest.getPageSize();
        String searchText = postQueryRequest.getSearchText();

        // ============================================================
        // 场景一：用户在搜索框输入了字 -> 走 ES
        // ============================================================
        // 只要 searchText 不为空，就认为用户在搜索，必须用 ES 才能支持分词和高亮
        if (StrUtil.isNotBlank(searchText)) {
            return searchByEs(postQueryRequest);
        }

        // ============================================================
        // 场景二：用户只是在刷首页 -> 走 MySQL
        // ============================================================
        // 没有搜索词，直接查数据库，性能最稳

        // 1. MP 的查询包装器
        LambdaQueryWrapper<Post> queryWrapper = new LambdaQueryWrapper<>();

        // 如果有点“标签”筛选 (比如点“树洞”分类)，这里加个 where tag = ?
        if (StrUtil.isNotBlank(postQueryRequest.getTag())) {
            queryWrapper.eq(Post::getTag, postQueryRequest.getTag());
        }

        // 按创建时间倒序 (新的在上面)
        queryWrapper.orderByDesc(Post::getCreateTime);

        // 2. 执行数据库分页查询
        Page<Post> postPage = this.page(new Page<>(current, size), queryWrapper);

        // 3. 转成 VO 返回
        return getPostVOPage(postPage);
    }

    /**
     * 🛠️ [通用方法] Post (数据库实体) -> PostVO (前端视图)
     * 这是一个非常经典的 "Entity 转 VO" 模板方法
     */
    private Page<PostVO> getPostVOPage(Page<Post> postPage) {
        List<Post> posts = postPage.getRecords();
        Page<PostVO> voPage = new Page<>(postPage.getCurrent(), postPage.getSize(), postPage.getTotal());

        // 防御性编程：如果是空列表，直接返回，别往下走了
        if (CollUtil.isEmpty(posts)) {
            voPage.setRecords(new ArrayList<>());
            return voPage;
        }

        // =================================================
        // 1. 批量查询用户信息 (性能优化核心！)
        // =================================================
        // ❌ 错误做法：在下面的循环里一个一个查 User，会导致查 10 个帖子要读 10 次库 (N+1 问题)
        // ✅ 正确做法：
        //    a. 先把这页帖子的所有作者 ID 收集起来 -> [101, 102, 101]
        Set<Long> userIds = posts.stream().map(Post::getUserId).collect(Collectors.toSet());

        //    b. 一次 SQL 查完所有作者 -> SELECT * FROM user WHERE id IN (101, 102)
        //    c. 转成 Map 方便查找 -> {101: UserA, 102: UserB}
        Map<Long, User> userMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // =================================================
        // 2. 准备当前登录用户
        // =================================================
        // 我们需要知道“我”是谁，才能判断“我”有没有点赞
        User loginUser = UserHolder.getUser();

        // =================================================
        // 3. 组装流水线
        // =================================================
        List<PostVO> voList = posts.stream().map(post -> {
            PostVO vo = new PostVO();
            // 属性拷贝：把 Post 里的 id, content, createTime 拷给 VO
            BeanUtil.copyProperties(post, vo);

            // --- 装修步骤 A: 贴上作者头像和名字 ---
            User author = userMap.get(post.getUserId()); // 直接从内存 Map 拿，不查库
            if (author != null) {
                vo.setUsername(author.getNickname());
                vo.setUserAvatar(author.getAvatar());
            }

            String likeKey = POST_LIKED_KEY + post.getId();

            // --- 装修步骤 B: 计算个性化状态 ---
            if (loginUser != null) {
                // 我是不是楼主？(决定是否显示删除按钮)
                vo.setIsOwner(loginUser.getId().equals(post.getUserId()));

                // 我点赞了吗？(决定爱心是不是红的)
                // 去 Redis 的 Set 集合里查：我的 ID 在不在这个帖子的点赞名单里？
                Boolean isLiked = stringRedisTemplate.opsForSet().isMember(likeKey, loginUser.getId().toString());
                vo.setIsLiked(Boolean.TRUE.equals(isLiked));

                // 覆盖点赞数
            } else {
                // 没登录，当然全都是 false
                vo.setIsLiked(false);
                vo.setIsOwner(false);
            }

            // 查 Redis 里的 Set 大小以得到点赞数，避免数据不一致性
            Long realLikeCount = stringRedisTemplate.opsForSet().size(likeKey);

            // 如果 Redis 里有数据 (比如你刚取消赞，Redis是8，DB是9)，这里强行用 8 覆盖 9
            if (realLikeCount != null && realLikeCount > 0) {
                vo.setLikeCount(realLikeCount.intValue());
            }

            return vo;
        }).collect(Collectors.toList());

        // 把装修好的列表放回分页对象
        voPage.setRecords(voList);
        return voPage;
    }

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




