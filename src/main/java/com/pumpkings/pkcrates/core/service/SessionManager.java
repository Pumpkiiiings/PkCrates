package com.pumpkings.pkcrates.core.service;

import com.pumpkings.pkcrates.core.model.session.CrateSession;
import org.bukkit.Location;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which crate blocks currently have an opening in progress.
 *
 * <p>Sessions are keyed by a canonical {@code world:x:y:z} string rather than by
 * {@link Location}: {@code Location.equals} also compares yaw and pitch, so two
 * references to the same block can fail to match if either was derived from an
 * entity's position.</p>
 *
 * <p>Backed by a concurrent map because session lookups are reached from future
 * continuations that may not have hopped back to the main thread yet.</p>
 */
public class SessionManager {

    private final Map<String, CrateSession> activeSessions = new ConcurrentHashMap<>();

    /**
     * Builds the canonical key for a block position, ignoring yaw/pitch.
     */
    private static String keyOf(Location location) {
        return location.getWorld().getName()
                + ":" + location.getBlockX()
                + ":" + location.getBlockY()
                + ":" + location.getBlockZ();
    }

    public boolean isCrateInUse(Location location) {
        return activeSessions.containsKey(keyOf(location));
    }

    public void startSession(Location location, CrateSession session) {
        activeSessions.put(keyOf(location), session);
    }

    public void endSession(Location location) {
        activeSessions.remove(keyOf(location));
    }

    public CrateSession getSession(Location location) {
        return activeSessions.get(keyOf(location));
    }

    /**
     * @return A live view of the active sessions. Safe to iterate while other threads
     *         start or end sessions.
     */
    public Collection<CrateSession> getActiveSessions() {
        return Collections.unmodifiableCollection(activeSessions.values());
    }

    public void cleanup() {
        activeSessions.clear();
    }
}
