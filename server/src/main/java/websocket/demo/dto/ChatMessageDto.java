package websocket.demo.dto;

import lombok.Data;


public record ChatMessageDto(ChatMessageType type, String sender, String content) {}
