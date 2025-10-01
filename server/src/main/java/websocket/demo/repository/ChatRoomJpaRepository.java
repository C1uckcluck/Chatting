
package websocket.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import websocket.demo.domain.ChatRoomEntity;

public interface ChatRoomJpaRepository extends JpaRepository<ChatRoomEntity, String> {
}
