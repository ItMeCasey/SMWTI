package com.itmecasey.smwti.devtest;

import com.itmecasey.smwti.Smwti;
import com.itmecasey.smwti.SmwtiConfig;
import com.itmecasey.smwti.client.SettingsScreen;
import com.itmecasey.smwti.client.UserHidden;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;

/**
 * A development-only camera tour: it opens a fresh creative superflat world, builds a few things
 * to look at (a chest, a furnace, a crop, water, a cow), points the camera at each and saves a
 * screenshot into {@code run/screenshots} as {@code smwti_<name>.png}, then quits. It does nothing
 * unless the environment variable {@code SMWTI_SHOTS} is set.
 *
 * <p>It runs on a desktop someone may be using, so it stays out of the way: it lets go of the mouse
 * every tick, keeps rendering while unfocused, and the script that starts it mutes the game.
 * Progress goes to the log as {@code SMWTI-SHOTS} lines.
 */
@EventBusSubscriber(modid = Smwti.MODID, value = Dist.CLIENT)
public final class ShotTour {
    private static final boolean ENABLED = System.getenv("SMWTI_SHOTS") != null;
    private static final String WORLD = "smwti_shots";
    /** Ticks for the overlay to ask the server and get its answer, and for the chunk renderer to catch up. */
    private static final int SETTLE = 40;

    private enum Stage { WAITING_FOR_MENU, LOADING_WORLD, TOURING, DONE }

    /** One thing to do, after waiting the given number of ticks. */
    private record Step(int delay, Runnable action) {
    }

    private static final Deque<Step> STEPS = new ArrayDeque<>();
    private static Stage stage = Stage.WAITING_FOR_MENU;
    private static int idle;
    private static int wait;
    /** The first air block above the superflat ground. */
    private static int ground;
    private static boolean pauseOnLostFocus;

