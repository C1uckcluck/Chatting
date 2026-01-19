package websocket.demo.dto;


public record ChatMessageDto(Long id, ChatMessageType type, String sender, String content, String imageUrl, String sendAt, Integer unreadCount) {}
