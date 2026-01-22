package websocket.demo.service;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import websocket.demo.domain.ChatRoomEntity;
import websocket.demo.repository.ChatRoomJpaRepository;
import websocket.demo.repository.LastReadTimestampJpaRepository;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class LastReadTimestampServiceTest {

    @Autowired
    private LastReadTimestampService lastReadTimestampService;

    @Autowired
    private ChatRoomJpaRepository chatRoomJpaRepository;

    @Autowired
    private LastReadTimestampJpaRepository lastReadTimestampJpaRepository;

    @Test
    @DisplayName("첫 읽음 갱신은 이전 시간이 없고 저장된다")
    void updateLastReadTimestampFirstTime() {
        ChatRoomEntity room = chatRoomJpaRepository.save(new ChatRoomEntity("room-last-1", "Room", 1L));

        LocalDateTime previous = lastReadTimestampService.updateLastReadTimestamp(room.getRoomId(), "user1");

        assertThat(previous).isNull();
        assertThat(lastReadTimestampJpaRepository.findByChatRoom_RoomIdAndUsername(room.getRoomId(), "user1"))
                .isPresent();
    }

    @Test
    @DisplayName("두 번째 갱신은 이전 시간이 반환되고 시간이 갱신된다")
    void updateLastReadTimestampReturnsPrevious() {
        ChatRoomEntity room = chatRoomJpaRepository.save(new ChatRoomEntity("room-last-2", "Room", 1L));
        lastReadTimestampService.updateLastReadTimestamp(room.getRoomId(), "user1");
        LocalDateTime first = lastReadTimestampJpaRepository
                .findByChatRoom_RoomIdAndUsername(room.getRoomId(), "user1")
                .orElseThrow()
                .getLastReadAt();

        LocalDateTime previous = lastReadTimestampService.updateLastReadTimestamp(room.getRoomId(), "user1");

        LocalDateTime second = lastReadTimestampJpaRepository
                .findByChatRoom_RoomIdAndUsername(room.getRoomId(), "user1")
                .orElseThrow()
                .getLastReadAt();
        assertThat(previous).isEqualTo(first);
        assertThat(second).isAfterOrEqualTo(first);
    }
}
