
package websocket.demo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import websocket.demo.domain.ChatRoomEntity;
import websocket.demo.domain.LastReadTimestampEntity;
import websocket.demo.repository.ChatRoomJpaRepository;
import websocket.demo.repository.LastReadTimestampJpaRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LastReadTimestampService {

    private final LastReadTimestampJpaRepository lastReadTimestampRepository;
    private final ChatRoomJpaRepository chatRoomRepository;

    @Transactional
    public void updateLastReadTimestamp(String roomId, String username) {
        ChatRoomEntity chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));

        LastReadTimestampEntity lastReadTimestamp = lastReadTimestampRepository
                .findByChatRoom_RoomIdAndUsername(roomId, username)
                .orElse(new LastReadTimestampEntity(chatRoom, username, LocalDateTime.now()));

        lastReadTimestamp.updateLastReadAt(LocalDateTime.now());
        lastReadTimestampRepository.save(lastReadTimestamp);
    }
}
