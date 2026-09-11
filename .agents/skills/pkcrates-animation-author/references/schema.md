# PkCrates scripted animation schema

This describes the implemented parser in `core/animation/script`. Anything absent here is unsupported.

## Files and registration

- Definitions: `plugins/PkCrates/animations/*.yml`
- Components: `plugins/PkCrates/effects.yml`
- Crate reference: `animation: ID`
- Script ids are case-insensitive and must not collide with a built-in id.
- Built-ins: `ROULETTE`, `CSGO`, `FOUNTAIN`, `SPIRAL`, `BLACKHOLE`, `METEOR`,
  `PORTAL`, `LIGHTNING`, `GALAXY`, `FROZEN`.

## Root fields

```yaml
id: MY_ANIMATION             # [A-Za-z0-9_-], max 64 characters
duration: 120                # 1..1200 ticks
seed: 1337                   # optional deterministic base seed
variables:                   # numeric constants only
  radius: 1.4
limits:
  max-actions-per-tick: 100  # clamped 1..200
  max-displays: 20           # clamped 1..50
timeline: []
tracks:                      # names organize only; all tracks run in parallel
  effects: []
  displays: []
```

## Timing and conditions

```yaml
at: 20

during: 20..80
every: 2

from: 20
to: 80
every: 2

at: 20
repeat: 6       # 1..100
interval: 4
```

Optional numeric condition: `if: 'winner.weight <= 5'`.
Comparators: `<`, `>`, `<=`, `>=`, `==`, `!=`. Boolean operators, strings, rarity names,
permissions, and player state are not implemented.

## Expressions

Numeric fields of display animation actions accept expressions. Variables: user-defined numeric
variables, `time`, `duration`, `progress`, `action.progress`, `motion.progress`, `winner.weight`.

Operators: `+ - * / % ^` and parentheses. Functions: `sin`, `cos`, `tan`, `abs`, `sqrt`,
`floor`, `ceil`, `round`, `min`, `max`, `clamp(value,min,max)`, `lerp(from,to,progress)`,
`random()` and `random(min,max)`. Constants: `pi`, `e`.

No assignments, reflection, or external calls exist.

## Effect components

```yaml
components:
  electric-opening:
    actions:
      - 'sound:ENTITY_LIGHTNING_BOLT_THUNDER volume:0.6 pitch:1.8'
      - 'particle:ELECTRIC_SPARK shape:VORTEX radius:1.4 height:3 count:80'
      - 'firework:FFEE55 type:STAR'
```

Use with `use: electric-opening`. Inline compact effects use
`effect: 'particle:END_ROD shape:SPHERE radius:1 count:30'`. Structured effects use:

```yaml
particle:
  type: END_ROD
  shape: SPHERE
  radius: 1
  count: 30
```

Kinds: `particle`, `sound`, `firework`. Shapes: `POINT`, `CIRCLE`, `SPHERE`, `HELIX`,
`VORTEX`, `BURST`, `BEAM`, `STAR`, `WAVE`. Fireworks: `BALL`, `BALL_LARGE`, `STAR`,
`BURST`, `CREEPER`. Particle count is capped at 500.

## Display actions

```yaml
- at: 5
  spawn:
    id: preview
    item: RANDOM_REWARD       # WINNER, WINNING_REWARD, RANDOM_REWARD, or vanilla Material
    offset: [0, 0.2, 0]
    scale: 0.55
    billboard: CENTER         # FIXED, VERTICAL, HORIZONTAL, CENTER
    teleport-duration: 1      # 0..59

- during: 5..75
  animate:
    id: preview
    motion: ORBIT
    radius: 1.4
    height: '0.5 + sin(time * 0.12) * 0.2'
    rotations: 3
    spin: 5
    scale-from: 0.55
    scale-to: 1.2
    easing: ease-in-out

- at: 76
  remove: preview
```

Motions: `ORBIT`, `SPIRAL`, `HELIX`, `RISE`, `FALL`, `BOUNCE`, `FLOAT`, `FLY_TO_PLAYER`,
`CUSTOM`. `CUSTOM` accepts expression fields `x`, `y`, `z` relative to spawn origin.

Easing: `linear`, `ease-in`, `quad-in`, `ease-out`, `quad-out`, `ease-in-out`, `sine`,
`ease-out-back`, `bounce`.

All tracked displays are removed on completion, cancellation, reload, shutdown, or exception.

## Title action

```yaml
- at: 100
  title:
    text: '<gold>{winner}</gold>'
    subtitle: '<gray>{crate}</gray>'
    fade-in: 200
    stay: 1200
    fade-out: 300
```

Placeholders: `{player}`, `{crate}`, `{winner}`. Durations are milliseconds.

## Unsupported

Do not invent commands, camera control, movement locking, nested actions, parameterized macros,
inheritance, string conditions, rarity comparisons, text/image particle shapes, Bézier paths,
signals, or event hooks. They are not part of the current DSL.
