
package websocket.demo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import websocket.demo.domain.ChatRoomEntity;
import websocket.demo.domain.ChatRoomMemberEntity;
import websocket.demo.dto.ChatRoomDto;
import websocket.demo.repository.ChatRoomJpaRepository;
import websocket.demo.repository.ChatRoomMemberJpaRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {

    private final ChatRoomJpaRepository chatRoomRepository;
    private final ChatRoomMemberJpaRepository chatRoomMemberRepository;

    public List<ChatRoomDto> findAll() {
        return chatRoomRepository.findAll().stream()
                .map(room -> new ChatRoomDto(room.getRoomId(), room.getName()))
                .collect(Collectors.toList());
    }

    public ChatRoomDto findById(String roomId) {
        ChatRoomEntity chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
        return new ChatRoomDto(chatRoom.getRoomId(), chatRoom.getName());
    }

    public List<ChatRoomDto> findByUsername(String username) {
        return chatRoomMemberRepository.findChatRoomsByUsername(username).stream()
                .map(room -> new ChatRoomDto(room.getRoomId(), room.getName()))
                .collect(Collectors.toList());
    }

    @Transactional
    public boolean enterRoom(String roomId, String username) {
        if (chatRoomMemberRepository.existsByChatRoom_RoomIdAndUsername(roomId, username)) {
            return false;
        }
        ChatRoomEntity chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
        chatRoomMemberRepository.save(new ChatRoomMemberEntity(chatRoom, username));
        return true;
    }

    @Transactional
    public boolean leaveRoom(String roomId, String username) {
        return chatRoomMemberRepository.deleteByChatRoom_RoomIdAndUsername(roomId, username) > 0;
    }

    @Transactional
    public ChatRoomDto create(String name) {
        ChatRoomDto newRoomDto = ChatRoomDto.create(name);
        ChatRoomEntity newRoomEntity = new ChatRoomEntity(newRoomDto.roomId(), newRoomDto.name());
        chatRoomRepository.save(newRoomEntity);
        return newRoomDto;
    }
}
