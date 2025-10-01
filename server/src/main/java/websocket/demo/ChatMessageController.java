
package websocket.demo;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import websocket.demo.dto.ChatMessageDto;
import websocket.demo.service.ChatMessageService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    @GetMapping("/rooms/{roomId}/messages")
    public List<ChatMessageDto> getRoomMessages(@PathVariable String roomId) {
        return chatMessageService.findMessagesByRoomId(roomId);
    }
}
