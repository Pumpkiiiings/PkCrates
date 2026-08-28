package com.pumpkings.pkcrates.core.effect;

import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Geometric patterns a particle effect can be drawn in.
 *
 * <p>Each constant turns a point budget into a list of offsets from the effect's origin.
 * Keeping the geometry here — rather than inside each animation, as the built-in
 * animations do — is what lets a server owner invent new looks from {@code config.yml}
 * without touching Java.</p>
 */
public enum EffectShape {

    /** Everything at the origin. The default, and what a bare particle line produces. */
    POINT {
        @Override
        public List<Vector> offsets(int count, double radius, double height) {
            List<Vector> points = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                points.add(new Vector(0, 0, 0));
            }
            return points;
        }
    },

    /** Flat horizontal ring of {@code radius}, drawn at {@code height}. */
    CIRCLE {
        @Override
        public List<Vector> offsets(int count, double radius, double height) {
            List<Vector> points = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                double angle = 2 * Math.PI * i / count;
                points.add(new Vector(Math.cos(angle) * radius, height, Math.sin(angle) * radius));
            }
            return points;
        }
    },

    /** Hollow sphere, points spread evenly using the golden-angle spiral. */
    SPHERE {
        @Override
        public List<Vector> offsets(int count, double radius, double height) {
            List<Vector> points = new ArrayList<>(count);
            double goldenAngle = Math.PI * (3 - Math.sqrt(5));
            for (int i = 0; i < count; i++) {
                double y = 1 - (i / (double) Math.max(1, count - 1)) * 2;
                double ringRadius = Math.sqrt(Math.max(0, 1 - y * y));
                double theta = goldenAngle * i;
                points.add(new Vector(
                        Math.cos(theta) * ringRadius * radius,
                        y * radius + height,
                        Math.sin(theta) * ringRadius * radius));
            }
            return points;
        }
    },

    /** Two strands twisting upwards, DNA style. */
    HELIX {
        @Override
        public List<Vector> offsets(int count, double radius, double height) {
            List<Vector> points = new ArrayList<>(count);
            int perStrand = Math.max(1, count / 2);
            for (int strand = 0; strand < 2; strand++) {
                double phase = strand * Math.PI;
                for (int i = 0; i < perStrand; i++) {
                    double progress = i / (double) perStrand;
                    double angle = progress * Math.PI * 4 + phase;
                    points.add(new Vector(
                            Math.cos(angle) * radius,
                            progress * height,
                            Math.sin(angle) * radius));
                }
            }
            return points;
        }
    },

    /** Spiral that tightens as it rises, like something being pulled in. */
    VORTEX {
        @Override
        public List<Vector> offsets(int count, double radius, double height) {
            List<Vector> points = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                double progress = i / (double) count;
                double angle = progress * Math.PI * 6;
                double currentRadius = radius * (1 - progress);
                points.add(new Vector(
                        Math.cos(angle) * currentRadius,
                        progress * height,
                        Math.sin(angle) * currentRadius));
            }
            return points;
        }
    },

    /** Random points inside a sphere; pair with a non-zero speed for an explosion. */
    BURST {
        @Override
        public List<Vector> offsets(int count, double radius, double height) {
            List<Vector> points = new ArrayList<>(count);
            ThreadLocalRandom random = ThreadLocalRandom.current();
            for (int i = 0; i < count; i++) {
                points.add(new Vector(
                        random.nextGaussian() * radius / 2,
                        height + random.nextGaussian() * radius / 2,
                        random.nextGaussian() * radius / 2));
            }
            return points;
        }
    },

    /** Vertical column rising from the origin. */
    BEAM {
        @Override
        public List<Vector> offsets(int count, double radius, double height) {
            List<Vector> points = new ArrayList<>(count);
            ThreadLocalRandom random = ThreadLocalRandom.current();
            for (int i = 0; i < count; i++) {
                double progress = i / (double) count;
                points.add(new Vector(
                        (random.nextDouble() - 0.5) * radius,
                        progress * height,
                        (random.nextDouble() - 0.5) * radius));
            }
            return points;
        }
    },

    /** Outline of a five-pointed star, lying flat. */
    STAR {
        @Override
        public List<Vector> offsets(int count, double radius, double height) {
            List<Vector> points = new ArrayList<>(count);
            int vertices = 5;
            int perEdge = Math.max(1, count / vertices);

            for (int edge = 0; edge < vertices; edge++) {
                // Stepping two vertices at a time traces the classic pentagram.
                double angleFrom = 2 * Math.PI * (edge * 2 % vertices) / vertices - Math.PI / 2;
                double angleTo = 2 * Math.PI * ((edge * 2 + 2) % vertices) / vertices - Math.PI / 2;

                double fromX = Math.cos(angleFrom) * radius;
                double fromZ = Math.sin(angleFrom) * radius;
                double toX = Math.cos(angleTo) * radius;
                double toZ = Math.sin(angleTo) * radius;

                for (int i = 0; i < perEdge; i++) {
                    double t = i / (double) perEdge;
                    points.add(new Vector(
                            fromX + (toX - fromX) * t,
                            height,
                            fromZ + (toZ - fromZ) * t));
                }
            }
            return points;
        }
    },

    /** Three concentric rings, reading as an expanding pulse. */
    WAVE {
        @Override
        public List<Vector> offsets(int count, double radius, double height) {
            List<Vector> points = new ArrayList<>(count);
            int rings = 3;
            int perRing = Math.max(1, count / rings);

            for (int ring = 1; ring <= rings; ring++) {
                double ringRadius = radius * ring / rings;
                for (int i = 0; i < perRing; i++) {
                    double angle = 2 * Math.PI * i / perRing;
                    points.add(new Vector(
                            Math.cos(angle) * ringRadius,
                            height,
                            Math.sin(angle) * ringRadius));
                }
            }
            return points;
        }
    };

    /**
     * Generates the offsets this shape draws.
     *
     * @param count  How many particles to place.
     * @param radius Horizontal extent, in blocks.
     * @param height Vertical extent or elevation, depending on the shape.
     * @return Offsets from the effect origin.
     */
    public abstract List<Vector> offsets(int count, double radius, double height);

    /**
     * @param name Shape name, case-insensitive.
     * @return The matching shape, or {@link #POINT} when the name is unknown or absent.
     */
    public static EffectShape parse(String name) {
        if (name == null || name.isBlank()) return POINT;
        try {
            return valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return POINT;
        }
    }
}
