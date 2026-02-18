package com.luno.echo.common.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.luno.echo.model.es.EsSearchResult;
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
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class EsSearchUtil {

    @Resource
    private ElasticsearchOperations elasticsearchOperations;

    /**
     * ⚡️ 通用搜索方法 (支持：关键词搜索 + 高亮 + 精确过滤)
     *
     * @param keyword     搜索关键词 (例如 "Java")
     * @param page        页码 (从1开始)
     * @param size        每页大小
     * @param clazz       ES 实体类 (例如 PostEsDTO.class)
     * @param searchFields 需要搜索和高亮的字段列表 (例如 ["content", "tag"])
     * @param filterMap   精确过滤条件 (例如 {"tag": "学习"}, 可传 null)
     * @return 简化后的结果
     */
    public <T> EsSearchResult search(String keyword, int page, int size, Class<T> clazz,
                                     List<String> searchFields, Map<String, String> filterMap) {

        EsSearchResult result = new EsSearchResult();

        // 1. 准备高亮配置 (红字)
        HighlightFieldParameters highlightParam = HighlightFieldParameters.builder()
                .withPreTags("<span style='color:red'>")
                .withPostTags("</span>")
                .withRequireFieldMatch(false)
                .build();

        // 2. 动态添加高亮字段
        List<HighlightField> highlightFields = new ArrayList<>();
        if (CollUtil.isNotEmpty(searchFields)) {
            for (String field : searchFields) {
                highlightFields.add(new HighlightField(field, highlightParam));
            }
        }
        Highlight highlight = new Highlight(highlightFields);

        // 3. 构建查询 Query
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> {
                    // A. 关键词搜索 (OR 匹配多个字段)
                    if (StrUtil.isNotBlank(keyword) && CollUtil.isNotEmpty(searchFields)) {
                        b.must(m -> m.bool(sub -> {
                            for (String field : searchFields) {
                                sub.should(s -> s.match(ma -> ma.field(field).query(keyword)));
                            }
                            return sub;
                        }));
                    }

                    // B. 精确过滤 (Filter)
                    if (CollUtil.isNotEmpty(filterMap)) {
                        filterMap.forEach((k, v) -> {
                            if (StrUtil.isNotBlank(v)) {
                                b.filter(f -> f.term(t -> t.field(k).value(v)));
                            }
                        });
                    }
                    return b;
                }))
                .withPageable(PageRequest.of(page - 1, size))
                .withHighlightQuery(new HighlightQuery(highlight, clazz))
                .build();

        // 4. 执行
        SearchHits<T> hits = elasticsearchOperations.search(query, clazz);

        // 5. 解析结果
        List<Long> ids = new ArrayList<>();
        Map<Long, Map<String, String>> highlightResult = new HashMap<>();

        if (hits.hasSearchHits()) {
            for (SearchHit<T> hit : hits) {
                // 这里假设 ID 是 String 类型，转为 Long。如果你的 ID 是 String，请去掉 parseLong
                Long id = Long.parseLong(hit.getId());
                ids.add(id);

                // 解析高亮
                Map<String, String> fieldHighMap = new HashMap<>();
                for (String field : searchFields) {
                    List<String> fieldHighlights = hit.getHighlightField(field);
                    if (CollUtil.isNotEmpty(fieldHighlights)) {
                        fieldHighMap.put(field, fieldHighlights.get(0));
                    }
                }
                highlightResult.put(id, fieldHighMap);
            }
        }

        result.setIds(ids);
        result.setTotal(hits.getTotalHits());
        result.setHighlightMap(highlightResult);
        return result;
    }
}