package com.luno.echo.config;

import com.luno.echo.common.constant.MqConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MqConfig {

    @Bean
    public DirectExchange postExchange() {
        return new DirectExchange(MqConstant.POST_EXCHANGE);
    }

    @Bean
    public Queue insertQueue() {
        return new Queue(MqConstant.POST_INSERT_QUEUE, true); // true表示持久化
    }

    @Bean
    public Queue deleteQueue() {
        return new Queue(MqConstant.POST_DELETE_QUEUE, true);
    }

    // 绑定：交换机 + 路由键 -> 队列
    @Bean
    public Binding insertBinding() {
        return BindingBuilder.bind(insertQueue()).to(postExchange()).with(MqConstant.POST_INSERT_KEY);
    }

    @Bean
    public Binding deleteBinding() {
        return BindingBuilder.bind(deleteQueue()).to(postExchange()).with(MqConstant.POST_DELETE_KEY);
    }
}