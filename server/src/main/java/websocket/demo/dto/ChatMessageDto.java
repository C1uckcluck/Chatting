package websocket.demo.dto;


public record ChatMessageDto(ChatMessageType type, String sender, String content, String imageUrl, String sendAt, Integer unreadCount) {}
