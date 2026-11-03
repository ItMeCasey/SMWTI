# Show Me What This Is (SMWTI)

A look-at overlay for Minecraft Forge 26.1.2. Point at a block or an entity and a small panel at the top of the screen tells you what it is, which mod it comes from and, where a mod says so, what state it is in: a crop's growth, a creature's health, a machine's progress.

It exists because the usual overlay mods (Jade, The One Probe) have no Forge build for this Minecraft version, and WTHIT is the only other choice. SMWTI is inspired by them but shares no code with them.

Status: version 0.1, in development. See `PLAN.md`.

## For players

- Install it on the client. It also works on a server that has it, and shows more there: mods can send live data about a block, such as a machine's progress.
- Settings are in `config/smwti-client.toml`: switch the overlay off, move it to the left, center or right, scale it, hide the mod name, hide it while sneaking.

## For mod authors

Add lines to the overlay through `com.itmecasey.smwti.api.SmwtiApi`. Call it while your mod loads, from a class that is only loaded when `smwti` is present.

```java
// On the client: lines worked out from what the client can see.
SmwtiApi.registerBlock((tooltip, view) -> {
    if (view.state().is(MyBlocks.BATTERY.get())) {
        tooltip.add(Component.literal("Charged"));
    }
});

// On the server: lines worked out where the real state lives. They reach the client in view.serverLines().
SmwtiApi.registerServerBlock((tooltip, level, pos, entity) -> {
    if (entity instanceof BatteryBlockEntity battery) {
        tooltip.bar(Component.literal(battery.energy() + " FE"), battery.fill(), 0xE8963A);
    }
});

SmwtiApi.registerEntity((tooltip, entity) -> { /* ... */ });
```

A provider that throws is logged and skipped. Server providers run only for block entities within 12 blocks of the player, a few times a second while they look.

## Building

```
gradlew.bat build
```

Needs JDK 21 to run Gradle; the build uses a Java 25 toolchain. The jar lands in `build/libs`.

## License

MIT. Not affiliated with Jade, WTHIT, The One Probe or their authors.
