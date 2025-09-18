package websocket.demo.config;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import websocket.demo.config.handler.ChatWebSocketHandler;


@Configuration
@AllArgsConstructor
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        System.out.println("최초 WebSocket 연결을 위한 등록 handler");
        //클라이언트에서 웹 소켓 연결을 위해 paths 엔드포인트로 연결 시도하면 chatWebSocketHandler 이를 처리
        registry.addHandler(chatWebSocketHandler, "/ws-stomp")
                .setAllowedOrigins("*");
    }
}
