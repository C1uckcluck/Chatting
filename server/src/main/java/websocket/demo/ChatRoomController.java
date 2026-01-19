
package websocket.demo;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import websocket.demo.dto.ChatRoomDto;
import websocket.demo.dto.ApiResponse;
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
    public ApiResponse<List<ChatRoomDto>> getAllRooms() {
        return ApiResponse.success(chatRoomService.findAll());
    }

    @GetMapping("/rooms/my")
    public ApiResponse<List<ChatRoomDto>> getMyRooms(Principal principal) {
        return ApiResponse.success(chatRoomService.findByUsername(principal.getName()));
    }

    // 채팅방 생성
    @PostMapping("/rooms")
    public ApiResponse<ChatRoomDto> createRoom(@RequestBody String name) {
        return ApiResponse.success(chatRoomService.create(name));
    }

    // 특정 채팅방 정보 반환
    @GetMapping("/rooms/{roomId}")
    public ApiResponse<ChatRoomDto> getRoomById(@PathVariable String roomId) {
        return ApiResponse.success(chatRoomService.findById(roomId));
    }

    @PostMapping("/rooms/{roomId}/enter")
    public ApiResponse<Void> enterRoom(@PathVariable String roomId, Principal principal) {
        String username = principal.getName();
        boolean joined = chatRoomService.enterRoom(roomId, username);
        if (joined) {
            ChatMessageDto chatMessage = new ChatMessageDto(
                    ChatMessageType.ENTER,
                    username,
                    username + "님이 입장했습니다.",
                    null,
                    LocalDateTime.now().format(formatter),
                    0
            );
            chatMessageService.saveMessage(chatMessage, roomId);
            messagingTemplate.convertAndSend("/sub/" + roomId, chatMessage);
        }
        return ApiResponse.success(null);
    }

    @PostMapping("/rooms/{roomId}/leave")
    public ApiResponse<Void> leaveRoom(@PathVariable String roomId, Principal principal) {
        String username = principal.getName();
        boolean left = chatRoomService.leaveRoom(roomId, username);
        if (left) {
            ChatMessageDto chatMessage = new ChatMessageDto(
                    ChatMessageType.LEAVE,
                    username,
                    username + "님이 퇴장했습니다.",
                    null,
                    LocalDateTime.now().format(formatter),
                    0
            );
            chatMessageService.saveMessage(chatMessage, roomId);
            messagingTemplate.convertAndSend("/sub/" + roomId, chatMessage);
        }
        return ApiResponse.success(null);
    }
}
