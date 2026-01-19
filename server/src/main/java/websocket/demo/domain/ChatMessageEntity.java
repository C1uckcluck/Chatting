
package websocket.demo.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import websocket.demo.dto.ChatMessageType;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private ChatRoomEntity chatRoom;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private ChatMessageType type;

    private String sender;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String imageUrl;

    private Integer initialUnreadCount;

    private LocalDateTime sendAt;

    public ChatMessageEntity(ChatRoomEntity chatRoom, ChatMessageType type, String sender, String content, String imageUrl, LocalDateTime sendAt, Integer initialUnreadCount) {
        this.chatRoom = chatRoom;
        this.type = type;
        this.sender = sender;
        this.content = content;
        this.imageUrl = imageUrl;
        this.sendAt = sendAt;
        this.initialUnreadCount = initialUnreadCount;
    }
}
