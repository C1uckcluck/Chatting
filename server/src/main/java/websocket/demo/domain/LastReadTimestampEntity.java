
package websocket.demo.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(LastReadTimestampEntity.LastReadTimestampId.class)
public class LastReadTimestampEntity {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private ChatRoomEntity chatRoom;

    @Id
    @Column(name = "username")
    private String username;

    private LocalDateTime lastReadAt;

    public LastReadTimestampEntity(ChatRoomEntity chatRoom, String username, LocalDateTime lastReadAt) {
        this.chatRoom = chatRoom;
        this.username = username;
        this.lastReadAt = lastReadAt;
    }

    public void updateLastReadAt(LocalDateTime lastReadAt) {
        this.lastReadAt = lastReadAt;
    }

    // 복합키를 위한 IdClass
    @NoArgsConstructor
    @EqualsAndHashCode
    public static class LastReadTimestampId implements Serializable {
        private ChatRoomEntity chatRoom;
        private String username;
    }
}
