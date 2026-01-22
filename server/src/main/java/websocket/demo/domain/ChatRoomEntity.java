
package websocket.demo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomEntity {

    public static final int DEFAULT_MAX_CAPACITY = 50;

    @Id
    @Column(name = "room_id")
    private String roomId;

    private String name;

    private Integer maxCapacity;

    public ChatRoomEntity(String roomId, String name) {
        this(roomId, name, DEFAULT_MAX_CAPACITY);
    }

    public ChatRoomEntity(String roomId, String name, Integer maxCapacity) {
        this.roomId = roomId;
        this.name = name;
        this.maxCapacity = maxCapacity;
    }
}
