
package websocket.demo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import websocket.demo.domain.ChatMessageEntity;
import websocket.demo.domain.ChatRoomEntity;
import websocket.demo.dto.ChatMessageDto;
import websocket.demo.repository.ChatMessageJpaRepository;
import websocket.demo.repository.ChatRoomJpaRepository;

import websocket.demo.dto.ChatMessageType;

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
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Transactional
    public void saveMessage(ChatMessageDto messageDto, String roomId) {
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

        chatMessageRepository.save(chatMessage);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDto> findMessagesByRoomId(String roomId) {
        return chatMessageRepository.findByChatRoom_RoomId(roomId).stream()
                .map(entity -> new ChatMessageDto(
                        entity.getType(),
                        entity.getSender(),
                        entity.getContent(),
                        entity.getImageUrl(),
                        entity.getSendAt().format(formatter),
                        entity.getInitialUnreadCount()
                ))
                .collect(Collectors.toList());
    }
}
