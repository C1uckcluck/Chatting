
package websocket.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import websocket.demo.domain.ChatRoomMemberEntity;

public interface ChatRoomMemberJpaRepository extends JpaRepository<ChatRoomMemberEntity, ChatRoomMemberEntity.ChatRoomMemberId> {
    long countByChatRoom_RoomId(String roomId);
}
