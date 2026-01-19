
package websocket.demo;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import websocket.demo.dto.ApiResponse;
import websocket.demo.service.LastReadTimestampService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ReadMessageController {

    private final LastReadTimestampService lastReadTimestampService;

    @PostMapping("/rooms/{roomId}/read")
    public ApiResponse<Void> markAsRead(@PathVariable String roomId, @RequestBody String username) {
        lastReadTimestampService.updateLastReadTimestamp(roomId, username);
        return ApiResponse.success(null);
    }
}
