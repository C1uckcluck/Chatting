
package websocket.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import websocket.demo.domain.ChatMessageEntity;

import java.util.List;

public interface ChatMessageJpaRepository extends JpaRepository<ChatMessageEntity, Long> {
    List<ChatMessageEntity> findByChatRoom_RoomId(String roomId);
}
