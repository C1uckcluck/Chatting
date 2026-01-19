package websocket.demo.dto;

public record PresenceUpdateDto(ChatMessageType type, String username, String nickname, boolean online) {}
