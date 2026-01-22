package websocket.demo.repository;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import websocket.demo.domain.ChatRoomEntity;
import websocket.demo.domain.ChatRoomMemberEntity;
import websocket.demo.domain.Member;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ChatRoomMemberJpaRepositoryTest {

    @Autowired
    private ChatRoomMemberJpaRepository chatRoomMemberJpaRepository;

    @Autowired
    private ChatRoomJpaRepository chatRoomJpaRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("채팅방 멤버십 관련 쿼리들이 정상 동작한다")
    void roomMembershipQueriesWork() {
        ChatRoomEntity room1 = chatRoomJpaRepository.save(new ChatRoomEntity("room-1", "Room 1", 1L));
        ChatRoomEntity room2 = chatRoomJpaRepository.save(new ChatRoomEntity("room-2", "Room 2", 1L));

        memberRepository.save(new Member("user1", "pw", "nick1"));
        memberRepository.save(new Member("user2", "pw", "nick2"));

        chatRoomMemberJpaRepository.save(new ChatRoomMemberEntity(room1, "user1"));
        chatRoomMemberJpaRepository.save(new ChatRoomMemberEntity(room1, "user2"));
        chatRoomMemberJpaRepository.save(new ChatRoomMemberEntity(room2, "user1"));

        assertThat(chatRoomMemberJpaRepository.countByChatRoom_RoomId("room-1")).isEqualTo(2);
        assertThat(chatRoomMemberJpaRepository.findUsernamesByRoomId("room-1"))
                .containsExactlyInAnyOrder("user1", "user2");

        assertThat(chatRoomMemberJpaRepository.findChatRoomsByUsername("user1"))
                .extracting(ChatRoomEntity::getRoomId)
                .containsExactlyInAnyOrder("room-1", "room-2");

        assertThat(chatRoomMemberJpaRepository.existsByChatRoom_RoomIdAndUsername("room-1", "user2")).isTrue();
        assertThat(chatRoomMemberJpaRepository.deleteByChatRoom_RoomIdAndUsername("room-1", "user2")).isEqualTo(1);
        assertThat(chatRoomMemberJpaRepository.existsByChatRoom_RoomIdAndUsername("room-1", "user2")).isFalse();
    }

    @Test
    @DisplayName("채팅방 참가자 조회 시 닉네임을 조인으로 가져온다")
    void findParticipantsByRoomIdJoinsNickname() {
        ChatRoomEntity room = chatRoomJpaRepository.save(new ChatRoomEntity("room-3", "Room 3", 1L));
        memberRepository.save(new Member("user3", "pw", "nick3"));
        chatRoomMemberJpaRepository.save(new ChatRoomMemberEntity(room, "user3"));

        List<ChatRoomMemberJpaRepository.RoomParticipantProjection> participants =
                chatRoomMemberJpaRepository.findParticipantsByRoomId("room-3");

        assertThat(participants).hasSize(1);
        assertThat(participants.get(0).getUsername()).isEqualTo("user3");
        assertThat(participants.get(0).getNickname()).isEqualTo("nick3");
    }
}
