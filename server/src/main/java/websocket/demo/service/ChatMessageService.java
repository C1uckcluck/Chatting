
package websocket.demo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import websocket.demo.domain.ChatMessageEntity;
import websocket.demo.domain.ChatRoomEntity;
import websocket.demo.dto.ChatMessageDto;
import websocket.demo.repository.ChatMessageJpaRepository;
import websocket.demo.repository.ChatRoomJpaRepository;

import websocket.demo.repository.ChatRoomMemberJpaRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageJpaRepository chatMessageRepository;
    private final ChatRoomJpaRepository chatRoomRepository;
    private final ChatRoomMemberJpaRepository chatRoomMemberRepository;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Transactional
    public ChatMessageDto saveMessage(ChatMessageDto messageDto, String roomId) {
        ChatRoomEntity chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));

        // DTO의 sendAt을 LocalDateTime으로 변환
        LocalDateTime sendAt = LocalDateTime.parse(messageDto.sendAt(), formatter);

        // 안 읽은 수 계산 (전체 멤버 - 1(본인))
        long memberCount = chatRoomMemberRepository.countByChatRoom_RoomId(roomId);
        int unreadCount = (int) Math.max(0, memberCount - 1);

        ChatMessageEntity chatMessage = new ChatMessageEntity(
                chatRoom,
                messageDto.type(),
                messageDto.sender(),
                messageDto.content(),
                messageDto.imageUrl(),
                sendAt,
                unreadCount
        );

        ChatMessageEntity saved = chatMessageRepository.save(chatMessage);
        return new ChatMessageDto(
                saved.getId(),
                saved.getType(),
                saved.getSender(),
                saved.getContent(),
                saved.getImageUrl(),
                saved.getSendAt().format(formatter),
                saved.getInitialUnreadCount()
        );
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDto> findMessagesByRoomId(String roomId) {
        return chatMessageRepository.findByChatRoom_RoomId(roomId).stream()
                .map(entity -> new ChatMessageDto(
                        entity.getId(),
                        entity.getType(),
                        entity.getSender(),
                        entity.getContent(),
                        entity.getImageUrl(),
                        entity.getSendAt().format(formatter),
                        entity.getInitialUnreadCount()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public List<ReadUpdateItem> decrementUnreadCounts(String roomId, String username, LocalDateTime lastReadAt) {
        if (lastReadAt == null) {
            return List.of();
        }
        List<Long> ids = chatMessageRepository.findUnreadCountIds(roomId, username, lastReadAt);
        if (ids.isEmpty()) {
            return List.of();
        }
        chatMessageRepository.decrementUnreadCountsByIds(ids);
        return chatMessageRepository.findUnreadCountsByIds(ids).stream()
                .map(row -> new ReadUpdateItem(((Number) row[0]).longValue(), ((Number) row[1]).intValue()))
                .collect(Collectors.toList());
    }

    public record ReadUpdateItem(Long messageId, Integer unreadCount) {}
}
