
package websocket.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import websocket.demo.domain.ChatMessageEntity;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatMessageJpaRepository extends JpaRepository<ChatMessageEntity, Long> {
    List<ChatMessageEntity> findByChatRoom_RoomId(String roomId);

    @Query("select m.id from ChatMessageEntity m where m.chatRoom.roomId = :roomId and m.sender <> :username and m.sendAt > :lastReadAt and m.initialUnreadCount > 0")
    List<Long> findUnreadCountIds(@Param("roomId") String roomId, @Param("username") String username, @Param("lastReadAt") LocalDateTime lastReadAt);

    @Modifying
    @Query("update ChatMessageEntity m set m.initialUnreadCount = m.initialUnreadCount - 1 where m.id in :ids")
    int decrementUnreadCountsByIds(@Param("ids") List<Long> ids);

    @Query("select m.id, m.initialUnreadCount from ChatMessageEntity m where m.id in :ids")
    List<Object[]> findUnreadCountsByIds(@Param("ids") List<Long> ids);
}
