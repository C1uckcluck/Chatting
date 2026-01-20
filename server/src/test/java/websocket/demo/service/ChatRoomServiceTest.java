package websocket.demo.service;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import websocket.demo.domain.ChatRoomEntity;
import websocket.demo.domain.ChatRoomMemberEntity;
import websocket.demo.domain.Member;
import websocket.demo.dto.ChatRoomDto;
import websocket.demo.dto.RoomParticipantDto;
import websocket.demo.repository.ChatRoomJpaRepository;
import websocket.demo.repository.ChatRoomMemberJpaRepository;
import websocket.demo.repository.MemberRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ChatRoomServiceTest {

    @Autowired
    private ChatRoomService chatRoomService;

    @Autowired
    private ChatRoomJpaRepository chatRoomJpaRepository;

    @Autowired
    private ChatRoomMemberJpaRepository chatRoomMemberJpaRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RoomPresenceService roomPresenceService;

    @Test
    @DisplayName("채팅방을 생성하고 조회할 수 있다")
    void createAndFindRoom() {
        ChatRoomDto created = chatRoomService.create("테스트방");

        ChatRoomDto found = chatRoomService.findById(created.roomId());

        assertThat(found.roomId()).isEqualTo(created.roomId());
        assertThat(found.name()).isEqualTo("테스트방");
    }

    @Test
    @DisplayName("채팅방 참가 입장과 퇴장이 정상 동작한다")
    void enterAndLeaveRoom() {
        ChatRoomEntity room = chatRoomJpaRepository.save(new ChatRoomEntity("room-enter", "Room"));

        boolean joined = chatRoomService.enterRoom(room.getRoomId(), "user1");
        boolean joinedAgain = chatRoomService.enterRoom(room.getRoomId(), "user1");

        assertThat(joined).isTrue();
        assertThat(joinedAgain).isFalse();
        assertThat(chatRoomMemberJpaRepository.existsByChatRoom_RoomIdAndUsername(room.getRoomId(), "user1"))
                .isTrue();

        boolean left = chatRoomService.leaveRoom(room.getRoomId(), "user1");

        assertThat(left).isTrue();
        assertThat(chatRoomMemberJpaRepository.existsByChatRoom_RoomIdAndUsername(room.getRoomId(), "user1"))
                .isFalse();
    }

    @Test
    @DisplayName("사용자 기준으로 참여 중인 채팅방을 조회한다")
    void findRoomsByUsername() {
        ChatRoomEntity room1 = chatRoomJpaRepository.save(new ChatRoomEntity("room-a", "A"));
        ChatRoomEntity room2 = chatRoomJpaRepository.save(new ChatRoomEntity("room-b", "B"));
        chatRoomMemberJpaRepository.save(new ChatRoomMemberEntity(room1, "user1"));
        chatRoomMemberJpaRepository.save(new ChatRoomMemberEntity(room2, "user1"));

        List<ChatRoomDto> rooms = chatRoomService.findByUsername("user1");

        assertThat(rooms).extracting(ChatRoomDto::roomId)
                .containsExactlyInAnyOrder("room-a", "room-b");
    }

    @Test
    @DisplayName("채팅방 참가자 목록에 닉네임과 접속 상태가 반영된다")
    void getRoomParticipantsReturnsNicknameAndPresence() {
        ChatRoomEntity room = chatRoomJpaRepository.save(new ChatRoomEntity("room-participants", "Room"));
        memberRepository.save(new Member("user1", "pw", "nick1"));
        chatRoomMemberJpaRepository.save(new ChatRoomMemberEntity(room, "user1"));

        roomPresenceService.markOnline(room.getRoomId(), "user1");

        List<RoomParticipantDto> participants = chatRoomService.getRoomParticipants(room.getRoomId());

        assertThat(participants).hasSize(1);
        assertThat(participants.get(0).username()).isEqualTo("user1");
        assertThat(participants.get(0).nickname()).isEqualTo("nick1");
        assertThat(participants.get(0).online()).isTrue();
    }

    @Test
    @DisplayName("없는 채팅방 조회 시 예외가 발생한다")
    void findRoomNotFound() {
        assertThatThrownBy(() -> chatRoomService.findById("missing-room"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
