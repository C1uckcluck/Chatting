
package websocket.demo;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import websocket.demo.dto.ApiResponse;
import websocket.demo.dto.ReadUpdateMessageDto;
import websocket.demo.dto.ChatMessageType;
import websocket.demo.service.ChatMessageService;
import websocket.demo.service.LastReadTimestampService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ReadMessageController {

    private final LastReadTimestampService lastReadTimestampService;
    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/rooms/{roomId}/read")
    public ApiResponse<Void> markAsRead(@PathVariable String roomId, @RequestBody String username) {
        var previous = lastReadTimestampService.updateLastReadTimestamp(roomId, username);
        var updates = chatMessageService.decrementUnreadCounts(roomId, username, previous).stream()
                .map(item -> new ReadUpdateMessageDto.ReadUpdateItemDto(item.messageId(), item.unreadCount()))
                .toList();
        if (!updates.isEmpty()) {
            messagingTemplate.convertAndSend("/sub/" + roomId, new ReadUpdateMessageDto(ChatMessageType.READ_UPDATE, updates));
        }
        return ApiResponse.success(null);
    }
}
