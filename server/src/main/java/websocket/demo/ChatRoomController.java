
package websocket.demo;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import websocket.demo.dto.ChatRoomDto;
import websocket.demo.dto.ChatMessageDto;
import websocket.demo.dto.ChatMessageType;
import websocket.demo.service.ChatRoomService;
import websocket.demo.service.ChatMessageService;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Collection;
import java.util.List;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")

public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // 모든 채팅방 목록 반환
    @GetMapping("/rooms")
    public List<ChatRoomDto> getAllRooms() {
        return chatRoomService.findAll();
    }

    @GetMapping("/rooms/my")
    public List<ChatRoomDto> getMyRooms(Principal principal) {
        return chatRoomService.findByUsername(principal.getName());
    }

    // 채팅방 생성
    @PostMapping("/rooms")
    public ChatRoomDto createRoom(@RequestBody String name) {
        return chatRoomService.create(name);
    }

    // 특정 채팅방 정보 반환
    @GetMapping("/rooms/{roomId}")
    public ChatRoomDto getRoomById(@PathVariable String roomId) {
        return chatRoomService.findById(roomId);
    }

    @PostMapping("/rooms/{roomId}/enter")
    public void enterRoom(@PathVariable String roomId, Principal principal) {
        String username = principal.getName();
        boolean joined = chatRoomService.enterRoom(roomId, username);
        if (joined) {
            ChatMessageDto chatMessage = new ChatMessageDto(
                    ChatMessageType.ENTER,
                    username,
                    username + "님이 입장했습니다.",
                    LocalDateTime.now().format(formatter),
                    0
            );
            chatMessageService.saveMessage(chatMessage, roomId);
            messagingTemplate.convertAndSend("/sub/" + roomId, chatMessage);
        }
    }

    @PostMapping("/rooms/{roomId}/leave")
    public void leaveRoom(@PathVariable String roomId, Principal principal) {
        String username = principal.getName();
        boolean left = chatRoomService.leaveRoom(roomId, username);
        if (left) {
            ChatMessageDto chatMessage = new ChatMessageDto(
                    ChatMessageType.LEAVE,
                    username,
                    username + "님이 퇴장했습니다.",
                    LocalDateTime.now().format(formatter),
                    0
            );
            chatMessageService.saveMessage(chatMessage, roomId);
            messagingTemplate.convertAndSend("/sub/" + roomId, chatMessage);
        }
    }
}
