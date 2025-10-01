
package websocket.demo.service;

import org.springframework.stereotype.Repository;
import websocket.demo.dto.ChatRoomDto;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class ChatRoomRepository {

    private final Map<String, ChatRoomDto> chatRoomMap = new ConcurrentHashMap<>();

    public Collection<ChatRoomDto> findAll() {
        return chatRoomMap.values();
    }

    public ChatRoomDto findById(String roomId) {
        return chatRoomMap.get(roomId);
    }

    public ChatRoomDto create(String name) {
        ChatRoomDto newRoom = ChatRoomDto.create(name);
        chatRoomMap.put(newRoom.roomId(), newRoom);
        return newRoom;
    }
}
