
package websocket.demo.dto;

import java.util.UUID;

public record ChatRoomDto(
        String roomId,
        String name,
        Long ownerId,
        String ownerUsername,
        Integer maxCapacity,
        Integer currentCount
) {
    public static ChatRoomDto create(String name, Long ownerId, String ownerUsername, Integer maxCapacity) {
        return new ChatRoomDto(UUID.randomUUID().toString(), name, ownerId, ownerUsername, maxCapacity, 0);
    }
}
