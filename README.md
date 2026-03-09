# Sands of Time

A **Minecraft 1.21.1 NeoForge** addon mod for [Relics](https://modrinth.com/mod/relics-mod) that adds the **Entropic Hourglass** — a time-manipulation relic discovered through archaeology.

## Overview

Sands of Time adds a single, deep relic to the Relics mod: the **Entropic Hourglass**. Found buried in desert structures, this ancient artifact passively absorbs temporal energy and lets you spend it to accelerate blocks and entities in the world.

- **Accelerate machines** — furnaces, hoppers, brewing stands, and any block with a tile entity ticker
- **Speed up nature** — crops, saplings, and any block with random ticks
- **Overclock entities** — speed up animal growth, mob AI, and more

## Requirements

| Dependency | Version |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.219+ |
| [Relics](https://modrinth.com/mod/relics-mod) | 0.10.7.8 |
| [OctoLib](https://modrinth.com/mod/octo-lib) | 0.6.0.1+1.21 |
| [Curios API](https://modrinth.com/mod/curios) | 9.3.1+1.21.1 |

## The Entropic Hourglass

**Curio Slot:** Charm · **Max Level:** 15 · **Obtained via:** Desert Pyramid & Desert Well archaeology (~5% chance)

### Abilities

#### ⏳ Chrono-Accumulation (Passive — Unlocked at Level 0)
Passively absorbs temporal energy while the hourglass is in any inventory slot (including curio). Stored time is displayed as a HUD overlay and fuels all active abilities. Accumulation rate scales with ability level.

#### ⚡ Temporal Injection (Active — Unlocked at Level 3)
Right-click a block to inject temporal energy, accelerating it for 30 seconds. Works on:
- **Block entity tickers** — furnaces, hoppers, brewing stands, spawners, etc.
- **Randomly ticking blocks** — crops, saplings, sugar cane, cactus, etc.

Cycle through speed multipliers (2x → 4x → 8x → ... → 512x) by clicking the same block. Higher speeds cost more stored time. The maximum speed scales with ability level (4x at level 0, up to 256x at max level).

#### 🧬 Bio-Overclock (Active — Unlocked at Level 8)
Right-click a living entity to overclock its biological processes for 30 seconds. Accelerates mob AI, animal growth, and entity ticking. Re-clicking extends duration and can upgrade speed. Requires line of sight. Cannot target players.

### Progression
- Leveling is handled by the Relics framework — XP is earned by using each ability
- Each ability has its own research stars to unlock
- Speed multiplier cap and accumulation rate improve as you level up

## Configuration

The mod provides a config screen (accessible through the Mods menu) with:
- **Block Blacklist** — prevent specific blocks from being accelerated
- **Entity Blacklist** — prevent specific entities from being overclocked
- **HUD Mode** — choose between `ACTION_BAR`, `TOP_LEFT`, or `DISABLED` for the stored time display

## Building from Source

**Requirements:** JDK 21

```bash
./gradlew build
```

The output JAR will be in `build/libs/`.

## License

This project is licensed under the [MIT License](LICENSE).
