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

import websocket.demo.domain.ChatRoomEntity;
import websocket.demo.domain.ChatRoomMemberEntity;
import websocket.demo.repository.ChatRoomJpaRepository;
import websocket.demo.repository.ChatRoomMemberJpaRepository;
import websocket.demo.service.ChatMessageService;

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
    private final ChatMessageService chatMessageService;
    private final ChatRoomJpaRepository chatRoomRepository;
    private final ChatRoomMemberJpaRepository chatRoomMemberRepository;
    private final Map<String, String> sessionRoomIdMap = new ConcurrentHashMap<>();
    private final Map<String, String> sessionUsernameMap = new ConcurrentHashMap<>();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // STOMP 클라이언트가 연결될 때 호출되는 메소드
    @EventListener
    public void handleSessionConnect(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        String username = "Unknown";
        if (headerAccessor.getUser() != null) {
            username = headerAccessor.getUser().getName();
        } else {
             // 만약 헤더에 username이 있다면 fallback (기존 로직 유지 고려)
             String headerUsername = headerAccessor.getFirstNativeHeader("username");
             if (headerUsername != null) {
                 username = headerUsername;
             }
        }
        
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

            String username = sessionUsernameMap.get(sessionId);

            // 사용자를 채팅방 멤버로 추가 (이미 존재하지 않는 경우)
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

            ChatMessageDto chatMessage = new ChatMessageDto(ChatMessageType.ENTER, username, username + "님이 입장했습니다.",
                    LocalDateTime.now().format(formatter), 0);

            // DB에 메시지 저장
            chatMessageService.saveMessage(chatMessage, roomId);

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
                    LocalDateTime.now().format(formatter),
                    0
            );

            // DB에 메시지 저장
            chatMessageService.saveMessage(chatMessage, roomId);

            simpMessagingTemplate.convertAndSend("/sub/" + roomId, chatMessage);

            // 맵에서 세션 정보 제거
            sessionRoomIdMap.remove(sessionId);
            sessionUsernameMap.remove(sessionId);
        }
    }
}
