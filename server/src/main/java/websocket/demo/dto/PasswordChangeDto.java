package websocket.demo.dto;

public record PasswordChangeDto(String currentPassword, String newPassword, String newPasswordConfirm) {}
