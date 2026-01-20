package websocket.demo.service;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import websocket.demo.domain.ChatRoomEntity;
import websocket.demo.domain.ChatRoomMemberEntity;
import websocket.demo.dto.ChatRoomDto;
import websocket.demo.dto.RoomParticipantDto;
import websocket.demo.repository.ChatRoomJpaRepository;
import websocket.demo.repository.ChatRoomMemberJpaRepository;
import websocket.demo.repository.MemberRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {

    private final ChatRoomJpaRepository chatRoomRepository;
    private final ChatRoomMemberJpaRepository chatRoomMemberRepository;
    private final MemberRepository memberRepository;
    private final RoomPresenceService roomPresenceService;

    public List<ChatRoomDto> findAll() {
        return chatRoomRepository.findAll().stream()
                .map(room -> new ChatRoomDto(room.getRoomId(), room.getName()))
                .collect(Collectors.toList());
    }

    public Page<ChatRoomDto> findAllPaged(Pageable pageable) {
        return chatRoomRepository.findAll(pageable)
                .map(room -> new ChatRoomDto(room.getRoomId(), room.getName()));
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

    public List<String> findUsernamesByRoomId(String roomId) {
        return chatRoomMemberRepository.findUsernamesByRoomId(roomId);
    }

    public List<RoomParticipantDto> getRoomParticipants(String roomId) {
        return chatRoomMemberRepository.findParticipantsByRoomId(roomId).stream()
                .map(participant -> new RoomParticipantDto(
                        participant.getUsername(),
                        participant.getNickname(),
                        roomPresenceService.isOnline(roomId, participant.getUsername())
                ))
                .toList();
    }

    public String findNicknameByUsername(String username) {
        return memberRepository.findByUsername(username)
                .map(m -> m.getNickname())
                .orElse(username);
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
