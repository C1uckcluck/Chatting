
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

    @Id
    @Column(name = "room_id")
    private String roomId;

    private String name;

    public ChatRoomEntity(String roomId, String name) {
        this.roomId = roomId;
        this.name = name;
    }
}
