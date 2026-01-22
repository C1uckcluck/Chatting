package websocket.demo;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
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
import websocket.demo.dto.CreateChatRoomRequest;
import websocket.demo.dto.UpdateChatRoomCapacityRequest;
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
    public ApiResponse<ChatRoomDto> createRoom(@RequestBody CreateChatRoomRequest request, Principal principal) {
        return ApiResponse.success(chatRoomService.create(request.name(), principal.getName(), request.maxCapacity()));
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
    public ResponseEntity<ApiResponse<Void>> enterRoom(@PathVariable String roomId, Principal principal) {
        String username = principal.getName();
        ChatRoomService.EnterRoomResult result = chatRoomService.enterRoom(roomId, username);
        if (result == ChatRoomService.EnterRoomResult.FULL) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.failure("최대 인원에 도달해 입장할 수 없습니다."));
        }
        if (result == ChatRoomService.EnterRoomResult.JOINED) {
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
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PatchMapping("/rooms/{roomId}/capacity")
    public ResponseEntity<ApiResponse<Void>> updateRoomCapacity(
            @PathVariable String roomId,
            @RequestBody UpdateChatRoomCapacityRequest request,
            Principal principal
    ) {
        ChatRoomService.UpdateCapacityResult result = chatRoomService.updateMaxCapacity(
                roomId,
                principal.getName(),
                request.maxCapacity()
        );
        if (result == ChatRoomService.UpdateCapacityResult.FORBIDDEN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.failure("방장만 채팅 인원을 변경할 수 있습니다."));
        }
        if (result == ChatRoomService.UpdateCapacityResult.BELOW_CURRENT) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.failure("현재 인원보다 작은 값으로 설정할 수 없습니다."));
        }
        return ResponseEntity.ok(ApiResponse.success(null));
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
