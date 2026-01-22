
package websocket.demo.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import websocket.demo.domain.ChatRoomEntity;

import jakarta.persistence.LockModeType;

public interface ChatRoomJpaRepository extends JpaRepository<ChatRoomEntity, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ChatRoomEntity c where c.roomId = :roomId")
    Optional<ChatRoomEntity> findByIdForUpdate(@Param("roomId") String roomId);
}
