package websocket.demo;

import lombok.AllArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;
import websocket.demo.dto.ChatMessageDto;
import websocket.demo.dto.ChatMessageType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@AllArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate template; // 특정 사용자에게 메세지를 보내는데 사용되는 STOMP Template
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm");


    // /pub/roomId에 메세지 전달시 호출되는 메소드
    @MessageMapping("/{roomId}")
    public void send(@DestinationVariable String roomId,
                               ChatMessageDto chatMessageDto) {
        // /sub/roomId 에 구독중인 사용자들에게 메세지 전달
        if (chatMessageDto.type() == ChatMessageType.TALK) {
            chatMessageDto = new ChatMessageDto(chatMessageDto.type(), chatMessageDto.sender(),
                    chatMessageDto.content(), LocalDateTime.now().format(formatter));
            template.convertAndSend("/sub/" + roomId, chatMessageDto);
        }
    }


}
