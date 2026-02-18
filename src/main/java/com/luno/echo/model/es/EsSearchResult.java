package com.luno.echo.model.es;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class EsSearchResult {
    /**
     * 查到的数据 ID 列表
     */
    private List<Long> ids;

    /**
     * 总条数
     */
    private long total;

    /**
     * 高亮数据 Map
     * Key: 数据ID
     * Value: Map<字段名, 高亮内容>
     */
    private Map<Long, Map<String, String>> highlightMap;
}