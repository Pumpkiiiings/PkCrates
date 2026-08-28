package com.pumpkings.pkcrates.core.ambient;

/**
 * Ambient particle styles for a placed crate block.
 *
 * <p>Each constant maps 1-to-1 with the string written in the crate's
 * {@code ambient-effect} field, case-insensitive. The actual rendering
 * logic lives in {@link CrateAmbientTask}.</p>
 */
public enum AmbientEffect {

    /** Orbiting enchantment glyphs — magical / arcane feel. */
    ENCHANT,

    /** Spinning ring of flame particles — dangerous / legendary feel. */
    FLAME_RING,

    /** Falling snowflakes with occasional sparkle — winter / ice feel. */
    SNOWFALL,

    /** Pulsing portal ring above the crate — mystic / nether feel. */
    PORTAL_RING,

    /**
     * Star-burst END_ROD particles that shoot upward from the block — epic feel.
     */
    STAR_BURST,

    /** Rising TOTEM_OF_UNDYING columns — divine / god-tier feel. */
    TOTEM,

    /** Slow rainbow DUST orbit — colorful / festive feel. */
    RAINBOW,

    /** Blue soul flame and smoke — spooky / underworld feel. */
    SOUL_FLAME,

    /** End portal and dragon breath — void / dark magic feel. */
    VOID,

    /** Floating hearts — romantic / cute feel. */
    HEARTS,

    /** Falling cherry petals — peaceful / spring feel. */
    CHERRY_BLOSSOM,

    /** Sculk souls and charge — deep dark / scary feel. */
    SCULK,

    /** Electric sparks — energetic / tech feel. */
    ELECTRIC,

    /** Water bubbles floating up — aquatic / ocean feel. */
    WATER,

    /** Dripping and falling honey — sweet / sticky feel. */
    HONEY,

    /** No ambient effect. Default for new crates. */
    NONE;

    /**
     * Returns the {@link AmbientEffect} matching {@code name}, or {@link #NONE}
     * when the value is blank or unrecognised.
     */
    public static AmbientEffect fromString(String name) {
        if (name == null || name.isBlank()) return NONE;
        try {
            return valueOf(name.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
