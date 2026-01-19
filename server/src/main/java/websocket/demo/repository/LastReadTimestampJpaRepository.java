
package websocket.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import websocket.demo.domain.LastReadTimestampEntity;

import java.util.Optional;

public interface LastReadTimestampJpaRepository extends JpaRepository<LastReadTimestampEntity, LastReadTimestampEntity.LastReadTimestampId> {
    Optional<LastReadTimestampEntity> findByChatRoom_RoomIdAndUsername(String roomId, String username);
}
