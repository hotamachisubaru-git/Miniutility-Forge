package org.hotamachisubaru.miniutility.forge.GUI;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ForgeTrashBoxSessionStore {
    private final Map<UUID, Long> sessions = new ConcurrentHashMap<>();
    private static final long SESSION_TIMEOUT = 5 * 60 * 1000; // 5 minutes

    public void startSession(UUID playerId) {
        sessions.put(playerId, System.currentTimeMillis());
    }

    public boolean hasActiveSession(UUID playerId) {
        Long startTime = sessions.get(playerId);
        if (startTime == null) return false;
        if (System.currentTimeMillis() - startTime > SESSION_TIMEOUT) {
            sessions.remove(playerId);
            return false;
        }
        return true;
    }

    public void endSession(UUID playerId) {
        sessions.remove(playerId);
    }

    public void cleanupSessions() {
        long now = System.currentTimeMillis();
        sessions.entrySet().removeIf(entry -> now - entry.getValue() > SESSION_TIMEOUT);
    }
}
