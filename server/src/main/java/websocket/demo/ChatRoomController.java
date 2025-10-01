
package websocket.demo;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import websocket.demo.dto.ChatRoomDto;
import websocket.demo.service.ChatRoomService;

import java.util.Collection;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")

public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    // 모든 채팅방 목록 반환
    @GetMapping("/rooms")
    public List<ChatRoomDto> getAllRooms() {
        return chatRoomService.findAll();
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
}
