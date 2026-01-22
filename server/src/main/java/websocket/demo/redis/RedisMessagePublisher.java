package websocket.demo.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisMessagePublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String TOPIC_PREFIX = "stomp-broadcast:";

    public void publish(String destination, Object payload) {
        if (destination == null || payload == null) {
            return;
        }
        String roomId = extractRoomId(destination);
        if (roomId == null) {
            return;
        }
        redisTemplate.convertAndSend(TOPIC_PREFIX + roomId, new RedisBroadcastMessage(destination, payload));
    }

    private String extractRoomId(String destination) {
        if (!destination.startsWith("/sub/")) {
            return null;
        }
        int lastSlash = destination.lastIndexOf('/');
        if (lastSlash < 0 || lastSlash == destination.length() - 1) {
            return null;
        }
        return destination.substring(lastSlash + 1);
    }
}
