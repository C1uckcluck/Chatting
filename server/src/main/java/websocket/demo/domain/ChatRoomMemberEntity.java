
package websocket.demo.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(ChatRoomMemberEntity.ChatRoomMemberId.class)
public class ChatRoomMemberEntity {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private ChatRoomEntity chatRoom;

    @Id
    @Column(name = "username")
    private String username;

    public ChatRoomMemberEntity(ChatRoomEntity chatRoom, String username) {
        this.chatRoom = chatRoom;
        this.username = username;
    }

    // 복합키를 위한 IdClass
    @NoArgsConstructor
    @EqualsAndHashCode
    @AllArgsConstructor
    public static class ChatRoomMemberId implements Serializable {
        private ChatRoomEntity chatRoom;
        private String username;
    }
}
