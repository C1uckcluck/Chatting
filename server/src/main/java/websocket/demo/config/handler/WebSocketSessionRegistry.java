package websocket.demo.config.handler;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class WebSocketSessionRegistry {

    private final Map<String, Set<String>> sessionRoomIdsMap = new ConcurrentHashMap<>();
    private final Map<String, String> subscriptionRoomIdMap = new ConcurrentHashMap<>();
    private final Map<String, String> subscriptionSessionIdMap = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> sessionSubscriptionIdsMap = new ConcurrentHashMap<>();
    private final Map<String, String> sessionUsernameMap = new ConcurrentHashMap<>();
    private final Map<String, String> sessionDisplayNameMap = new ConcurrentHashMap<>();

    public void registerUser(String sessionId, String username, String displayName) {
        sessionUsernameMap.put(sessionId, username);
        sessionDisplayNameMap.put(sessionId, displayName);
    }

    public void registerRoom(String sessionId, String roomId, String subscriptionId) {
        sessionRoomIdsMap
                .computeIfAbsent(sessionId, key -> ConcurrentHashMap.newKeySet())
                .add(roomId);
        if (subscriptionId != null) {
            subscriptionRoomIdMap.put(subscriptionId, roomId);
            subscriptionSessionIdMap.put(subscriptionId, sessionId);
            sessionSubscriptionIdsMap
                    .computeIfAbsent(sessionId, key -> ConcurrentHashMap.newKeySet())
                    .add(subscriptionId);
        }
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
        Set<String> roomIds = sessionRoomIdsMap.get(sessionId);
        if (roomIds == null || roomIds.isEmpty()) {
            return null;
        }
        return roomIds.iterator().next();
    }

    public Set<String> getRoomIds(String sessionId) {
        Set<String> roomIds = sessionRoomIdsMap.get(sessionId);
        if (roomIds == null) {
            return Collections.emptySet();
        }
        return new HashSet<>(roomIds);
    }

    public String removeSubscription(String subscriptionId) {
        if (subscriptionId == null) {
            return null;
        }
        String roomId = subscriptionRoomIdMap.remove(subscriptionId);
        String sessionId = subscriptionSessionIdMap.remove(subscriptionId);
        if (sessionId != null) {
            Set<String> subscriptionIds = sessionSubscriptionIdsMap.get(sessionId);
            if (subscriptionIds != null) {
                subscriptionIds.remove(subscriptionId);
                if (subscriptionIds.isEmpty()) {
                    sessionSubscriptionIdsMap.remove(sessionId);
                }
            }
        }
        if (sessionId != null && roomId != null) {
            Set<String> roomIds = sessionRoomIdsMap.get(sessionId);
            if (roomIds != null) {
                roomIds.remove(roomId);
                if (roomIds.isEmpty()) {
                    sessionRoomIdsMap.remove(sessionId);
                }
            }
        }
        return roomId;
    }

    public void removeSession(String sessionId) {
        Set<String> subscriptionIds = sessionSubscriptionIdsMap.remove(sessionId);
        if (subscriptionIds != null) {
            for (String subscriptionId : subscriptionIds) {
                subscriptionRoomIdMap.remove(subscriptionId);
                subscriptionSessionIdMap.remove(subscriptionId);
            }
        }
        sessionRoomIdsMap.remove(sessionId);
        sessionUsernameMap.remove(sessionId);
        sessionDisplayNameMap.remove(sessionId);
    }
}
