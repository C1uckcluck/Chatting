
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageJpaRepository chatMessageRepository;
    private final ChatRoomJpaRepository chatRoomRepository;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Transactional
    public void saveMessage(ChatMessageDto messageDto, String roomId) {
        ChatRoomEntity chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));

        // DTO의 sendAt을 LocalDateTime으로 변환
        LocalDateTime sendAt = LocalDateTime.parse(messageDto.sendAt(), formatter);

        ChatMessageEntity chatMessage = new ChatMessageEntity(
                chatRoom,
                messageDto.type(),
                messageDto.sender(),
                messageDto.content(),
                sendAt
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
                        entity.getSendAt().format(formatter)
                ))
                .collect(Collectors.toList());
    }
}
