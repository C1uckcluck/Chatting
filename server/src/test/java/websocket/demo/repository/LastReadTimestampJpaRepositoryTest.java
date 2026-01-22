package websocket.demo.repository;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import websocket.demo.domain.ChatRoomEntity;
import websocket.demo.domain.LastReadTimestampEntity;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class LastReadTimestampJpaRepositoryTest {

    @Autowired
    private LastReadTimestampJpaRepository lastReadTimestampJpaRepository;

    @Autowired
    private ChatRoomJpaRepository chatRoomJpaRepository;

    @Test
    @DisplayName("채팅방과 사용자로 마지막 읽음 시각을 조회한다")
    void findByChatRoomIdAndUsernameReturnsRecord() {
        ChatRoomEntity room = chatRoomJpaRepository.save(new ChatRoomEntity("room-last", "Room", 1L));
        LocalDateTime lastReadAt = LocalDateTime.now().minusMinutes(5);
        lastReadTimestampJpaRepository.save(new LastReadTimestampEntity(room, "user1", lastReadAt));

        var found = lastReadTimestampJpaRepository.findByChatRoom_RoomIdAndUsername("room-last", "user1");

        assertThat(found).isPresent();
        assertThat(found.get().getLastReadAt()).isEqualTo(lastReadAt);
    }
}
