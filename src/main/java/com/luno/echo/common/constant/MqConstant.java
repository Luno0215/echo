package com.luno.echo.common.constant;

public interface MqConstant {
    // 交换机
    String POST_EXCHANGE = "post_exchange";

    // 队列
    String POST_INSERT_QUEUE = "post_insert_queue";
    String POST_DELETE_QUEUE = "post_delete_queue";

    // 路由键 (Routing Key)
    String POST_INSERT_KEY = "post.insert";
    String POST_DELETE_KEY = "post.delete";
}