    private ShotTour() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!ENABLED || stage == Stage.DONE) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.mouseHandler.isMouseGrabbed()) {
            minecraft.mouseHandler.releaseMouse();
        }
        switch (stage) {
            case WAITING_FOR_MENU -> waitForMenu(minecraft);
            case LOADING_WORLD -> waitForWorld(minecraft);
            case TOURING -> runSteps();
            default -> {
            }
        }
    }

    /** Once loading has finished and a menu is up, creates the world (replacing an old one). */
    private static void waitForMenu(Minecraft minecraft) {
        if (minecraft.gui.overlay() != null || minecraft.level != null || minecraft.gui.screen() == null || ++idle < 20) {
            return;
        }
        pauseOnLostFocus = minecraft.options.pauseOnLostFocus;
        minecraft.options.pauseOnLostFocus = false;

        LevelStorageSource source = minecraft.getLevelSource();
        if (source.levelExists(WORLD)) {
            try (LevelStorageSource.LevelStorageAccess access = source.createAccess(WORLD)) {
                access.deleteLevel();
            } catch (IOException e) {
                Smwti.LOGGER.warn("SMWTI-SHOTS could not delete the old world", e);
            }
        }
        LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE,
                new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT);
        stage = Stage.LOADING_WORLD;
        idle = 0;
        Smwti.LOGGER.info("SMWTI-SHOTS creating world");
        minecraft.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(0L, false, false),
                registries -> registries.lookupOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), new TitleScreen());
    }

    /** Waits for the player to stand in the world, then plans the tour. */
    private static void waitForWorld(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null || minecraft.gui.screen() != null
                || minecraft.getSingleplayerServer() == null || ++idle < 40) {
            return;
        }
        ground = minecraft.level.getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0);
        Smwti.LOGGER.info("SMWTI-SHOTS in the world, ground at y={}", ground);
        planTour();
        stage = Stage.TOURING;
    }

    private static void runSteps() {
        if (wait > 0) {
            wait--;
            return;
        }
        Step step = STEPS.poll();
        if (step == null) {
            return;
        }
        step.action().run();
        Step next = STEPS.peek();
        wait = next == null ? 0 : next.delay();
    }

    // --- The tour ----------------------------------------------------------------------------

    private static void planTour() {
        then(0, () -> {
            server("gamerule advance_time false", "gamerule advance_weather false", "time set noon", "weather clear",
                    "kill @e[type=!player]");
            LocalPlayer player = player();
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
            Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON);
            if (Minecraft.getInstance().gui.hud.isHidden()) {
                Minecraft.getInstance().gui.hud.toggle();
            }
        });

        // A chest with a few stacks, short form then long form.
        then(2, () -> server(
                at("setblock %d %d %d minecraft:chest", 0, 0),
                at("item replace block %d %d %d container.0 with minecraft:iron_ingot 32", 0, 0),
                at("item replace block %d %d %d container.1 with minecraft:diamond 5", 0, 0),
                at("item replace block %d %d %d container.2 with minecraft:cobblestone 64", 0, 0),
                at("item replace block %d %d %d container.3 with minecraft:oak_log 12", 0, 0),
                at("item replace block %d %d %d container.4 with minecraft:apple 3", 0, 0),
                at("item replace block %d %d %d container.5 with minecraft:bread 7", 0, 0),
                at("item replace block %d %d %d container.6 with minecraft:coal 20", 0, 0)));
        lookAt(0);
        then(2, () -> SmwtiConfig.setDetail(SmwtiConfig.Detail.ALWAYS));
        shoot("chest_long", SETTLE);
        then(2, () -> SmwtiConfig.setDetail(SmwtiConfig.Detail.NEVER));
        shoot("chest_short", SETTLE);

        // A furnace, which only has an inventory.
        then(2, () -> server(at("setblock %d %d %d minecraft:furnace[facing=south]", 4, 0),
                at("item replace block %d %d %d container.0 with minecraft:raw_iron 16", 4, 0)));
        lookAt(4);
        shoot("furnace", SETTLE);

        // A ripe crop and one that is half grown, on farmland.
        then(2, () -> server(
                at("setblock %d %d %d minecraft:farmland", 8, -1),
                at("setblock %d %d %d minecraft:wheat[age=4]", 8, 0)));
        lookAt(8);
        shoot("wheat", SETTLE);

        // Water, looked at straight down so the crosshair goes into the surface.
        then(2, () -> server(at("setblock %d %d %d minecraft:water", 12, -1)));
        then(4, () -> moveTo(new Vec3(12.5, ground + 2.5, 0.5), new Vec3(12.5, ground - 0.5, 0.5)));
        shoot("water", SETTLE);

        // A cow, looked at from the side at its body. Long form, so the entity id shows.
        then(2, () -> {
            SmwtiConfig.setDetail(SmwtiConfig.Detail.ALWAYS);
            server(at("summon minecraft:cow %d.5 %d %d.5 {NoAI:1b}", 16, 0));
        });
        then(4, () -> moveTo(new Vec3(16.5, ground + 1.3, -2.0), new Vec3(16.5, ground + 0.9, 0.5)));
        shoot("cow", SETTLE);

        // The test resource pack: its theme and its rules (a rename and an extra line).
        then(2, () -> SmwtiConfig.setTheme("smwti:neon"));
        shoot("pack_theme_cow", SETTLE);
        then(2, () -> SmwtiConfig.setTheme("modern"));

        // The overlay switched off with the toggle key's handler, then back on.
        then(2, () -> SmwtiConfig.setEnabled(false));
        shoot("toggled_off", SETTLE);
        then(2, () -> SmwtiConfig.setEnabled(true));

        // Hold to show with no key held: the overlay must stay away.
        then(2, () -> SmwtiConfig.setHoldToShow(true));
        shoot("hold_not_held", SETTLE);
        then(2, () -> SmwtiConfig.setHoldToShow(false));

        // Hidden with the hide key's handler, on the chest.
        lookAt(0);
        then(2, () -> UserHidden.add("minecraft:chest"));
        shoot("hidden_chest", SETTLE);
        then(2, () -> UserHidden.remove("minecraft:chest"));

        // Vanilla lines still unseen: campfire, beacon, armor stand, a dropped enchanted sword and iron ore.
        then(2, () -> server(
                at("setblock %d %d %d minecraft:campfire[lit=true]", 20, 0),
                at("setblock %d %d %d minecraft:beacon", 24, 0),
                at("summon minecraft:armor_stand %d.5 %d %d.5 {equipment:{head:{id:\"minecraft:diamond_helmet\",count:1},mainhand:{id:\"minecraft:iron_sword\",count:1}}}", 28, 0),
                at("summon minecraft:item %d.5 %d %d.5 {Item:{id:\"minecraft:diamond_sword\",count:1,components:{\"minecraft:enchantments\":{\"minecraft:sharpness\":5}}}}", 32, 1),
                at("setblock %d %d %d minecraft:iron_ore", 36, 0),
                String.format(Locale.ROOT, "setblock 40 %d 1 minecraft:stone", ground),
                at("summon minecraft:item_frame %d.5 %d %d.5 {facing:\"north\",Item:{id:\"minecraft:diamond_sword\",count:1,components:{\"minecraft:enchantments\":{\"minecraft:sharpness\":5}}}}", 40, 0)));
        then(2, () -> SmwtiConfig.setDetail(SmwtiConfig.Detail.ALWAYS));
        lookDownAt(20);
        shoot("campfire", SETTLE);
        lookDownAt(24);
        shoot("beacon", SETTLE);
        lookBodyAt(28, 1.0);
        shoot("armor_stand", SETTLE);
        lookBodyAt(32, 0.2);
        shoot("enchanted_sword", SETTLE);
        lookDownAt(36);
        shoot("iron_ore", SETTLE);
        lookBodyAt(40, 0.5);
        shoot("item_frame", SETTLE);
        then(2, () -> SmwtiConfig.setDetail(SmwtiConfig.Detail.WHILE_SNEAKING));

        // The settings screen.
        then(2, () -> {
            SmwtiConfig.setDetail(SmwtiConfig.Detail.WHILE_SNEAKING);
            Minecraft.getInstance().gui.setScreen(new SettingsScreen(null));
        });
        shoot("settings", 10);

        // The position screen and the hidden-things screen, with an empty list and with one entry.
        then(2, () -> Minecraft.getInstance().gui.setScreen(new com.itmecasey.smwti.client.PositionScreen(null)));
        shoot("position", 10);
        then(2, () -> {
            for (String id : new java.util.ArrayList<>(UserHidden.all())) {
                UserHidden.remove(id);
            }
            Minecraft.getInstance().gui.setScreen(new com.itmecasey.smwti.client.HiddenScreen(null));
        });
        shoot("hidden_empty", 10);
        then(2, () -> {
            UserHidden.add("minecraft:chest");
            Minecraft.getInstance().gui.setScreen(new com.itmecasey.smwti.client.HiddenScreen(null));
        });
        shoot("hidden_list", 10);
        then(2, () -> UserHidden.remove("minecraft:chest"));

        then(20, () -> {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.options.pauseOnLostFocus = pauseOnLostFocus;
            Smwti.LOGGER.info("SMWTI-SHOTS DONE");
            stage = Stage.DONE;
            minecraft.stop();
        });
    }

    /** Puts the camera two blocks south of the thing at this x and a little above, looking at it. */
    private static void lookAt(int x) {
        then(4, () -> {
            Minecraft.getInstance().gui.setScreen(null);
            moveTo(new Vec3(x + 0.5, ground + 1.3, -1.6), new Vec3(x + 0.5, ground + 0.4, 0.5));
        });
    }

    /** Straight down onto the top of the block at this x, so the crosshair lands on that block. */
    private static void lookDownAt(int x) {
        then(4, () -> moveTo(new Vec3(x + 0.5, ground + 4.0, 0.5), new Vec3(x + 0.5, ground + 0.5, 0.5)));
    }

    /** Side-on at a height above the ground line, so the crosshair lands on a body at that height. */
    private static void lookBodyAt(int x, double height) {
        then(4, () -> moveTo(new Vec3(x + 0.5, ground + 1.5, -2.0), new Vec3(x + 0.5, ground + height, 0.5)));
    }

    /** Like {@link #lookAt} but from higher up, for something below the ground line. */
    private static void lookAtDown(int x) {
        then(4, () -> moveTo(new Vec3(x + 0.5, ground + 1.8, -1.2), new Vec3(x + 0.5, ground - 0.5, 0.5)));
    }

    private static void moveTo(Vec3 eye, Vec3 target) {
        Vec3 d = target.subtract(eye);
        float yaw = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = (float) -(Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * Mth.RAD_TO_DEG);
        server(String.format(Locale.ROOT, "tp @a %.3f %.3f %.3f %.2f %.2f", eye.x, eye.y - 1.62, eye.z, yaw, pitch));
        LocalPlayer player = player();
        player.snapTo(eye.x, eye.y - 1.62, eye.z, yaw, pitch);
        player.setYHeadRot(yaw);
        player.setDeltaMovement(Vec3.ZERO);
        // Fly, so the camera does not sink onto the ground after the teleport and miss its target.
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
    }

    /** A command with the position of a block on the ground line at this x filled in; {@code up} raises it. */
    private static String at(String command, int x, int up) {
        return String.format(Locale.ROOT, command, x, ground + up, 0);
    }

    /** What the crosshair is on, as a block position or an entity id and its position. */
    private static String describeHit(net.minecraft.world.phys.HitResult hit) {
        if (hit instanceof net.minecraft.world.phys.BlockHitResult block) {
            return "block " + block.getBlockPos();
        }
        if (hit instanceof net.minecraft.world.phys.EntityHitResult entity) {
            return "entity " + entity.getEntity().getType() + " at " + entity.getEntity().position();
        }
        return String.valueOf(hit);
    }

    private static void shoot(String name, int delay) {
        then(delay, () -> {
            Minecraft minecraft = Minecraft.getInstance();
            // Command echoes and recipe toasts would only clutter the shot.
            minecraft.gui.hud.getChat().clearMessages(false);
            minecraft.gui.toastManager().clear();
        });
        then(2, () -> {
            Minecraft minecraft = Minecraft.getInstance();
            LocalPlayer self = minecraft.player;
            Smwti.LOGGER.info("SMWTI-SHOTS {} eye={} feet={},{},{} pitch={} yaw={} hit={}", name, self.getEyePosition(),
                    self.getX(), self.getY(), self.getZ(), self.getXRot(), self.getYRot(), describeHit(minecraft.hitResult));
            Screenshot.grab(minecraft.gameDirectory, "smwti_" + name + ".png", minecraft.gameRenderer.mainRenderTarget(), 1,
                    message -> Smwti.LOGGER.info("SMWTI-SHOTS saved {}", name));
        });
    }

    private static void then(int delay, Runnable action) {
        STEPS.add(new Step(delay, action));
    }

    private static LocalPlayer player() {
        return Minecraft.getInstance().player;
    }

    private static void server(String... commands) {
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server != null) {
            server.execute(() -> {
                for (String command : commands) {
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
                }
            });
        }
    }
}
