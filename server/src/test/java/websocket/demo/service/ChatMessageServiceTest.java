package websocket.demo.service;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import websocket.demo.domain.ChatMessageEntity;
import websocket.demo.domain.ChatRoomEntity;
import websocket.demo.domain.ChatRoomMemberEntity;
import websocket.demo.dto.ChatMessageDto;
import websocket.demo.dto.ChatMessageType;
import websocket.demo.repository.ChatMessageJpaRepository;
import websocket.demo.repository.ChatRoomJpaRepository;
import websocket.demo.repository.ChatRoomMemberJpaRepository;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ChatMessageServiceTest {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private ChatMessageJpaRepository chatMessageJpaRepository;

    @Autowired
    private ChatRoomJpaRepository chatRoomJpaRepository;

    @Autowired
    private ChatRoomMemberJpaRepository chatRoomMemberJpaRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("메시지 저장 시 참여자 수를 기준으로 unreadCount가 계산된다")
    void saveMessageCalculatesUnreadCount() {
        ChatRoomEntity room = chatRoomJpaRepository.save(new ChatRoomEntity("room-msg-1", "Room"));
        chatRoomMemberJpaRepository.save(new ChatRoomMemberEntity(room, "user1"));
        chatRoomMemberJpaRepository.save(new ChatRoomMemberEntity(room, "user2"));

        ChatMessageDto dto = new ChatMessageDto(
                null,
                ChatMessageType.TALK,
                "user1",
                "hello",
                null,
                LocalDateTime.now().format(FORMATTER),
                null
        );

        ChatMessageDto saved = chatMessageService.saveMessage(dto, room.getRoomId());

        assertThat(saved.unreadCount()).isEqualTo(1);
        assertThat(chatMessageJpaRepository.findById(saved.id())).isPresent();
    }

    @Test
    @DisplayName("채팅방 ID로 저장된 메시지를 조회한다")
    void findMessagesByRoomId() {
        ChatRoomEntity room = chatRoomJpaRepository.save(new ChatRoomEntity("room-msg-2", "Room"));
        chatMessageJpaRepository.save(new ChatMessageEntity(
                room, ChatMessageType.TALK, "user1", "hello", null, LocalDateTime.now(), 1));

        List<ChatMessageDto> messages = chatMessageService.findMessagesByRoomId(room.getRoomId());

        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).sender()).isEqualTo("user1");
    }

    @Test
    @DisplayName("읽지 않은 메시지 카운트를 감소시키고 결과를 반환한다")
    void decrementUnreadCounts() {
        ChatRoomEntity room = chatRoomJpaRepository.save(new ChatRoomEntity("room-msg-3", "Room"));
        LocalDateTime base = LocalDateTime.now().minusMinutes(10);

        ChatMessageEntity target1 = chatMessageJpaRepository.save(new ChatMessageEntity(
                room, ChatMessageType.TALK, "user2", "m1", null, base.plusMinutes(1), 2));
        ChatMessageEntity target2 = chatMessageJpaRepository.save(new ChatMessageEntity(
                room, ChatMessageType.TALK, "user3", "m2", null, base.plusMinutes(2), 1));
        chatMessageJpaRepository.save(new ChatMessageEntity(
                room, ChatMessageType.TALK, "user1", "own", null, base.plusMinutes(3), 5));

        List<ChatMessageService.ReadUpdateItem> updates =
                chatMessageService.decrementUnreadCounts(room.getRoomId(), "user1", base);

        assertThat(updates).extracting(ChatMessageService.ReadUpdateItem::messageId)
                .containsExactlyInAnyOrder(target1.getId(), target2.getId());

        entityManager.flush();
        entityManager.clear();
        ChatMessageEntity updated1 = chatMessageJpaRepository.findById(target1.getId()).orElseThrow();
        ChatMessageEntity updated2 = chatMessageJpaRepository.findById(target2.getId()).orElseThrow();
        assertThat(updated1.getInitialUnreadCount()).isEqualTo(1);
        assertThat(updated2.getInitialUnreadCount()).isEqualTo(0);
    }
}