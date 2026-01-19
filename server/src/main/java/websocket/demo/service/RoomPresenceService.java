package websocket.demo.service;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RoomPresenceService {

    private final Map<String, Map<String, Integer>> roomUserCounts = new ConcurrentHashMap<>();

    public boolean markOnline(String roomId, String username) {
        Map<String, Integer> userCounts = roomUserCounts.computeIfAbsent(roomId, key -> new ConcurrentHashMap<>());
        int next = userCounts.getOrDefault(username, 0) + 1;
        userCounts.put(username, next);
        return next == 1;
    }

    public boolean markOffline(String roomId, String username) {
        Map<String, Integer> userCounts = roomUserCounts.get(roomId);
        if (userCounts == null) {
            return false;
        }
        Integer current = userCounts.get(username);
        if (current == null) {
            return false;
        }
        int next = current - 1;
        if (next <= 0) {
            userCounts.remove(username);
            if (userCounts.isEmpty()) {
                roomUserCounts.remove(roomId);
            }
            return true;
        }
        userCounts.put(username, next);
        return false;
    }

    public boolean forceOffline(String roomId, String username) {
        Map<String, Integer> userCounts = roomUserCounts.get(roomId);
        if (userCounts == null) {
            return false;
        }
        boolean removed = userCounts.remove(username) != null;
        if (userCounts.isEmpty()) {
            roomUserCounts.remove(roomId);
        }
        return removed;
    }

    public boolean isOnline(String roomId, String username) {
        Map<String, Integer> userCounts = roomUserCounts.get(roomId);
        return userCounts != null && userCounts.getOrDefault(username, 0) > 0;
    }

    public Set<String> getOnlineUsernames(String roomId) {
        Map<String, Integer> userCounts = roomUserCounts.get(roomId);
        if (userCounts == null) {
            return Collections.emptySet();
        }
        return userCounts.keySet();
    }
}
