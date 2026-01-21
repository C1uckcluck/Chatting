package websocket.demo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import websocket.demo.redis.RedisMessagePublisher;

@Service
@RequiredArgsConstructor
public class MessageBroadcastService {

    private final RedisMessagePublisher redisMessagePublisher;

    public void send(String destination, Object payload) {
        redisMessagePublisher.publish(destination, payload);
    }
}
