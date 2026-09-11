---
name: pkcrates-animation-author
description: Create or edit PkCrates scripted animation YAML and reusable effect components. Use for animation timelines, tracks, expressions, display motion, effects, presets, or diagnosing animation load errors; do not use for Java animation classes.
---

# PkCrates Animation Author

Create configuration that the current PkCrates parser actually accepts. Never infer a key,
action, function, movement, easing, or placeholder from similar plugins.

## Required workflow

1. Read [references/schema.md](references/schema.md) completely before producing or editing animation YAML.
2. Inspect the target crate and existing `animations/*.yml` / `effects.yml` when available. Preserve unrelated configuration.
3. Prefer named components and prebuilt movements over expressions. Use expressions only where they materially improve the animation.
4. Check every emitted field against the schema. If the requested behavior is unsupported, say so and identify the nearest supported alternative; do not fabricate syntax.
5. Keep duration, tick ranges, action count, display count, particle counts, and nesting within documented limits.
6. Build with `./gradlew build` after repository changes. The loader performs final runtime validation during plugin enable or `/crate reload`; report that live-server validation is still needed when no Paper server was run.

## Output rules

- Put reusable particle/sound/firework bundles in `effects.yml`.
- Put each animation in its own `animations/<id>.yml`.
- Reference the animation from a crate with `animation: <id>`.
- Use stable lowercase display ids and remove temporary displays when no longer needed. Runtime cleanup is a final safety net, not a substitute for intentional removal.
- Explain any expression in plain language next to the generated YAML.
- Do not emit arbitrary JavaScript, Lua, Java, shell commands, file paths, URLs, reflection, or plugin API calls. The DSL supports none of them.

## Design defaults

- 60–160 ticks for ordinary openings.
- At most 10 display entities and 150 particles per visible tick unless the user explicitly asks for a heavier effect.
- `teleport-duration: 1` for smoothly updated displays.
- Reveal `WINNER` near the end; use `RANDOM_REWARD` only for previews.
- Use a deterministic `seed` when repeatable previews matter.
