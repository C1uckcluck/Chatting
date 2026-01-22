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
import websocket.demo.domain.Member;
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
        Map<Long, String> owners = loadOwnerUsernames(rooms);
        return rooms.stream()
                .map(room -> new ChatRoomDto(
                        room.getRoomId(),
                        room.getName(),
                        room.getOwnerId(),
                        room.getOwnerId() == null ? null : owners.get(room.getOwnerId()),
                        room.getMaxCapacity(),
                        counts.getOrDefault(room.getRoomId(), 0)
                ))
                .collect(Collectors.toList());
    }

    public Page<ChatRoomDto> findAllPaged(Pageable pageable) {
        Page<ChatRoomEntity> page = chatRoomRepository.findAll(pageable);
        Map<String, Integer> counts = loadMemberCounts(page.getContent());
        Map<Long, String> owners = loadOwnerUsernames(page.getContent());
        return page.map(room -> new ChatRoomDto(
                room.getRoomId(),
                room.getName(),
                room.getOwnerId(),
                room.getOwnerId() == null ? null : owners.get(room.getOwnerId()),
                room.getMaxCapacity(),
                counts.getOrDefault(room.getRoomId(), 0)
        ));
    }

    public ChatRoomDto findById(String roomId) {
        ChatRoomEntity chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
        int memberCount = (int) chatRoomMemberRepository.countByChatRoom_RoomId(roomId);
        String ownerUsername = chatRoom.getOwnerId() == null
                ? null
                : memberRepository.findById(chatRoom.getOwnerId()).map(Member::getUsername).orElse(null);
        return new ChatRoomDto(
                chatRoom.getRoomId(),
                chatRoom.getName(),
                chatRoom.getOwnerId(),
                ownerUsername,
                chatRoom.getMaxCapacity(),
                memberCount
        );
    }

    public List<ChatRoomDto> findByUsername(String username) {
        List<ChatRoomEntity> rooms = chatRoomMemberRepository.findChatRoomsByUsername(username);
        Map<String, Integer> counts = loadMemberCounts(rooms);
        Map<Long, String> owners = loadOwnerUsernames(rooms);
        return rooms.stream()
                .map(room -> new ChatRoomDto(
                        room.getRoomId(),
                        room.getName(),
                        room.getOwnerId(),
                        room.getOwnerId() == null ? null : owners.get(room.getOwnerId()),
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
    public ChatRoomDto create(String name, String ownerUsername, Integer maxCapacity) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Room name is required.");
        }
        if (maxCapacity == null || maxCapacity < 1) {
            throw new IllegalArgumentException("Max capacity must be at least 1.");
        }
        Member owner = memberRepository.findByUsername(ownerUsername)
                .orElseThrow(() -> new IllegalArgumentException("Owner not found: " + ownerUsername));
        ChatRoomDto newRoomDto = ChatRoomDto.create(name, owner.getId(), owner.getUsername(), maxCapacity);
        ChatRoomEntity newRoomEntity = new ChatRoomEntity(
                newRoomDto.roomId(),
                newRoomDto.name(),
                newRoomDto.ownerId(),
                newRoomDto.maxCapacity()
        );
        ChatRoomEntity savedRoom = chatRoomRepository.saveAndFlush(newRoomEntity);
        if (ownerUsername != null && !ownerUsername.isBlank()) {
            chatRoomMemberRepository.save(new ChatRoomMemberEntity(savedRoom, ownerUsername));
        }
        return new ChatRoomDto(
                newRoomDto.roomId(),
                newRoomDto.name(),
                newRoomDto.ownerId(),
                newRoomDto.ownerUsername(),
                newRoomDto.maxCapacity(),
                ownerUsername != null && !ownerUsername.isBlank() ? 1 : 0
        );
    }

    @Transactional
    public UpdateCapacityResult updateMaxCapacity(String roomId, String username, Integer newMaxCapacity) {
        if (newMaxCapacity == null || newMaxCapacity < 1) {
            throw new IllegalArgumentException("Max capacity must be at least 1.");
        }
        ChatRoomEntity chatRoom = chatRoomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
        Long requesterId = memberRepository.findByUsername(username)
                .map(Member::getId)
                .orElse(null);
        if (requesterId == null || chatRoom.getOwnerId() == null || !chatRoom.getOwnerId().equals(requesterId)) {
            return UpdateCapacityResult.FORBIDDEN;
        }
        long memberCount = chatRoomMemberRepository.countByChatRoom_RoomId(roomId);
        if (memberCount > newMaxCapacity) {
            return UpdateCapacityResult.BELOW_CURRENT;
        }
        chatRoom.updateMaxCapacity(newMaxCapacity);
        return UpdateCapacityResult.UPDATED;
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

    private Map<Long, String> loadOwnerUsernames(List<ChatRoomEntity> rooms) {
        List<Long> ownerIds = rooms.stream()
                .map(ChatRoomEntity::getOwnerId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (ownerIds.isEmpty()) {
            return Map.of();
        }
        return memberRepository.findAllById(ownerIds).stream()
                .collect(Collectors.toMap(Member::getId, Member::getUsername));
    }

    public enum EnterRoomResult {
        JOINED,
        ALREADY_JOINED,
        FULL
    }

    public enum UpdateCapacityResult {
        UPDATED,
        FORBIDDEN,
        BELOW_CURRENT
    }
}
