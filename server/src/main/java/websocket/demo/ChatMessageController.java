
package websocket.demo;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;
import websocket.demo.dto.ApiResponse;
import websocket.demo.dto.ChatMessageDto;
import websocket.demo.service.ChatMessageService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

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
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Empty file");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image uploads are allowed");
        }

        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "image" : file.getOriginalFilename());
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
