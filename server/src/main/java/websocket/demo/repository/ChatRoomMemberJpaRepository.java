
package websocket.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import websocket.demo.domain.ChatRoomEntity;
import websocket.demo.domain.ChatRoomMemberEntity;

import java.util.List;

public interface ChatRoomMemberJpaRepository extends JpaRepository<ChatRoomMemberEntity, ChatRoomMemberEntity.ChatRoomMemberId> {
    long countByChatRoom_RoomId(String roomId);

    @Query("select distinct crm.chatRoom from ChatRoomMemberEntity crm where crm.username = :username")
    List<ChatRoomEntity> findChatRoomsByUsername(@Param("username") String username);

    boolean existsByChatRoom_RoomIdAndUsername(String roomId, String username);

    long deleteByChatRoom_RoomIdAndUsername(String roomId, String username);
}
