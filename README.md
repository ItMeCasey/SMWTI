# Show Me What This Is (SMWTI)

A look-at overlay for Minecraft Forge 26.1.2. Point at a block or an entity and a small panel at the top of the screen tells you what it is, which mod it comes from and, where a mod says so, what state it is in: a crop's growth, a creature's health, a machine's progress.

It exists because the usual overlay mods (Jade, The One Probe) have no Forge build for this Minecraft version, and WTHIT is the only other choice. SMWTI is inspired by them but shares no code with them.

Status: in development (version 2026.11.1). See `PLAN.md`.

## For players

- Install it on the client. It also works on a server that has it, and shows more there: the server sends live data about a block or entity, such as what is in a chest or how full a tank is.
- The overlay shows the target's icon, name and mod, then lines from mods and from SMWTI itself: a crop's growth, a creature's health, the tool that breaks a block, to the right of the name (a green check when you hold the right one, a red cross when the block gives no drops without it), inventories shown as icons with counts, fluid tanks and stored energy on blocks that expose them. Looking at water or lava shows it too.
- Blocks: crop and plant growth, redstone power, a note block's note, composter, cauldron, honey and respawn anchor levels, furnace and brewing stand progress, a spawner's mob, a beacon's level, a hive's bees, a jukebox's disc, a flower pot's plant. Entities: armor, active effects with their time left, riders and mounts, a horse's speed and jump, what an item frame or a dropped item holds, a pet's owner, and how long a young animal takes to grow up or an adult to breed again.
- Sneak for the long form, which lists more (the default; change it in the settings): the registry id, the block's state, more items.
- Themes (dark, light, vanilla, clear, modern; modern is the default) and the backdrop's opacity (80% by default) are in the settings. "Move overlay..." there lets you drag the panel anywhere on the screen; the panel clings softly to the centers and corners, keeps clear of the screen's edges and stays off the hotbar, and the spot is remembered as a fraction of the screen, so it survives window and size changes.
- F10 switches the overlay on and off and Control+F10 opens the settings screen; both keys can be changed in Controls. Settings are also in `config/smwti-client.toml`.

### Resource pack rules

A resource pack can tune the overlay with files at `assets/<namespace>/smwti/rules/<name>.json`:

```json
{
  "hide": ["minecraft:barrier", "#minecraft:beds"],
  "rename": {"minecraft:stone": "Plain Rock"},
  "extra_lines": {"minecraft:furnace": ["Burns what you feed it"]}
}
```

`hide` lists blocks and entity types (ids or `#tags`) the overlay ignores. `rename` gives a block or entity another name, as text or a translation key. `extra_lines` adds lines of text. Ids that name nothing are skipped, so packs can list mods that are not installed.

## For mod authors

Add to the overlay through `com.itmecasey.smwti.api.SmwtiApi`. Call it while your mod loads, from a class that is only loaded when `smwti` is present.

```java
// Client: lines worked out from what the client can see. view.detailed() is true for the long form.
SmwtiApi.registerBlock((tooltip, view) -> {
    if (view.state().is(MyBlocks.BATTERY.get())) {
        tooltip.add(Position.HEAD, Component.literal("Charged"));
    }
});

// Server: lines worked out where the real state lives. They reach the client in view.serverLines().
SmwtiApi.registerServerBlock((tooltip, level, pos, entity, detailed) -> {
    if (entity instanceof BatteryBlockEntity battery) {
        tooltip.bar(Component.literal(battery.energy() + " FE"), battery.fill(), 0xE8963A);
        tooltip.item(new ItemStack(Items.REDSTONE), Component.literal("Signal " + battery.signal()));
    }
});

// The same for entities (registerEntity, registerServerEntity), and overrides for a name or an icon:
SmwtiApi.overrideBlock(new TargetOverride<BlockView>() {
    @Override
    public Component name(BlockView view) {
        return view.state().is(MyBlocks.BATTERY.get()) ? Component.literal("Flux Battery") : null;
    }
});

// Settings for your own lines, listed in the settings screen: a switch, a slider, a cycling choice.
// Name each with the translation key smwti.option.<namespace>.<path>.
Option showRecipe = SmwtiApi.registerOption(Identifier.fromNamespaceAndPath("mymod", "show_recipe"), true);
IntSetting rows = SmwtiApi.registerInt(Identifier.fromNamespaceAndPath("mymod", "rows"), 3, 1, 8);
ChoiceSetting unit = SmwtiApi.registerChoice(Identifier.fromNamespaceAndPath("mymod", "unit"), List.of("fe", "percent"), "fe");

// Draw something yourself (client side): tooltip.element(Position.BODY, new MyArrowElement()).
// Point the overlay at another block (a multiblock part showing its controller):
SmwtiApi.registerRedirect((level, pos, state) -> state.is(MyBlocks.PART.get()) ? controllerOf(pos) : null);
// Run code around the drawing, or hide the overlay for a frame:
SmwtiApi.registerRenderCallback(new RenderCallback() {
    @Override
    public boolean before(Panel panel) {
        return !MyMod.cutsceneRunning();
    }
});
```

Lines go to the badge (right of the name, on the same row), the head (right under the name), the body or the tail. To list item stacks, call `tooltip.stack(Position.BODY, stack)` once per stack: they are drawn as a grid of icons with the count small in the lower right corner and no names. A provider that throws is logged and skipped. Server providers run only for targets within 12 blocks of the player, a few times a second while they look, and only if the server has SMWTI.

## Building

```
gradlew.bat build
```

Needs JDK 21 to run Gradle; the build uses a Java 25 toolchain. The jar lands in `build/libs`.

## License

MIT. Not affiliated with Jade, WTHIT, The One Probe or their authors.
