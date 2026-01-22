
package websocket.demo.dto;

import java.util.UUID;

public record ChatRoomDto(String roomId, String name, Integer maxCapacity, Integer currentCount) {
    public static ChatRoomDto create(String name, Integer maxCapacity) {
        return new ChatRoomDto(UUID.randomUUID().toString(), name, maxCapacity, 0);
    }
}
