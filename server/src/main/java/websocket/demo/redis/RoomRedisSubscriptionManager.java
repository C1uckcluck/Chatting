package websocket.demo.redis;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoomRedisSubscriptionManager {

    private static final String TOPIC_PREFIX = "stomp-broadcast:";

    private final RedisMessageListenerContainer container;
    private final MessageListenerAdapter listenerAdapter;
    private final ConcurrentHashMap<String, AtomicInteger> roomCounts = new ConcurrentHashMap<>();

    public void subscribe(String roomId) {
        if (roomId == null || roomId.isBlank()) {
            return;
        }
        AtomicInteger counter = roomCounts.computeIfAbsent(roomId, key -> new AtomicInteger(0));
        int next = counter.incrementAndGet();
        if (next == 1) {
            container.addMessageListener(listenerAdapter, new ChannelTopic(TOPIC_PREFIX + roomId));
        }
    }

    public void unsubscribe(String roomId) {
        if (roomId == null || roomId.isBlank()) {
            return;
        }
        AtomicInteger counter = roomCounts.get(roomId);
        if (counter == null) {
            return;
        }
        int next = counter.decrementAndGet();
        if (next <= 0) {
            roomCounts.remove(roomId);
            container.removeMessageListener(listenerAdapter, new ChannelTopic(TOPIC_PREFIX + roomId));
        }
    }
}
