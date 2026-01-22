package websocket.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import websocket.demo.domain.ChatRoomEntity;
import websocket.demo.domain.ChatRoomMemberEntity;
import websocket.demo.domain.Member;
import websocket.demo.dto.ChatRoomDto;
import websocket.demo.dto.RoomParticipantDto;
import websocket.demo.repository.ChatRoomJpaRepository;
import websocket.demo.repository.ChatRoomMemberJpaRepository;
import websocket.demo.repository.MemberRepository;

@SpringBootTest
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
        ChatRoomDto created = chatRoomService.create("테스트방", 10);

        ChatRoomDto found = chatRoomService.findById(created.roomId());

        assertThat(found.roomId()).isEqualTo(created.roomId());
        assertThat(found.name()).isEqualTo("테스트방");
        assertThat(found.maxCapacity()).isEqualTo(10);
    }

    @Test
    @DisplayName("채팅방 참가 입장과 퇴장이 정상 동작한다")
    void enterAndLeaveRoom() {
        ChatRoomEntity room = chatRoomJpaRepository.save(new ChatRoomEntity("room-enter", "Room"));

        ChatRoomService.EnterRoomResult joined = chatRoomService.enterRoom(room.getRoomId(), "user1");
        ChatRoomService.EnterRoomResult joinedAgain = chatRoomService.enterRoom(room.getRoomId(), "user1");

        assertThat(joined).isEqualTo(ChatRoomService.EnterRoomResult.JOINED);
        assertThat(joinedAgain).isEqualTo(ChatRoomService.EnterRoomResult.ALREADY_JOINED);
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

    @Test
    @DisplayName("정원이 가득 차면 입장이 거부된다")
    void enterRoomFailsWhenFull() {
        ChatRoomEntity room = chatRoomJpaRepository.save(new ChatRoomEntity("room-full", "Room", 1));
        chatRoomMemberJpaRepository.save(new ChatRoomMemberEntity(room, "user1"));

        ChatRoomService.EnterRoomResult result = chatRoomService.enterRoom(room.getRoomId(), "user2");

        assertThat(result).isEqualTo(ChatRoomService.EnterRoomResult.FULL);
    }


    @Test
    @DisplayName("동시에 입장 시 정원 초과 없이 한 명만 입장된다")
    void enterRoomConcurrentOnlyOneJoins() throws Exception {
        //given
        final int numberOfThread = 10;
        final int roomCapacity = 1;
        ChatRoomEntity room = chatRoomJpaRepository.saveAndFlush(new ChatRoomEntity("room-concurrent", "Room", roomCapacity));    
        CountDownLatch countDownLatch = new CountDownLatch(numberOfThread);
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThread);
        List<ChatRoomService.EnterRoomResult> results = new CopyOnWriteArrayList<>();

        //when
        for(int i=0; i<numberOfThread; i++) {
            final String userId = "user" + i;
            executor.execute(() -> {
                ChatRoomService.EnterRoomResult result = chatRoomService.enterRoom(room.getRoomId(), userId);
                results.add(result);
                countDownLatch.countDown();
            });
        }
        countDownLatch.await();
        executor.shutdown();

        // then
        final long enterCount = results.stream().filter(c -> c == ChatRoomService.EnterRoomResult.JOINED).count(); 
        final long deniedCount = results.stream().filter(c -> c == ChatRoomService.EnterRoomResult.FULL).count(); 
        assertThat(enterCount).isEqualTo(roomCapacity);
        assertThat(deniedCount).isEqualTo(numberOfThread - roomCapacity);
    }

}
