package xyz.datt.wave.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;
import xyz.datt.wave.dto.ChatMessageDto;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisPublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic channelTopic;

    public void publish(ChatMessageDto message) {
        log.info("Publishing message to Redis Topic [{}]: {}", channelTopic.getTopic(), message.getMessage());
        redisTemplate.convertAndSend(channelTopic.getTopic(), message);
    }
}
