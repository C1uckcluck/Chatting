package websocket.demo;

import lombok.AllArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;
import websocket.demo.domain.Member;
import websocket.demo.dto.ChatMessageDto;
import websocket.demo.dto.ChatMessageType;
import websocket.demo.repository.MemberRepository;
import websocket.demo.service.ChatMessageService;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@AllArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate template; // 특정 사용자에게 메세지를 보내는데 사용되는 STOMP Template
    private final ChatMessageService chatMessageService;
    private final MemberRepository memberRepository;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");


    // /pub/roomId에 메세지 전달시 호출되는 메소드
    @MessageMapping("/{roomId}")
    public void send(@DestinationVariable String roomId,
                               ChatMessageDto chatMessageDto,
                               Principal principal) {
        
        String senderNickname = "Unknown";
        if (principal != null) {
            String username = principal.getName();
            senderNickname = memberRepository.findByUsername(username)
                    .map(Member::getNickname)
                    .orElse(username);
        }

        // /sub/roomId 에 구독중인 사용자들에게 메세지 전달
        if (chatMessageDto.type() == ChatMessageType.TALK || chatMessageDto.type() == ChatMessageType.IMAGE) {
            // 안 읽은 수 계산을 위해 DTO를 새로 생성하지 않고, 서비스에서 처리하도록 위임
            // 이 컨트롤러에서는 시간만 설정하고 서비스로 전달
            ChatMessageDto messageToSend = new ChatMessageDto(
                    chatMessageDto.type(),
                    senderNickname, // 로그인한 사용자의 닉네임 사용
                    chatMessageDto.content(),
                    chatMessageDto.imageUrl(),
                    LocalDateTime.now().format(formatter),
                    null // unreadCount는 서비스에서 계산 후 채워짐
            );

            // DB에 메시지 저장 (이 과정에서 unreadCount가 계산되고 저장됨)
            chatMessageService.saveMessage(messageToSend, roomId);

            // 참고: 현재 구조에서는 저장 후 unreadCount가 채워진 DTO를 다시 받아야 하지만,
            // 간단한 구현을 위해 클라이언트에서는 이 메시지의 unreadCount를 바로 사용하지 않음.
            // 안읽음 카운트의 실시간 감소는 별도의 로직이 필요함.
            // 여기서는 저장된 메시지를 기준으로 계산된 초기 unreadCount를 포함하여 전송하는 것이 정석.
            // 지금은 단순화를 위해 null로 보냄.
            template.convertAndSend("/sub/" + roomId, messageToSend);
        }
    }


}
