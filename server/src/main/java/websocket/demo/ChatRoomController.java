
package websocket.demo;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import websocket.demo.dto.ChatRoomDto;
import websocket.demo.service.ChatRoomRepository;

import java.util.Collection;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")

public class ChatRoomController {

    private final ChatRoomRepository chatRoomRepository;

    // 모든 채팅방 목록 반환
    @GetMapping("/rooms")
    public Collection<ChatRoomDto> getAllRooms() {
        return chatRoomRepository.findAll();
    }

    // 채팅방 생성
    @PostMapping("/rooms")
    public ChatRoomDto createRoom(@RequestBody String name) {
        return chatRoomRepository.create(name);
    }

    // 특정 채팅방 정보 반환
    @GetMapping("/rooms/{roomId}")
    public ChatRoomDto getRoomById(@PathVariable String roomId) {
        return chatRoomRepository.findById(roomId);
    }
}
