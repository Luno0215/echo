package com.luno.echo;

import cn.hutool.core.bean.BeanUtil;
import com.luno.echo.model.entity.Post;
import com.luno.echo.model.es.PostEsDTO;
import com.luno.echo.model.es.repository.PostEsRepository;
import com.luno.echo.service.PostService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.stream.Collectors;

@SpringBootTest
class EsInitTest {

    @Resource
    private PostService postService; // 你的 MySQL 业务
    @Resource
    private PostEsRepository postEsRepository; // 你的 ES 仓库

    /**
     * 🚀 全量同步：把 MySQL 数据一股脑塞进 ES
     * 运行这个方法，你的 ES 就有数据了！
     */
    @Test
    void loadFullData() {
        // 1. 查出所有帖子 (MySQL)
        List<Post> postList = postService.list();
        if (postList.isEmpty()) {
            System.out.println("MySQL 里没数据，同步个寂寞...");
            return;
        }

        // 2. 转换 Post -> PostEsDTO
        List<PostEsDTO> esList = postList.stream().map(post -> {
            PostEsDTO dto = new PostEsDTO();
            // 属性拷贝
            BeanUtil.copyProperties(post, dto);
            return dto;
        }).collect(Collectors.toList());

        // 3. 批量写入 ES
        postEsRepository.saveAll(esList);
        
        System.out.println("🎉 同步完成！共写入 ES " + esList.size() + " 条数据。");
    }
}