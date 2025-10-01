
package websocket.demo.dto;

import java.util.UUID;

public record ChatRoomDto(String roomId, String name) {
    public static ChatRoomDto create(String name) {
        return new ChatRoomDto(UUID.randomUUID().toString(), name);
    }
}
