package websocket.demo;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.PageRequest;

import lombok.RequiredArgsConstructor;
import websocket.demo.dto.ApiResponse;
import websocket.demo.dto.ChatMessageDto;
import websocket.demo.dto.ChatMessageType;
import websocket.demo.dto.ChatRoomDto;
import websocket.demo.dto.PagedResponse;
import websocket.demo.dto.RoomParticipantDto;
import websocket.demo.dto.PresenceUpdateDto;
import websocket.demo.service.ChatMessageService;
import websocket.demo.service.ChatRoomService;
import websocket.demo.service.MessageBroadcastService;
import websocket.demo.service.RoomPresenceService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;
    private final MessageBroadcastService messageBroadcastService;
    private final RoomPresenceService roomPresenceService;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 모든 채팅방 목록 반환
    @GetMapping("/rooms")
    public ApiResponse<PagedResponse<ChatRoomDto>> getAllRooms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size
    ) {
        var pageable = PageRequest.of(page, size);
        var result = chatRoomService.findAllPaged(pageable);
        var payload = new PagedResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
        return ApiResponse.success(payload);
    }

    // 내가 접속한 채팅방 리스트 조회
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

    //참여자 조회
    @GetMapping("/rooms/{roomId}/participants")
    public ApiResponse<List<RoomParticipantDto>> getRoomParticipants(@PathVariable String roomId) {
        return ApiResponse.success(chatRoomService.getRoomParticipants(roomId));
    }

    @PostMapping("/rooms/{roomId}/enter")
    public ApiResponse<Void> enterRoom(@PathVariable String roomId, Principal principal) {
        String username = principal.getName();
        boolean joined = chatRoomService.enterRoom(roomId, username);
        if (joined) {
            ChatMessageDto chatMessage = new ChatMessageDto(
                    null,
                    ChatMessageType.ENTER,
                    username,
                    username + "님이 입장했습니다.",
                    null,
                    LocalDateTime.now().format(formatter),
                    0
            );
            ChatMessageDto saved = chatMessageService.saveMessage(chatMessage, roomId);
            messageBroadcastService.send("/sub/" + roomId, saved);
        }
        return ApiResponse.success(null);
    }

    @PostMapping("/rooms/{roomId}/leave")
    public ApiResponse<Void> leaveRoom(@PathVariable String roomId, Principal principal) {
        String username = principal.getName();
        boolean left = chatRoomService.leaveRoom(roomId, username);
        if (left) {
            ChatMessageDto chatMessage = new ChatMessageDto(
                    null,
                    ChatMessageType.LEAVE,
                    username,
                    username + "님이 퇴장했습니다.",
                    null,
                    LocalDateTime.now().format(formatter),
                    0
            );
            ChatMessageDto saved = chatMessageService.saveMessage(chatMessage, roomId);
            messageBroadcastService.send("/sub/" + roomId, saved);

            if (roomPresenceService.forceOffline(roomId, username)) {
                messageBroadcastService.send(
                        "/sub/" + roomId,
                        new PresenceUpdateDto(
                                ChatMessageType.PRESENCE_UPDATE,
                                username,
                                chatRoomService.findNicknameByUsername(username),
                                false
                        )
                );
            }
        }
        return ApiResponse.success(null);
    }
}
