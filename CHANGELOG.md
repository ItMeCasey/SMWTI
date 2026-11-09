# Changelog

## 2026.11.4 (NeoForge 1.20.1 only)

Fixes the NeoForge 1.20.1 jar, which crashed a real game. The other nine builds are unchanged (Forge 1.20.1 stays at 2026.11.2).

- NeoForge 1.20.1: the jar was built with NeoGradle 7, which leaves Minecraft's development names in the jar. A development run hides that, but a real Forge or NeoForge 1.20.1 game only knows Forge's own names and would stop with `NoSuchFieldError` or `NoSuchMethodError`. The module now builds with ForgeGradle against Forge 47.1.47 and reobfuscates the jar to those names (467 Forge-name references in the class files, none before). The mod class has the plain Forge 47 constructor.
- Loaded in a real Forge 47.4.28 install and a real NeoForge 47.1.106 install: `Show Me What This Is loaded (Forge 1.20.1)` and the main menu was reached with no linkage errors.

## 2026.11.3

Adds Minecraft 26.2 and 26.3 for Forge and NeoForge, so SMWTI now comes for 26.3, 26.2, 26.1.2, 1.21.1 and 1.20.1. The six older builds are unchanged and stay at 2026.11.2. Each new build was run in a client and checked on a block, an entity, a fluid and a dropped item.

- 26.2 and 26.3 renamed several client calls (screen, toasts, chat, HUD hiding, flat world presets); 26.3 also changed key types, sign text access and the NeoForge config types. SMWTI follows them.

## 2026.11.2

Adds NeoForge builds and Forge 1.20.1 and NeoForge 1.20.1, so SMWTI now comes for Forge and NeoForge on 26.1.2, 1.21.1 and 1.20.1. Every build was run in a client and checked on a block, an entity, a fluid and a dropped item.

- Dropped items now show in the overlay (name, enchantments, attack lines); the game never lets the crosshair pick them, so SMWTI looks for them along the look ray. Item frames show their item's enchantments too.
- The block id, block state and entity id lines are readable now (they were dark gray on the dark panel and the colon in `minecraft:stone` disappeared).
- A line position this version does not know (from a mod built for a newer SMWTI) falls back to the body instead of breaking the whole reply.
- NeoForge 1.21.1: the fluid name now comes from the supported call.
- Unit tests for the line codec and the tooltip API.

## 2026.11.1

First version, for Forge 26.1.2 and 1.21.1 (the 1.21.1 build has no shelf lines, and paintings take their title and artist from the game's language file).

- The overlay: the icon, name and mod of the block or entity you look at, with lines from mods and from SMWTI itself, a tool badge beside the name (a green check when you hold the right tool, a red cross when the block gives no drops without it), item stacks as icons with counts, bars, and the long form while sneaking.
- Vanilla lines for plant growth, redstone power, composters, cauldrons, hives, furnaces, brewing stands, spawners, beacons, jukeboxes, lecterns, signs, shelves, heads, campfires, enchanting power, locked hoppers, unstable TNT, waxed blocks, armor, effects, riders, horses, villagers, pets, young animals and eggs, item frames, paintings, display entities, dropped items, and the inventory, tanks and energy of any block that exposes them.
- Themes (dark, light, vanilla, clear, modern), opacity, size, drag-and-drop placement with snapping, hold-to-show, compact mode, a hide key and list, fluids as targets.
- A plugin system (`@SmwtiPlugin`) and a public API for other mods; resource packs can add themes and hide, rename or add lines; a server config for reach, private inventories and a blacklist.
