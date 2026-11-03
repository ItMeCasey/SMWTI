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

    /** The overlay's colors. */
    public enum Theme {
        DARK, LIGHT, VANILLA, CLEAR
    }

    /** When the long form of the overlay is shown. */
    public enum Detail {
        WHILE_SNEAKING, ALWAYS, NEVER
    }

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.EnumValue<Theme> THEME = BUILDER
            .comment("Colors of the overlay: dark, light, vanilla (like a tooltip) or clear (a faint backdrop).")
            .defineEnum("theme", Theme.DARK);

    private static final ForgeConfigSpec.DoubleValue OPACITY = BUILDER
            .comment("How solid the overlay's backdrop is, from 0.2 (faint) to 1.")
            .defineInRange("opacity", 1.0, 0.2, 1.0);

    private static final ForgeConfigSpec.EnumValue<Detail> DETAIL = BUILDER
            .comment("When providers may show their longer lines: only while sneaking, always, or never.")
            .defineEnum("detail", Detail.WHILE_SNEAKING);

    private static final ForgeConfigSpec.BooleanValue SHOW_FLUIDS = BUILDER
            .comment("Whether looking at water or lava shows it. Turn off to look through fluids at what is under them.")
            .define("showFluids", true);

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

    // The settings screen changes values through these and then writes the file.

    public static void setTheme(Theme value) {
        THEME.set(value);
    }

    public static void setOpacity(double value) {
        OPACITY.set(value);
    }

    public static void setEnabled(boolean value) {
        ENABLED.set(value);
    }

    public static void setAnchor(Anchor value) {
        ANCHOR.set(value);
    }

    public static void setScale(double value) {
        SCALE.set(value);
    }

    public static void setShowModName(boolean value) {
        SHOW_MOD_NAME.set(value);
    }

    public static void setHideWhileSneaking(boolean value) {
        HIDE_WHILE_SNEAKING.set(value);
    }

    public static void setDetail(Detail value) {
        DETAIL.set(value);
    }

    public static void setShowFluids(boolean value) {
        SHOW_FLUIDS.set(value);
    }

    /** The overlay's colors. */
    public static Theme theme() {
        return SPEC.isLoaded() ? THEME.get() : Theme.DARK;
    }

    /** How solid the backdrop is. */
    public static float opacity() {
        return SPEC.isLoaded() ? OPACITY.get().floatValue() : 1.0F;
    }

    /** When providers may show their long form. */
    public static Detail detail() {
        return SPEC.isLoaded() ? DETAIL.get() : Detail.WHILE_SNEAKING;
    }

    /** Writes the settings back to {@code smwti-client.toml}. */
    public static void save() {
        if (SPEC.isLoaded()) {
            SPEC.save();
        }
    }

    /** Whether providers may show their long form for a player who is or is not sneaking. */
    public static boolean detailed(boolean sneaking) {
        return switch (SPEC.isLoaded() ? DETAIL.get() : Detail.WHILE_SNEAKING) {
            case ALWAYS -> true;
            case NEVER -> false;
            case WHILE_SNEAKING -> sneaking;
        };
    }

    /** Whether fluids can be the target. */
    public static boolean showFluids() {
        return !SPEC.isLoaded() || SHOW_FLUIDS.get();
    }

    /** Whether sneaking hides the overlay. */
    public static boolean hideWhileSneaking() {
        return SPEC.isLoaded() && HIDE_WHILE_SNEAKING.get();
    }
}
