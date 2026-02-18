package com.luno.echo.mq;

import cn.hutool.core.bean.BeanUtil;
import com.luno.echo.common.constant.MqConstant;
import com.luno.echo.model.entity.Post;
import com.luno.echo.model.es.PostEsDTO;
import com.luno.echo.model.es.repository.PostEsRepository;
import com.luno.echo.service.PostService;
import com.rabbitmq.client.Channel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class PostMqConsumer {

    @Resource
    private PostEsRepository postEsRepository;

    @Resource
    private PostService postService;

    /**
     * 监听新增/修改队列
     * 只有当 MySQL 事务提交、消息发送成功后，这里才会收到 ID
     */
    @RabbitListener(queues = MqConstant.POST_INSERT_QUEUE)
    public void handleInsert(Long postId, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        try {
            // 1. 拿着 ID 去查 MySQL (确保数据是最新的)
            Post post = postService.getById(postId);
            if (post == null) {
                // 如果 MySQL 里都查不到，说明可能事务回滚了，直接丢弃消息
                channel.basicAck(tag, false); 
                return;
            }

            // 2. 写入 ES
            PostEsDTO postEsDTO = BeanUtil.copyProperties(post, PostEsDTO.class);
            postEsRepository.save(postEsDTO);
            
            // 3. 确认消息 (告诉 MQ 我处理完了，可以删了)
            channel.basicAck(tag, false);
            log.info("ES同步成功，postId: {}", postId);

        } catch (Exception e) {
            log.error("ES同步失败，准备重试: {}", e.getMessage());
            try {
                // 4. 发生异常，拒绝消息并放回队列 (requeue=true)，等待下一次消费
                // 生产环境通常会限制重试次数，避免死循环
                channel.basicNack(tag, false, true); 
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * 监听删除队列
     */
    @RabbitListener(queues = MqConstant.POST_DELETE_QUEUE)
    public void handleDelete(Long postId, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        try {
            postEsRepository.deleteById(postId);
            channel.basicAck(tag, false);
            log.info("ES删除成功，postId: {}", postId);
        } catch (Exception e) {
            log.error("ES同步失败，准备重试: {}", e.getMessage());
            try {
                // 4. 发生异常，拒绝消息并放回队列 (requeue=true)，等待下一次消费
                // 生产环境通常会限制重试次数，避免死循环
                channel.basicNack(tag, false, true);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}