package websocket.demo.config.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import websocket.demo.dto.ChatMessageDto;
import websocket.demo.dto.ChatMessageType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class SessionEventHandler {

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final Map<String, String> sessionRoomIdMap = new ConcurrentHashMap<>();
    private final Map<String, String> sessionUsernameMap = new ConcurrentHashMap<>();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // STOMP 클라이언트가 연결될 때 호출되는 메소드
    @EventListener
    public void handleSessionConnect(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String username = Objects.requireNonNull(headerAccessor.getFirstNativeHeader("username"));
        sessionUsernameMap.put(sessionId, username);
        log.info("New WebSocket connection: sessionId={}, username={}", sessionId, username);
    }

    // subscribe할 때 호출되는 메소드
    @EventListener
    public void handleWebsocketSubscribeListener(SessionSubscribeEvent event){
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination(); // /sub/{roomId}

        if (destination != null) {
            String roomId = destination.substring(destination.lastIndexOf('/') + 1);
            sessionRoomIdMap.put(sessionId, roomId);
            log.info("User {} subscribed to room {}", sessionId, roomId);

            String username = sessionUsernameMap.get(sessionId);
            ChatMessageDto chatMessage = new ChatMessageDto(ChatMessageType.ENTER, username, username + "님이 입장했습니다.",
                    LocalDateTime.now().format(formatter));
            simpMessagingTemplate.convertAndSend("/sub/" + roomId, chatMessage);
        }
    }

    // disconnect할 때 사용하는 메소드
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        String roomId = sessionRoomIdMap.get(sessionId);
        String username = sessionUsernameMap.get(sessionId);

        if (roomId != null && username != null) {
            log.info("User {} disconnected from room {}", sessionId, roomId);
            ChatMessageDto chatMessage = new ChatMessageDto(
                    ChatMessageType.LEAVE,
                    username,
                    username + "님이 퇴장했습니다.",
                    LocalDateTime.now().format(formatter)
            );
            simpMessagingTemplate.convertAndSend("/sub/" + roomId, chatMessage);

            // 맵에서 세션 정보 제거
            sessionRoomIdMap.remove(sessionId);
            sessionUsernameMap.remove(sessionId);
        }
    }
}
