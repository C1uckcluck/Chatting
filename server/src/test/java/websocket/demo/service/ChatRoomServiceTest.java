package websocket.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import websocket.demo.domain.ChatRoomEntity;
import websocket.demo.domain.ChatRoomMemberEntity;
import websocket.demo.domain.Member;
import websocket.demo.dto.ChatRoomDto;
import websocket.demo.dto.RoomParticipantDto;
import websocket.demo.repository.ChatRoomJpaRepository;
import websocket.demo.repository.ChatRoomMemberJpaRepository;
import websocket.demo.repository.MemberRepository;

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
    @DisplayName("???? ???? ??? ? ??")
    void createAndFindRoom() {
        memberRepository.save(new Member("owner", "pw", "nick"));
        ChatRoomDto created = chatRoomService.create("????", "owner", 10);

        ChatRoomDto found = chatRoomService.findById(created.roomId());

        assertThat(found.roomId()).isEqualTo(created.roomId());
        assertThat(found.name()).isEqualTo("????");
        assertThat(found.maxCapacity()).isEqualTo(10);
        assertThat(found.ownerUsername()).isEqualTo("owner");
        assertThat(found.ownerId()).isNotNull();
        assertThat(found.currentCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("??? ??? ??? ????")
    void enterAndLeaveRoom() {
        ChatRoomEntity room = chatRoomJpaRepository.save(new ChatRoomEntity("room-enter", "Room", 1L));

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
    @DisplayName("??? ???? ?? ?? ???? ????")
    void findRoomsByUsername() {
        ChatRoomEntity room1 = chatRoomJpaRepository.save(new ChatRoomEntity("room-a", "A", 1L));
        ChatRoomEntity room2 = chatRoomJpaRepository.save(new ChatRoomEntity("room-b", "B", 1L));
        chatRoomMemberJpaRepository.save(new ChatRoomMemberEntity(room1, "user1"));
        chatRoomMemberJpaRepository.save(new ChatRoomMemberEntity(room2, "user1"));

        List<ChatRoomDto> rooms = chatRoomService.findByUsername("user1");

        assertThat(rooms).extracting(ChatRoomDto::roomId)
                .containsExactlyInAnyOrder("room-a", "room-b");
    }

    @Test
    @DisplayName("??? ??? ??? ???? ?? ??? ????")
    void getRoomParticipantsReturnsNicknameAndPresence() {
        ChatRoomEntity room = chatRoomJpaRepository.save(new ChatRoomEntity("room-participants", "Room", 1L));
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
    @DisplayName("???? ??? ??? ????")
    void findRoomNotFound() {
        assertThatThrownBy(() -> chatRoomService.findById("missing-room"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("??? ?? ?? ??? ????")
    void enterRoomFailsWhenFull() {
        ChatRoomEntity room = chatRoomJpaRepository.save(new ChatRoomEntity("room-full", "Room", 1L, 1));
        chatRoomMemberJpaRepository.save(new ChatRoomMemberEntity(room, "user1"));

        ChatRoomService.EnterRoomResult result = chatRoomService.enterRoom(room.getRoomId(), "user2");

        assertThat(result).isEqualTo(ChatRoomService.EnterRoomResult.FULL);
    }

    @Test
    @DisplayName("??? ?? ? ?? ?? ?? ? ?? ????")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void enterRoomConcurrentOnlyOneJoins() throws Exception {
        ChatRoomEntity room = chatRoomJpaRepository.saveAndFlush(new ChatRoomEntity("room-concurrent", "Room", 1L, 1));

        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Callable<ChatRoomService.EnterRoomResult> task1 = () -> {
            barrier.await();
            return chatRoomService.enterRoom(room.getRoomId(), "user1");
        };
        Callable<ChatRoomService.EnterRoomResult> task2 = () -> {
            barrier.await();
            return chatRoomService.enterRoom(room.getRoomId(), "user2");
        };

        Future<ChatRoomService.EnterRoomResult> future1 = executor.submit(task1);
        Future<ChatRoomService.EnterRoomResult> future2 = executor.submit(task2);

        ChatRoomService.EnterRoomResult result1 = future1.get();
        ChatRoomService.EnterRoomResult result2 = future2.get();

        executor.shutdown();

        assertThat(List.of(result1, result2))
                .containsExactlyInAnyOrder(
                        ChatRoomService.EnterRoomResult.JOINED,
                        ChatRoomService.EnterRoomResult.FULL
                );
        assertThat(chatRoomMemberJpaRepository.countByChatRoom_RoomId(room.getRoomId())).isEqualTo(1);
    }
}
