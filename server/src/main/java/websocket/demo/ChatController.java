package websocket.demo;

import lombok.AllArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.RestController;
import websocket.demo.config.handler.WebSocketSessionRegistry;
import websocket.demo.domain.Member;
import websocket.demo.dto.ChatMessageDto;
import websocket.demo.dto.ChatMessageType;
import websocket.demo.repository.MemberRepository;
import websocket.demo.service.ChatMessageService;
import websocket.demo.service.MessageBroadcastService;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@AllArgsConstructor
public class ChatController {

    private final MessageBroadcastService messageBroadcastService; // 특정 사용자에게 메세지를 보내는데 사용되는 STOMP Template
    private final ChatMessageService chatMessageService;
    private final MemberRepository memberRepository;
    private final WebSocketSessionRegistry sessionRegistry;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    // /pub/roomId에 메세지 전달시 호출되는 메소드
    @MessageMapping("/{roomId}")
    public void send(@DestinationVariable String roomId,
                               ChatMessageDto chatMessageDto,
                               Principal principal,
                               @Header("simpSessionId") String sessionId) {
        
        String senderNickname = "Unknown";
        if (principal != null) {
            String username = principal.getName();
            senderNickname = memberRepository.findByUsername(username)
                    .map(Member::getNickname)
                    .orElse(username);
        } else if (sessionId != null) {
            String displayName = sessionRegistry.getDisplayName(sessionId);
            if (displayName != null) {
                senderNickname = displayName;
            }
        }

        // /sub/roomId 에 구독중인 사용자들에게 메세지 전달
        if (chatMessageDto.type() == ChatMessageType.TALK || chatMessageDto.type() == ChatMessageType.IMAGE) {
            ChatMessageDto messageToSend = new ChatMessageDto(
                    null,
                    chatMessageDto.type(),
                    senderNickname, // 로그인한 사용자의 닉네임 사용
                    chatMessageDto.content(),
                    chatMessageDto.imageUrl(),
                    LocalDateTime.now().format(formatter),
                    null // unreadCount는 서비스에서 계산 후 채워짐
            );

            ChatMessageDto saved = chatMessageService.saveMessage(messageToSend, roomId);
            messageBroadcastService.send("/sub/" + roomId, saved);
        }
    }


}
