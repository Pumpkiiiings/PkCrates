# PkCrates

**PkCrates** is an advanced Crate and Reward management plugin for Minecraft servers, built using SOLID principles, Clean Architecture, and a highly modular design. 
Originally inspired by popular crate systems, it has been redesigned from the ground up to offer superior experience, flexibility, and performance.

## 🌟 Key Features

- **Advanced Rarity System:** A complete ecosystem where rewards belong to rarities. Keys can restrict rarities (e.g., Guaranteed Epic, No Common allowed). Control effects, particles, sounds, and global broadcasts centrally from the rarity settings.
- **In-Game Visual Editors:** No need to touch YAML files to configure your system. You can create and edit Crates, Keys, Holograms, Animations, Rewards, and Rarities through intuitive and interactive GUI menus (supports Shift+Click, Q to drop, etc.).
- **Configurable Effects:** Particles, sounds and fireworks are defined in config, not code. Nine particle shapes (`POINT`, `CIRCLE`, `SPHERE`, `HELIX`, `VORTEX`, `BURST`, `BEAM`, `STAR`, `WAVE`) can be layered at each stage of an opening — globally, per crate, or per rarity. See [Effects](#-effects).
- **Epic Animations:** Enjoy seamless and smooth crate opening animations powered by asynchronous tasks and modern Display Entities. Available animations include:
  - `ROULETTE` (Classic Roulette)
  - `CSGO` (CS:GO style slide)
  - `BLACKHOLE` (Orbital items sucked into a portal)
  - `SPIRAL` (Items ascending in a magical spiral)
  - `METEOR` (Fireball crashing from the sky)
  - `FOUNTAIN` (Items shooting upwards like a geyser)
  - `PORTAL` (Nether-portal style spawning)
- **Claim System (Safeguard):** Players will never lose their rewards. If their inventory is full or they disconnect during an animation, the system persistently stores their prize until they claim it using `/crate claim`.
- **Audit & Webhooks:** Internally logs everything that happens in the plugin (key generation, crate openings, rewards won) to local files, the console, and remotely to Discord via Webhooks. Ensures maximum security and tracking for administrators.
- **Modern TextDisplay Holograms:** Extremely efficient holograms utilizing the Minecraft 1.20+ `TextDisplay` API. Allows customizing scale, billboard, shadow, and background colors. Supports dynamic Rarity System placeholders (e.g., `<rarity_id_display>`).
- **MiniMessage & HEX Color Support:** Fully supports modern MiniMessage formatting (`<gradient>`, `<rainbow>`, `<#ff00ff>`) as well as legacy `&#RRGGBB` HEX codes everywhere (items, holograms, chat, etc.).
- **Safe Hot-Reloading:** Make changes to the configuration and apply them safely with `/crate reload` without completely disabling the plugin, avoiding ClassLoader memory leaks and server lag.
- **Virtual & Physical Keys:** Virtual key balances are stored in an embedded SQLite database behind a connection pool, with all queries off the main thread. Pending claims are stored as per-player YAML files, buffered in memory and flushed in the background.

## ✨ Effects

Each line is `key:value` pairs in any order. Missing keys fall back to a default, unknown
keys are ignored, and invalid lines are reported once at load and then skipped.

```yaml
effects:
  ambient:
    # Repeats five times per second while a player is within max-distance.
    # Ambient bundles accept particles only.
    - 'particle:ENCHANT shape:CIRCLE radius:0.8 height:0.5 count:6'
  on-open:
    - 'particle:ELECTRIC_SPARK shape:VORTEX radius:1.4 height:3.0 count:80'
    - 'sound:ENTITY_LIGHTNING_BOLT_THUNDER volume:0.6 pitch:1.8'
  on-reward:
    - 'particle:DUST color:#FFEE55 size:1.6 shape:STAR radius:1.5 count:50'
    - 'firework:FFEE55 type:STAR'
  on-claim-stored:
    - 'sound:BLOCK_CHEST_CLOSE volume:0.8 pitch:0.8'
```

| Shape | Look |
|-------|------|
| `POINT` | Single spot (default) |
| `CIRCLE` | Flat horizontal ring |
| `SPHERE` | Hollow sphere, evenly spread |
| `HELIX` | Two strands twisting upward |
| `VORTEX` | Spiral tightening as it rises |
| `BURST` | Scatter — pair with `speed:` for an explosion |
| `BEAM` | Vertical column |
| `STAR` | Pentagram outline |
| `WAVE` | Three concentric rings |

**Where it can go**

- `config.yml` under `effects:` — the global default.
- A crate file under `effects:` — **replaces** the global bundle for that trigger, so a
  crate that only defines `on-open` still uses global `ambient` and `on-reward`.
- `rarities.yml` under `effects.list` — plays **in addition** to the crate's, layered on top
  of the existing `particle` / `sound` / `firework-color` fields, which keep working.

Particle counts are capped at 500 per line so a mistyped `count:` cannot stall the server.
Ambient rendering runs every four ticks but skips unloaded chunks and crates with no player
within `effects.ambient-settings.max-distance` (48 blocks by default). A crate-specific
`effects.ambient` takes precedence over the preset selected by `ambient-effect`; that preset
takes precedence over the global ambient bundle. Sounds and fireworks are rejected from
ambient bundles because repeating either five times per second is unsafe.
`REDSTONE`/`DUST`, `TOTEM`/`TOTEM_OF_UNDYING`, `SMOKE`/`SMOKE_NORMAL` and
`VILLAGER_HAPPY`/`HAPPY_VILLAGER` are interchangeable, so configs written against older
Bukkit naming still load.

The crate-opening title now comes from `messages.yml` (`crate.opening-title` /
`crate.opening-subtitle`); blank both to disable it.

## 🎬 Scripted animations

Advanced animations live in `plugins/PkCrates/animations/*.yml`; reusable particle, sound,
and firework components live in `plugins/PkCrates/effects.yml`. The bundled
`SCRIPTED_EXAMPLE` demonstrates parallel tracks, deterministic random previews, expressions,
display spawning, orbit/rise movement, easing, effects, titles, and cleanup.

```yaml
id: ZEUS_SCRIPT
duration: 120
variables:
  radius: 1.4
tracks:
  effects:
    - at: 0
      use: electric-opening
    - at: 100
      use: legendary-reveal
  displays:
    - at: 5
      spawn: {id: preview, item: RANDOM_REWARD, offset: [0, 0.2, 0], scale: 0.55}
    - during: 5..75
      animate:
        id: preview
        motion: ORBIT
        radius: radius
        height: '0.5 + sin(time * 0.12) * 0.2'
        rotations: 3
        spin: 5
        easing: ease-in-out
    - at: 76
      remove: preview
```

Supported motions are `ORBIT`, `SPIRAL`, `HELIX`, `RISE`, `FALL`, `BOUNCE`, `FLOAT`,
`FLY_TO_PLAYER`, and expression-driven `CUSTOM`. Expressions are sandboxed arithmetic—not
JavaScript—and expose only documented numeric variables and functions. Definitions are parsed,
resolved, and budget-checked on load; malformed scripts are rejected while built-in Java
animations remain available as fallbacks. Script ids cannot replace built-in ids.

For AI-assisted authoring, load the project skill
`.agents/skills/pkcrates-animation-author/SKILL.md`. Its schema enumerates every implemented
field and explicitly prohibits inventing unsupported syntax.

## 🔄 Migrating from PhoenixCrates

```
/crate migrate phoenix              # dry run — reports everything, writes nothing
/crate migrate phoenix confirm      # writes, skipping crates that already exist
/crate migrate phoenix overwrite    # writes, replacing existing crates
```

Scans `plugins/PhoenixCrates/`, `plugins/PhoenixCratesLite/` and
`plugins/PkCrates/migration/input/` — including each one's `crates/` and `keys/` subfolders.
Both editions are read when both are present. Requires `pkcrates.admin.migrate`.

Files are classified by content, not by folder or file name, so dropping crate and key ymls
together into the input folder works. This matters because PhoenixCrates names key files
after a timestamp (`key_1776813321157.yml`) while crates reference keys by their
`identifier` field — the migrator keys off `identifier` so the links survive.

**The migration is not lossless, and the report tells you exactly where.** Fields with no
PkCrates equivalent (money cost, cooldowns, guaranteed-win counters, alternative rewards,
per-reward permission restrictions, idle particle effects) are dropped and listed rather
than silently discarded. Three things always need a human afterwards:

- **Custom items.** `custom:<id>` references belong to a third-party item plugin. Those that
  are vanilla behind the prefix (`custom:golden_apple`) resolve automatically; the rest become
  `BARRIER` with the original preserved under `migrated-material` so you can find and fix them.
- **Reward odds.** PhoenixCrates rolls each reward against its own percentage, so the
  percentages can total well over 100%. PkCrates picks one reward by relative weight. The
  numbers are copied over, which preserves the ordering but not the absolute odds.
- **Crate blocks.** PkCrates binds a crate to a placed block — run `/crate setlocation <id>`
  afterwards. Keys are migrated automatically when their files are in the source folders;
  a linked key with no source file gets a TRIPWIRE_HOOK placeholder instead.

## 🔧 Requirements
- **Minecraft Server:** Paper 1.21 or higher.
- **Java:** Version 21 or higher.

## 💾 Storage

| Data | Backend | Notes |
|------|---------|-------|
| Virtual keys | SQLite (`database.db`) | HikariCP pool, single connection, WAL mode |
| Pending claims | YAML (`claims/<uuid>.yml`) | In-memory cache, async atomic writes |
| Crates / keys / rarities / menus | YAML | Reloadable via `/crate reload` |

Persistence sits behind the `ClaimRepository` port and `DatabaseManager`, so a MySQL or
MariaDB adapter can be added without touching the service layer. Those adapters are not
implemented yet — `SqliteClaimRepository` is a documented skeleton.

## 🚀 Quick Start
1. Join the server and use the command `/crate` or `/crates`.
2. Go to **Crates** and create your first crate by clicking the emerald.
3. Go to **Rarities** to define your server's rarities.
4. Enter your newly created crate's editor to add rewards, link them to rarities, and set the allowed keys.
5. Place a physical block in the world, return to the editor, and use the `Location` option to bind that block to the crate.

## 🛠 Building

```bash
./gradlew build
```

The jar lands in `build/libs/` at roughly 390 KB. The wrapper pins the Gradle version and
the `toolchain` block pins Java 21, so no local Gradle or `JAVA_HOME` setup is required
beyond having a JDK 21 available for Gradle to discover.

### Runtime libraries

HikariCP and the SQLite driver are **not bundled**. `PkCratesLoader` declares them through
Paper's `MavenLibraryResolver`, so on load the server checks its shared `libraries/` cache
and downloads only what is missing — with Maven checksum verification.

- First start on a fresh server: one download (~13 MB), then cached permanently.
- Later starts, and other plugins needing the same artifacts: no download, no duplicate copies.
- **Offline hosts** with an empty cache will fail to load with a resolver error. Pre-seed
  `libraries/`, or switch the two `compileOnly` entries in `build.gradle.kts` back to
  `implementation` and re-add the shadow plugin to bundle them.

---

> **Note for Developers:** This project was developed using a clean architecture model, ensuring high decoupling through services injected via Bukkit Services Manager. Modules can be isolated, enabled, or disabled without breaking the overall ecosystem.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
