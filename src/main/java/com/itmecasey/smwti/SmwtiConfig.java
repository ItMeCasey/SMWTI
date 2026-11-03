package com.itmecasey.smwti;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * The client configuration, stored in {@code smwti-client.toml}: how the overlay looks for this
 * player only. A server never reads it.
 */
public final class SmwtiConfig {
    /** Where on the screen the overlay sits. */
    public enum Anchor {
        TOP_LEFT, TOP_CENTER, TOP_RIGHT
    }

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("Set to false to hide the overlay.")
            .define("enabled", true);

    private static final ForgeConfigSpec.EnumValue<Anchor> ANCHOR = BUILDER
            .comment("Which part of the top of the screen the overlay hangs from.")
            .defineEnum("anchor", Anchor.TOP_CENTER);

    private static final ForgeConfigSpec.DoubleValue SCALE = BUILDER
            .comment("Size of the overlay, 1 is normal.")
            .defineInRange("scale", 1.0, 0.5, 2.0);

    private static final ForgeConfigSpec.BooleanValue SHOW_MOD_NAME = BUILDER
            .comment("Whether the name of the mod a block or entity comes from is shown under its name.")
            .define("showModName", true);

    private static final ForgeConfigSpec.BooleanValue HIDE_WHILE_SNEAKING = BUILDER
            .comment("Hide the overlay while sneaking, for building close to things.")
            .define("hideWhileSneaking", false);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private SmwtiConfig() {
    }

    /** Whether the overlay is shown at all. */
    public static boolean enabled() {
        return !SPEC.isLoaded() || ENABLED.get();
    }

    /** Which part of the top of the screen the overlay hangs from. */
    public static Anchor anchor() {
        return SPEC.isLoaded() ? ANCHOR.get() : Anchor.TOP_CENTER;
    }

    /** The overlay's size factor. */
    public static float scale() {
        return SPEC.isLoaded() ? SCALE.get().floatValue() : 1.0F;
    }

    /** Whether the mod's name shows under the target's name. */
    public static boolean showModName() {
        return !SPEC.isLoaded() || SHOW_MOD_NAME.get();
    }

    /** Whether sneaking hides the overlay. */
    public static boolean hideWhileSneaking() {
        return SPEC.isLoaded() && HIDE_WHILE_SNEAKING.get();
    }
}
