package websocket.demo.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * stomp 메세징 처리를 구현하는 인터페이스 구현체
 * websocket 통신의 설정 구성 가능
 */
@EnableWebSocketMessageBroker
@Configuration
public class WebSocketStompBrokerConfig implements WebSocketMessageBrokerConfigurer {

    // 메세지 브로커 옵션 구성
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 구독(sub)
        // 접두사(/sub)로 시작하는 메세지를 브로커가 처리하도록 설정 => 클라이언트는 이 접두사로 시작하는 주제를 구독하여 메세지를 수신할 수 있음
        // 예를 들어 특정 채팅방의 메세지를 받기 위해 /sub/** 형태로 sub 요청을 하면 됨.
        registry.enableSimpleBroker("/sub");

        // 발행(pub)
        // 전달된 메시지가 @MessageMapping이 달린 controller method로 라우팅, 클라이언트는 이 prefix를 통해 메세지를 publish 함
        // 예를 들어 특정 채팅방방의 메세지를 보내려면 /pub/chattingservername 경로로 메세지를 보내면 됨
        registry.setApplicationDestinationPrefixes("/pub");
    }

    // 특정 URL에 매핑되는 STOMP 엔드포인트를 등록하고, SockJS 옵션 활성화
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry
                .addEndpoint("/ws-stomp")
                .setAllowedOrigins("http://127.0.0.1:5500")
                .withSockJS();
    }
}
