package websocket.demo.service;

import java.util.List;
import java.util.Map;
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
        List<ChatRoomEntity> rooms = chatRoomRepository.findAll();
        Map<String, Integer> counts = loadMemberCounts(rooms);
        return rooms.stream()
                .map(room -> new ChatRoomDto(
                        room.getRoomId(),
                        room.getName(),
                        room.getMaxCapacity(),
                        counts.getOrDefault(room.getRoomId(), 0)
                ))
                .collect(Collectors.toList());
    }

    public Page<ChatRoomDto> findAllPaged(Pageable pageable) {
        Page<ChatRoomEntity> page = chatRoomRepository.findAll(pageable);
        Map<String, Integer> counts = loadMemberCounts(page.getContent());
        return page.map(room -> new ChatRoomDto(
                room.getRoomId(),
                room.getName(),
                room.getMaxCapacity(),
                counts.getOrDefault(room.getRoomId(), 0)
        ));
    }

    public ChatRoomDto findById(String roomId) {
        ChatRoomEntity chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
        int memberCount = (int) chatRoomMemberRepository.countByChatRoom_RoomId(roomId);
        return new ChatRoomDto(chatRoom.getRoomId(), chatRoom.getName(), chatRoom.getMaxCapacity(), memberCount);
    }

    public List<ChatRoomDto> findByUsername(String username) {
        List<ChatRoomEntity> rooms = chatRoomMemberRepository.findChatRoomsByUsername(username);
        Map<String, Integer> counts = loadMemberCounts(rooms);
        return rooms.stream()
                .map(room -> new ChatRoomDto(
                        room.getRoomId(),
                        room.getName(),
                        room.getMaxCapacity(),
                        counts.getOrDefault(room.getRoomId(), 0)
                ))
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
    public EnterRoomResult enterRoom(String roomId, String username) {
        ChatRoomEntity chatRoom = chatRoomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
        if (chatRoomMemberRepository.existsByChatRoom_RoomIdAndUsername(roomId, username)) {
            return EnterRoomResult.ALREADY_JOINED;
        }
        Integer maxCapacity = chatRoom.getMaxCapacity();
        if (maxCapacity != null) {
            long memberCount = chatRoomMemberRepository.countByChatRoom_RoomId(roomId);
            if (memberCount >= maxCapacity) {
                return EnterRoomResult.FULL;
            }
        }
        chatRoomMemberRepository.save(new ChatRoomMemberEntity(chatRoom, username));
        return EnterRoomResult.JOINED;
    }

    @Transactional
    public boolean leaveRoom(String roomId, String username) {
        return chatRoomMemberRepository.deleteByChatRoom_RoomIdAndUsername(roomId, username) > 0;
    }

    @Transactional
    public ChatRoomDto create(String name, Integer maxCapacity) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Room name is required.");
        }
        if (maxCapacity == null || maxCapacity < 1) {
            throw new IllegalArgumentException("Max capacity must be at least 1.");
        }
        ChatRoomDto newRoomDto = ChatRoomDto.create(name, maxCapacity);
        ChatRoomEntity newRoomEntity = new ChatRoomEntity(
                newRoomDto.roomId(),
                newRoomDto.name(),
                newRoomDto.maxCapacity()
        );
        chatRoomRepository.save(newRoomEntity);
        return new ChatRoomDto(
                newRoomDto.roomId(),
                newRoomDto.name(),
                newRoomDto.maxCapacity(),
                0
        );
    }

    private Map<String, Integer> loadMemberCounts(List<ChatRoomEntity> rooms) {
        if (rooms.isEmpty()) {
            return Map.of();
        }
        List<String> roomIds = rooms.stream().map(ChatRoomEntity::getRoomId).toList();
        return chatRoomMemberRepository.findMemberCountsByRoomIds(roomIds).stream()
                .collect(Collectors.toMap(
                        ChatRoomMemberJpaRepository.RoomMemberCountProjection::getRoomId,
                        projection -> (int) projection.getMemberCount()
                ));
    }

    public enum EnterRoomResult {
        JOINED,
        ALREADY_JOINED,
        FULL
    }
}
