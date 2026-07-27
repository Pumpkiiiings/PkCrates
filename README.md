# PkCrates

**PkCrates** is an advanced Crate and Reward management plugin for Minecraft servers, built using SOLID principles, Clean Architecture, and a highly modular design. 
Originally inspired by popular crate systems, it has been redesigned from the ground up to offer superior experience, flexibility, and performance.

## 🌟 Key Features

- **Advanced Rarity System:** A complete ecosystem where rewards belong to rarities. Keys can restrict rarities (e.g., Guaranteed Epic, No Common allowed). Control effects, particles, sounds, and global broadcasts centrally from the rarity settings.
- **In-Game Visual Editors:** No need to touch YAML files to configure your system. You can create and edit Crates, Keys, Holograms, Animations, Rewards, and Rarities through intuitive and interactive GUI menus (supports Shift+Click, Q to drop, etc.).
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
- **Virtual & Physical Keys:** Full database support (SQLite, MySQL, MariaDB) via Repository patterns for storing player data and virtual keys.

## 🔧 Requirements
- **Minecraft Server:** Paper 1.21 or higher.
- **Java:** Version 21 or higher.

## 🚀 Quick Start
1. Join the server and use the command `/crate` or `/crates`.
2. Go to **Crates** and create your first crate by clicking the emerald.
3. Go to **Rarities** to define your server's rarities.
4. Enter your newly created crate's editor to add rewards, link them to rarities, and set the allowed keys.
5. Place a physical block in the world, return to the editor, and use the `Location` option to bind that block to the crate.

---

> **Note for Developers:** This project was developed using a clean architecture model, ensuring high decoupling through services injected via Bukkit Services Manager. Modules can be isolated, enabled, or disabled without breaking the overall ecosystem.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
