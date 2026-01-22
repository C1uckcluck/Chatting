package websocket.demo.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import websocket.demo.domain.ChatMessageEntity;
import websocket.demo.domain.ChatRoomEntity;
import websocket.demo.dto.ChatMessageType;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ChatMessageJpaRepositoryTest {

    @Autowired
    private ChatMessageJpaRepository chatMessageJpaRepository;

    @Autowired
    private ChatRoomJpaRepository chatRoomJpaRepository;

    @Test
    @DisplayName("채팅방 ID로 메시지 목록을 조회한다")
    void findByChatRoomRoomIdReturnsMessages() {
        ChatRoomEntity room = chatRoomJpaRepository.save(new ChatRoomEntity("room-msg", "Room", 1L));
        chatMessageJpaRepository.save(new ChatMessageEntity(
                room, ChatMessageType.TALK, "user1", "hello", null, LocalDateTime.now(), 1));

        List<ChatMessageEntity> messages = chatMessageJpaRepository.findByChatRoom_RoomId("room-msg");

        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getSender()).isEqualTo("user1");
    }

    @Test
    @DisplayName("읽지 않은 메시지 카운트 관련 쿼리를 검증한다")
    void unreadCountQueriesWork() {
        ChatRoomEntity room = chatRoomJpaRepository.save(new ChatRoomEntity("room-unread", "Room", 1L));
        LocalDateTime base = LocalDateTime.now().minusMinutes(10);

        ChatMessageEntity match1 = chatMessageJpaRepository.save(new ChatMessageEntity(
                room, ChatMessageType.TALK, "user2", "m1", null, base.plusMinutes(1), 2));
        ChatMessageEntity match2 = chatMessageJpaRepository.save(new ChatMessageEntity(
                room, ChatMessageType.TALK, "user3", "m2", null, base.plusMinutes(2), 1));
        chatMessageJpaRepository.save(new ChatMessageEntity(
                room, ChatMessageType.TALK, "user1", "own", null, base.plusMinutes(3), 5));
        chatMessageJpaRepository.save(new ChatMessageEntity(
                room, ChatMessageType.TALK, "user4", "zero", null, base.plusMinutes(4), 0));

        List<Long> ids = chatMessageJpaRepository.findUnreadCountIds("room-unread", "user1", base);

        assertThat(ids).containsExactlyInAnyOrder(match1.getId(), match2.getId());

        chatMessageJpaRepository.decrementUnreadCountsByIds(ids);

        List<Object[]> counts = chatMessageJpaRepository.findUnreadCountsByIds(ids);
        assertThat(counts).hasSize(2);
        assertThat(counts).anySatisfy(row -> {
            assertThat(row[0]).isEqualTo(match1.getId());
            assertThat(row[1]).isEqualTo(1);
        });
        assertThat(counts).anySatisfy(row -> {
            assertThat(row[0]).isEqualTo(match2.getId());
            assertThat(row[1]).isEqualTo(0);
        });
    }
}
