package com.luno.echo.model.es;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.time.LocalDateTime;

// 1. indexName = "post": 相当于数据库表名
@Data
@Document(indexName = "tb_post")
public class PostEsDTO implements Serializable {

    @Id
    private Long id;

    // 1. analyzer = "standard": 标准分词器 (按字/词切分)
    // 2. analyzer = "ik_max_word": 存数据时，分得最细 (比如 "清华大学" -> "清华"、"大学"、"清华大学")
    // 3. searchAnalyzer = "ik_smart": 搜数据时，分得最粗 (比如搜 "清华大学" -> 就当一个词搜，更精准)
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String content;

    // 标签依然用 Keyword，精确匹配
    @Field(type = FieldType.Keyword) // Keyword 表示不分词，精确匹配
    private String tag;

    @Field(type = FieldType.Long)
    private Long userId;

    @Field(type = FieldType.Integer)
    private Integer likeCount;
    
    @Field(type = FieldType.Integer)
    private Integer commentCount;

    @Field(type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}