package websocket.demo.config.handler;

import java.time.format.DateTimeFormatter;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import websocket.demo.domain.ChatRoomEntity;
import websocket.demo.domain.ChatRoomMemberEntity;
import websocket.demo.dto.ChatMessageType;
import websocket.demo.dto.PresenceUpdateDto;
import websocket.demo.repository.ChatRoomJpaRepository;
import websocket.demo.repository.ChatRoomMemberJpaRepository;
import websocket.demo.service.ChatMessageService;
import websocket.demo.service.MessageBroadcastService;
import websocket.demo.service.RoomPresenceService;

@Component
@RequiredArgsConstructor
@Slf4j
public class SessionEventHandler {

    private final MessageBroadcastService messageBroadcastService;
    private final ChatMessageService chatMessageService;
    private final RoomPresenceService roomPresenceService;
    private final ChatRoomJpaRepository chatRoomRepository;
    private final ChatRoomMemberJpaRepository chatRoomMemberRepository;
    private final WebSocketSessionRegistry sessionRegistry;
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

        sessionRegistry.registerUser(sessionId, username, displayName);
        log.info("New WebSocket connection: sessionId={}, username={}", sessionId, username);
    }

    @EventListener
    public void handleWebsocketSubscribeListener(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination(); // /sub/{roomId}

        if (destination != null) {
            String roomId = destination.substring(destination.lastIndexOf('/') + 1);
            sessionRegistry.registerRoom(sessionId, roomId);

            String username = sessionRegistry.getUsername(sessionId);

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

            String displayName = sessionRegistry.getDisplayName(sessionId);
            if (roomPresenceService.markOnline(roomId, username)) {
                messageBroadcastService.send(
                        "/sub/" + roomId,
                        new PresenceUpdateDto(ChatMessageType.PRESENCE_UPDATE, username, displayName, true)
                );
            }

            log.info("User {} subscribed to room {}", sessionId, roomId);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        String roomId = sessionRegistry.getRoomId(sessionId);
        String username = sessionRegistry.getUsername(sessionId);

        if (roomId != null && username != null) {
            log.info("User {} disconnected from room {}", sessionId, roomId);
            String displayName = sessionRegistry.getDisplayName(sessionId);
            if (roomPresenceService.markOffline(roomId, username)) {
                messageBroadcastService.send(
                        "/sub/" + roomId,
                        new PresenceUpdateDto(ChatMessageType.PRESENCE_UPDATE, username, displayName, false)
                );
            }
            sessionRegistry.removeSession(sessionId);
        }
    }
}
