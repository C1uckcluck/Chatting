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
import websocket.demo.domain.ChatRoomEntity;
import websocket.demo.domain.ChatRoomMemberEntity;
import websocket.demo.repository.ChatRoomJpaRepository;
import websocket.demo.repository.ChatRoomMemberJpaRepository;
import websocket.demo.service.ChatMessageService;

import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class SessionEventHandler {

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ChatMessageService chatMessageService;
    private final ChatRoomJpaRepository chatRoomRepository;
    private final ChatRoomMemberJpaRepository chatRoomMemberRepository;
    private final Map<String, String> sessionRoomIdMap = new ConcurrentHashMap<>();
    private final Map<String, String> sessionUsernameMap = new ConcurrentHashMap<>();
    private final Map<String, String> sessionDisplayNameMap = new ConcurrentHashMap<>();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @EventListener
    public void handleSessionConnect(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        String username = "Unknown";
        String displayName = null;
        if (headerAccessor.getUser() != null) {
            username = headerAccessor.getUser().getName();
        }
        String headerUsername = headerAccessor.getFirstNativeHeader("username");
        if (headerUsername != null) {
            username = headerUsername;
        }
        displayName = headerAccessor.getFirstNativeHeader("nickname");
        if (displayName == null) {
            displayName = username;
        }

        sessionUsernameMap.put(sessionId, username);
        sessionDisplayNameMap.put(sessionId, displayName);
        log.info("New WebSocket connection: sessionId={}, username={}", sessionId, username);
    }

    @EventListener
    public void handleWebsocketSubscribeListener(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination(); // /sub/{roomId}

        if (destination != null) {
            String roomId = destination.substring(destination.lastIndexOf('/') + 1);
            sessionRoomIdMap.put(sessionId, roomId);

            String username = sessionUsernameMap.get(sessionId);

            ChatRoomEntity chatRoom = chatRoomRepository.findById(roomId)
                    .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
            chatRoomMemberRepository.findById(new ChatRoomMemberEntity.ChatRoomMemberId(chatRoom, username))
                    .ifPresentOrElse(
                            member -> log.info("User {} is already a member of room {}", username, roomId),
                            () -> {
                                chatRoomMemberRepository.save(new ChatRoomMemberEntity(chatRoom, username));
                                log.info("User {} added to room {}", username, roomId);
                            }
                    );

            log.info("User {} subscribed to room {}", sessionId, roomId);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        String roomId = sessionRoomIdMap.get(sessionId);
        String username = sessionUsernameMap.get(sessionId);

        if (roomId != null && username != null) {
            log.info("User {} disconnected from room {}", sessionId, roomId);
            sessionRoomIdMap.remove(sessionId);
            sessionUsernameMap.remove(sessionId);
            sessionDisplayNameMap.remove(sessionId);
        }
    }
}
