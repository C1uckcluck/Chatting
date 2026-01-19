package websocket.demo.dto;


public record ChatMessageDto(ChatMessageType type, String sender, String content, String sendAt, Integer unreadCount) {}
