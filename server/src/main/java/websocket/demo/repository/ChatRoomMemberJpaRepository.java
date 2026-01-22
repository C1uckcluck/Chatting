
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

    @Query("select crm.username from ChatRoomMemberEntity crm where crm.chatRoom.roomId = :roomId")
    List<String> findUsernamesByRoomId(@Param("roomId") String roomId);

    @Query("select m.username as username, m.nickname as nickname " +
            "from ChatRoomMemberEntity crm join Member m on m.username = crm.username " +
            "where crm.chatRoom.roomId = :roomId")
    List<RoomParticipantProjection> findParticipantsByRoomId(@Param("roomId") String roomId);

    @Query("select crm.chatRoom.roomId as roomId, count(crm) as memberCount " +
            "from ChatRoomMemberEntity crm " +
            "where crm.chatRoom.roomId in :roomIds " +
            "group by crm.chatRoom.roomId")
    List<RoomMemberCountProjection> findMemberCountsByRoomIds(@Param("roomIds") List<String> roomIds);

    boolean existsByChatRoom_RoomIdAndUsername(String roomId, String username);

    long deleteByChatRoom_RoomIdAndUsername(String roomId, String username);

    interface RoomParticipantProjection {
        String getUsername();

        String getNickname();
    }

    interface RoomMemberCountProjection {
        String getRoomId();

        long getMemberCount();
    }
}
