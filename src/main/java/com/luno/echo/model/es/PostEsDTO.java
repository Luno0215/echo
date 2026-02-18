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

    // 2. analyzer = "standard": 标准分词器 (按字/词切分)
    // 如果你以后装了 IK 分词器，这里可以改成 "ik_max_word"
    @Field(type = FieldType.Text, analyzer = "standard")
    private String content;

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