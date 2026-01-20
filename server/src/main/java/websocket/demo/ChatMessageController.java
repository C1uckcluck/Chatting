
package websocket.demo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import websocket.demo.dto.ApiResponse;
import websocket.demo.dto.ChatMessageDto;
import websocket.demo.service.ChatMessageService;
import websocket.demo.service.ChatRoomService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatMessageController {


    private static final List<String> ALLOWED_EXTENTIONS = List.of(".png", ".jpg", ".jpeg", ".gif", ".webp");

    private final ChatMessageService chatMessageService;
    private final ChatRoomService chatRoomService;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @GetMapping("/rooms/{roomId}/messages")
    public ApiResponse<List<ChatMessageDto>> getRoomMessages(@PathVariable String roomId) {
        return ApiResponse.success(chatMessageService.findMessagesByRoomId(roomId));
    }

    @PostMapping(path = "/rooms/{roomId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<String> uploadRoomImage(
            @PathVariable String roomId,
            @RequestPart("file") MultipartFile file
    ) throws IOException {
        try {
            UUID.fromString(roomId);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid roomId format");
        }
        chatRoomService.findById(roomId);

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Empty file");
        }
        if (file.getSize() > 1L * 1024 * 1024) {
            throw new IllegalArgumentException("Image size must be 3MB or less");
        }
        String contentType = file.getContentType();
        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "image" : file.getOriginalFilename());
        String lowerName = originalName.toLowerCase();
        boolean hasImageType = contentType != null && contentType.startsWith("image/");
        boolean hasImageExt = ALLOWED_EXTENTIONS.stream().anyMatch(lowerName::endsWith);
        if (!hasImageType && !hasImageExt) {
            throw new IllegalArgumentException("Only image uploads are allowed");
        }

        String safeName = originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String storedName = UUID.randomUUID() + "_" + safeName;

        Path roomDir = Path.of(uploadDir, roomId);
        Files.createDirectories(roomDir);
        Path target = roomDir.resolve(storedName);
        file.transferTo(target);

        String url = "/uploads/" + roomId + "/" + storedName;
        return ApiResponse.success(url);
    }
}
