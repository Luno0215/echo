package com.luno.echo.model.es.repository;

import com.luno.echo.model.es.PostEsDTO;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

// 继承这个接口，你就自动拥有了 save, delete, findAll, search 等方法
public interface PostEsRepository extends ElasticsearchRepository<PostEsDTO, Long> {
}