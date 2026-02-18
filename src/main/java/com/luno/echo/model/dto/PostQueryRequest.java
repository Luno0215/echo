package com.luno.echo.model.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class PostQueryRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 当前页号 (默认第1页)
     */
    private long current = 1;

    /**
     * 页面大小 (默认查10条)
     */
    private long pageSize = 10;

    /**
     * 搜索关键词 (可选)
     */
    private String content;

    /**
     * 按标签筛选 (可选)
     */
    private String tag;
}