package websocket.demo.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisMessagePublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic channelTopic;

    public void publish(String destination, Object payload) {
        if (destination == null || payload == null) {
            return;
        }
        redisTemplate.convertAndSend(channelTopic.getTopic(), new RedisBroadcastMessage(destination, payload));
    }
}
