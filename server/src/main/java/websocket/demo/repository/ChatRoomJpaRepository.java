
package websocket.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import websocket.demo.domain.ChatRoomEntity;

public interface ChatRoomJpaRepository extends JpaRepository<ChatRoomEntity, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ChatRoomEntity c where c.roomId = :roomId")
    Optional<ChatRoomEntity> findByIdForUpdate(@Param("roomId") String roomId);

   @Query("""

        select c.roomId as roomId,

            c.name as name,

            c.ownerId as ownerId,

            m.username as ownerUsername,

            c.maxCapacity as maxCapacity,

            (select count(crm) from ChatRoomMemberEntity crm where crm.chatRoom = c) as memberCount

        from ChatRoomEntity c

            left join Member m on m.id = c.ownerId

        where

            c.roomId = :roomId

        """)
    Optional<ChatRoomDetailProjection> findDetailById(@Param("roomId") String roomId);

    interface ChatRoomDetailProjection {
        String getRoomId();

        String getName();

        Long getOwnerId();

        String getOwnerUsername();

        Integer getMaxCapacity();

        long getMemberCount();
    }
}
