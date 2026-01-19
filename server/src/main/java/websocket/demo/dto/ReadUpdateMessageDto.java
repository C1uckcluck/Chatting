package websocket.demo.dto;

import java.util.List;

public record ReadUpdateMessageDto(ChatMessageType type, List<ReadUpdateItemDto> updates) {
    public record ReadUpdateItemDto(Long messageId, Integer unreadCount) {}
}
