package websocket.demo.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisMessageSubscriber {

    private final SimpMessagingTemplate messagingTemplate;

    public void handleMessage(RedisBroadcastMessage message) {
        if (message == null || message.getDestination() == null || message.getPayload() == null) {
            return;
        }
        messagingTemplate.convertAndSend(message.getDestination(), message.getPayload());
    }
}
