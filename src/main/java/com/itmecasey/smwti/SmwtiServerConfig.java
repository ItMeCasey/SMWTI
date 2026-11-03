package com.itmecasey.smwti;

import java.util.List;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * The server's say over what the overlay may show, stored in the world's
 * {@code serverconfig/smwti-server.toml}. It applies to everyone on the server: a pack maker can
 * stop the server from answering at all, shorten how far away a player may ask, keep inventories
 * private (so a chest's contents cannot be read from across a room) and list blocks and entities it
 * never answers about.
 */
public final class SmwtiServerConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue ANSWER = BUILDER
            .comment("Set to false and the server never sends lines about blocks or entities. The overlay still shows what the client knows by itself.")
            .define("answerQuestions", true);

    private static final ForgeConfigSpec.IntValue REACH = BUILDER
            .comment("How far away, in blocks, a player may be from a block or entity and still get lines about it.")
            .defineInRange("reach", 12, 2, 64);

    private static final ForgeConfigSpec.BooleanValue INVENTORIES = BUILDER
            .comment("Whether the overlay lists what is in chests, furnaces and other containers.")
            .define("showInventories", true);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLACKLIST = BUILDER
            .comment("Blocks, block entity types and entity types the server never sends lines about, by id: [\"minecraft:ender_chest\", \"minecraft:villager\"].")
            .defineListAllowEmpty("blacklist", List.<String>of(), value -> value instanceof String);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private SmwtiServerConfig() {
    }

    /** Whether the server answers questions at all. */
    public static boolean answer() {
        return !SPEC.isLoaded() || ANSWER.get();
    }

    /** The farthest a player may be and still ask, in blocks. */
    public static int reach() {
        return SPEC.isLoaded() ? REACH.get() : 12;
    }

    /** Whether containers' contents are listed. */
    public static boolean showInventories() {
        return !SPEC.isLoaded() || INVENTORIES.get();
    }

    /** Whether the server never answers about the thing with this id. */
    public static boolean blacklisted(String id) {
        return SPEC.isLoaded() && BLACKLIST.get().contains(id);
    }
}
