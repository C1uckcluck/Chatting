package websocket.demo.config.handler;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import websocket.demo.dto.ChatMessageDto;
import websocket.demo.dto.ChatMessageType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@AllArgsConstructor
@Slf4j
public class SessionEventHandler {

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final Map<String, String> sessionRoomIdMap = new ConcurrentHashMap<>();

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

            ChatMessageDto chatMessage = new ChatMessageDto(ChatMessageType.ENTER, "System", "새로운 사용자가 입장했습니다.");
            simpMessagingTemplate.convertAndSend("/sub/" + roomId, chatMessage);
        }
    }

    // disconnect할 때 사용하는 메소드
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        // 연결이 끊어진 세션이 어떤 채팅방에 있었는지 확인
        String roomId = sessionRoomIdMap.get(sessionId);
        if (roomId != null) {
            // 퇴장 메시지 전송
            ChatMessageDto chatMessage = new ChatMessageDto(
                    ChatMessageType.LEAVE,
                    "System", // 퇴장 메시지도 시스템이 보냄
                    "사용자가 퇴장했습니다."
            );
            simpMessagingTemplate.convertAndSend("/sub/" + roomId, chatMessage);

            // 맵에서 세션 정보 제거
            sessionRoomIdMap.remove(sessionId);
        }
    }

}
