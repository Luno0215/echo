package com.luno.echo.job;

import cn.hutool.core.collection.CollUtil;
import com.luno.echo.model.entity.Post;
import com.luno.echo.service.PostService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.luno.echo.common.constant.RedisConstants.POST_LIKED_KEY;
import static com.luno.echo.common.constant.RedisConstants.POST_LIKE_DIRTY_KEY;

@Component
@Slf4j
public class LikeTask {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private PostService postService;

    /**
     * 每 30 秒执行一次同步
     */
    @Scheduled(cron = "0/30 * * * * ?")
    public void syncPostLike() {
        log.info("开始执行点赞数同步任务...");

        // 1. 从 Redis 获取所有“脏”帖子 ID
        String dirtyKey = POST_LIKE_DIRTY_KEY;
        Set<String> dirtyPostIds = stringRedisTemplate.opsForSet().members(dirtyKey);

        if (CollUtil.isEmpty(dirtyPostIds)) {
            log.info("没有需要同步的点赞数据。");
            return;
        }

        // 2. 遍历 ID，更新数据库
        List<Post> updateList = new ArrayList<>();
        for (String postIdStr : dirtyPostIds) {
            Long postId = Long.valueOf(postIdStr);
            
            // 2.1 获取 Redis 里实时的真实点赞数 (SCARD)
            String likeKey = POST_LIKED_KEY + postId;
            Long realLikeCount = stringRedisTemplate.opsForSet().size(likeKey);
            
            // 2.2 准备更新对象
            Post post = new Post();
            post.setId(postId);
            post.setLikeCount(realLikeCount.intValue());
            updateList.add(post);
        }

        // 3. 【批量更新】数据库 (这是 MyBatis Plus 的 saveOrUpdateBatch，或者你自己写 SQL)
        // 注意：为了性能，最好是 updateBatchById
        postService.updateBatchById(updateList);

        // 4. 清空脏数据集合 (防止重复更新)
        // 这一步有微小的并发风险(在同步期间又有新点赞)，但在非金融级项目可忽略
        stringRedisTemplate.opsForSet().remove(dirtyKey, dirtyPostIds.toArray());
        
        log.info("点赞同步完成，更新了 {} 条帖子。", updateList.size());
    }
}