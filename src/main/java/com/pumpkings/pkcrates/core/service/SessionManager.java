package com.pumpkings.pkcrates.core.service;

import com.pumpkings.pkcrates.core.model.session.CrateSession;
import org.bukkit.Location;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SessionManager {

    // Map of physical block locations to their active session.
    private final Map<Location, CrateSession> activeSessions = new HashMap<>();

    public boolean isCrateInUse(Location location) {
        return activeSessions.containsKey(location);
    }

    public void startSession(Location location, CrateSession session) {
        activeSessions.put(location, session);
    }

    public void endSession(Location location) {
        activeSessions.remove(location);
    }

    public CrateSession getSession(Location location) {
        return activeSessions.get(location);
    }

    public Collection<CrateSession> getActiveSessions() {
        return activeSessions.values();
    }
    
    public void cleanup() {
        activeSessions.clear();
    }
}
