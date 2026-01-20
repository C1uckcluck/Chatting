package websocket.demo.config.handler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class WebSocketSessionRegistry {

    private final Map<String, String> sessionRoomIdMap = new ConcurrentHashMap<>();
    private final Map<String, String> sessionUsernameMap = new ConcurrentHashMap<>();
    private final Map<String, String> sessionDisplayNameMap = new ConcurrentHashMap<>();

    public void registerUser(String sessionId, String username, String displayName) {
        sessionUsernameMap.put(sessionId, username);
        sessionDisplayNameMap.put(sessionId, displayName);
    }

    public void registerRoom(String sessionId, String roomId) {
        sessionRoomIdMap.put(sessionId, roomId);
    }

    public String getUsername(String sessionId) {
        return sessionUsernameMap.get(sessionId);
    }

    public String getDisplayName(String sessionId) {
        String username = sessionUsernameMap.get(sessionId);
        String displayName = sessionDisplayNameMap.get(sessionId);
        return displayName != null ? displayName : username;
    }

    public String getRoomId(String sessionId) {
        return sessionRoomIdMap.get(sessionId);
    }

    public void removeSession(String sessionId) {
        sessionRoomIdMap.remove(sessionId);
        sessionUsernameMap.remove(sessionId);
        sessionDisplayNameMap.remove(sessionId);
    }
}
