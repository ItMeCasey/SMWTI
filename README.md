# Show Me What This Is (SMWTI)

A look-at overlay for Minecraft Forge 26.1.2. Point at a block or an entity and a small panel at the top of the screen tells you what it is, which mod it comes from and, where a mod says so, what state it is in: a crop's growth, a creature's health, a machine's progress.

It exists because the usual overlay mods (Jade, The One Probe) have no Forge build for this Minecraft version, and WTHIT is the only other choice. SMWTI is inspired by them but shares no code with them.

Status: in development (version 2026.11.1). See `PLAN.md`.

## For players

- Install it on the client. It also works on a server that has it, and shows more there: the server sends live data about a block or entity, such as what is in a chest or how full a tank is.
- The overlay shows the target's icon, name and mod, then lines from mods and from SMWTI itself: a crop's growth, a creature's health, the tool that breaks a block, to the right of the name (a green check when you hold the right one, a red cross when the block gives no drops without it), inventories shown as icons with counts, fluid tanks and stored energy on blocks that expose them. Looking at water or lava shows it too.
- Blocks: crop and plant growth, redstone power, a note block's note, composter, cauldron, honey and respawn anchor levels, furnace and brewing stand progress, a spawner's mob, a beacon's level, a hive's bees, a jukebox's disc, a flower pot's plant. Entities: armor, active effects with their time left, riders and mounts, a horse's speed and jump, what an item frame or a dropped item holds, a pet's owner, and how long a young animal takes to grow up or an adult to breed again.
- Also: enchanting power around an enchanting table, locked hoppers, unstable TNT, waxed blocks and signs, shelves, player heads, signs' text, command block commands (a server setting, off by default), a spawner's countdown, the next egg a chicken lays, paintings' title and artist, a zombie villager being cured, pet armor, falling blocks and display entities by what they show, and a dropped item's tooltip in the long form.
- Sneak for the long form, which lists more (the default; change it in the settings): the registry id, the block's state, more items.
- Hold-to-show (Left Alt, switched on in the settings), a compact mode, and Shift+F10 to hide what you are looking at, with a list in the settings to show things again.
- Themes (dark, light, vanilla, clear, modern; modern is the default) and the backdrop's opacity (80% by default) are in the settings. "Move overlay..." there lets you drag the panel anywhere on the screen; the panel clings softly to the centers and corners, keeps clear of the screen's edges and stays off the hotbar, and the spot is remembered as a fraction of the screen, so it survives window and size changes.
- F10 switches the overlay on and off and Control+F10 opens the settings screen; both keys can be changed in Controls. Settings are also in `config/smwti-client.toml`.

### Server settings

`serverconfig/smwti-server.toml` in the world lets a server stop answering, shorten the reach, keep inventories private, show command block commands (off by default) and blacklist blocks and entities by id.

### Resource pack themes and rules

A resource pack can tune the overlay with files at `assets/<namespace>/smwti/rules/<name>.json`:

```json
{
  "hide": ["minecraft:barrier", "#minecraft:beds"],
  "rename": {"minecraft:stone": "Plain Rock"},
  "extra_lines": {"minecraft:furnace": ["Burns what you feed it"]}
}
```

A pack can also add a theme with a file at `assets/<namespace>/smwti/themes/<name>.json` (colors as `#RRGGBB`, alphas 0 to 255, every field optional: `panel`, `panel_alpha`, `edge`, `edge_alpha`, `text`, `sub`, `bar_back`, `shadow`, `accent`); it shows up in the settings as `<namespace>:<name>`.

`hide` lists blocks and entity types (ids or `#tags`) the overlay ignores. `rename` gives a block or entity another name, as text or a translation key. `extra_lines` adds lines of text. Ids that name nothing are skipped, so packs can list mods that are not installed.

## For mod authors: writing a plugin

A plugin is a class marked `@SmwtiPlugin` that implements `ISmwtiPlugin`. SMWTI finds it by scanning the installed mods, so your mod never calls into SMWTI and nothing of the plugin is loaded unless SMWTI is there. Put another mod's id in the annotation to load the plugin only when that mod is installed.

Compile against the API jar (`gradlew apiJar` in this repository builds it; it holds only the `com.itmecasey.smwti.api` package) and list `smwti` as an optional dependency in your `mods.toml`:

```groovy
dependencies {
    compileOnly files('libs/smwti-forge-26.1.2-2026.11.1-api.jar')
}
```

```java
@SmwtiPlugin            // or @SmwtiPlugin("create") to load only when Create is installed
public class MyPlugin implements ISmwtiPlugin {
    /** Runs on the client and on a dedicated server: what the server works out. */
    @Override
    public void register(CommonRegistration registration) {
        registration.serverBlock((tooltip, level, pos, entity, detailed) -> {
            if (entity instanceof BatteryBlockEntity battery) {
                tooltip.bar(Component.literal(battery.energy() + " FE"), battery.fill(), 0xE8963A);
                tooltip.stack(Position.BODY, new ItemStack(Items.REDSTONE, battery.signal()));
            }
        });
    }

    /** Runs on the client only: what draws. */
    @Override
    public void registerClient(ClientRegistration registration) {
        registration.block((tooltip, view) -> {
            if (view.state().is(MyBlocks.BATTERY.get())) {
                tooltip.add(Position.HEAD, Component.literal("Charged"));
            }
        });
        registration.overrideBlock(new TargetOverride<BlockView>() {
            @Override
            public Component name(BlockView view) {
                return view.state().is(MyBlocks.BATTERY.get()) ? Component.literal("Flux Battery") : null;
            }
        });
        Option showRecipe = registration.option(Identifier.fromNamespaceAndPath("mymod", "show_recipe"), true);
        registration.block((tooltip, view) -> { if (showRecipe.enabled()) { /* ... */ } });
    }
}
```

What a plugin can do:

| Call | What it does |
|---|---|
| `serverBlock`, `serverEntity` | Lines worked out on the server; they reach the client in `view.serverLines()`. |
| `block`, `entity` | Lines worked out on the client. `view.detailed()` is true for the long form (sneaking, by default). |
| `overrideBlock`, `overrideEntity` | Another name or icon for a target. |
| `redirect` | Show another block than the one under the crosshair (a multiblock part showing its controller). |
| `renderCallback` | Run before and after the overlay is drawn; answer `false` from `before` to skip a frame. |
| `option`, `intSetting`, `choice` | A switch, a slider or a cycling choice in the settings screen, saved for the player. Name them with `smwti.option.<namespace>.<path>`. |

Lines go to the badge (right of the name, same row), the head (under the name), the body or the tail. `tooltip.add` adds text, `bar` a bar under a line, `item` a small icon before text, `stack` an icon with its count in the corner and no name (stacks added one after another form a grid), and `element(position, element)` something you draw yourself (client side only). A plugin that throws is logged and skipped. Server providers run only for targets within the reach the server allows (12 blocks by default) and only if the server has SMWTI.

You can also register without a plugin class, by calling the static methods of `SmwtiApi` while your mod loads.

## Building

```
gradlew.bat build
```

Needs JDK 21 to run Gradle; the build uses a Java 25 toolchain. The jar lands in `build/libs`.

## License

MIT. Not affiliated with Jade, WTHIT, The One Probe or their authors.
