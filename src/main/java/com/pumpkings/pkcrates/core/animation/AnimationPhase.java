package com.pumpkings.pkcrates.core.animation;

import com.pumpkings.pkcrates.core.model.session.CrateSession;

public interface AnimationPhase {

    /**
     * Called when the animation starts for this session.
     * @param session The opening session
     */
    void onStart(CrateSession session);

    /**
     * Called every tick (20 times per second).
     * Particles are generated here, ArmorStands are moved, etc.
     * @param session The opening session
     */
    void onTick(CrateSession session);

    /**
     * @param session The opening session
     * @return true if the animation has finished and should move to the next phase or end.
     */
    boolean isFinished(CrateSession session);

    /**
     * Called when the animation ends (either naturally or forced).
     * Ideal for removing temporary ArmorStands or Holograms.
     * @param session The opening session
     */
    void onEnd(CrateSession session);
}
