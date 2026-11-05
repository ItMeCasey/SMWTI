package com.itmecasey.smwti;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The client configuration, stored in {@code smwti-client.toml}: how the overlay looks for this
 * player only. A server never reads it.
 */
public final class SmwtiConfig {
    /** Where on the screen the overlay sits. */
    public enum Anchor {
        TOP_LEFT, TOP_CENTER, TOP_RIGHT, CUSTOM
    }

    /** When the long form of the overlay is shown. */
    public enum Detail {
        WHILE_SNEAKING, ALWAYS, NEVER
    }

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.ConfigValue<String> THEME = BUILDER
            .comment("Colors of the overlay: dark, light, vanilla (like a tooltip), clear (a faint backdrop), modern (flat and square with an orange line on top), or the id of a theme a resource pack adds, such as mypack:neon.")
            .define("theme", "modern");

    private static final ModConfigSpec.DoubleValue OPACITY = BUILDER
            .comment("How solid the overlay's backdrop is, from 0.2 (faint) to 1.")
            .defineInRange("opacity", 0.8, 0.2, 1.0);

    private static final ModConfigSpec.EnumValue<Detail> DETAIL = BUILDER
            .comment("When providers may show their longer lines: only while sneaking, always, or never.")
            .defineEnum("detail", Detail.WHILE_SNEAKING);

    private static final ModConfigSpec.BooleanValue HOLD_TO_SHOW = BUILDER
            .comment("Show the overlay only while the hold key is down (Left Alt by default, changeable in Controls).")
            .define("holdToShow", false);

    private static final ModConfigSpec.BooleanValue COMPACT = BUILDER
            .comment("A smaller overlay: no mod name, no head or tail lines, tighter spacing.")
            .define("compact", false);

    private static final ModConfigSpec.BooleanValue SHOW_FLUIDS = BUILDER
            .comment("Whether looking at water or lava shows it. Turn off to look through fluids at what is under them.")
            .define("showFluids", true);

    private static final ModConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("Set to false to hide the overlay.")
            .define("enabled", true);

    private static final ModConfigSpec.EnumValue<Anchor> ANCHOR = BUILDER
            .comment("Which part of the top of the screen the overlay hangs from. CUSTOM uses positionX and positionY, which the settings screen sets when you drag the overlay.")
            .defineEnum("anchor", Anchor.TOP_CENTER);

    private static final ModConfigSpec.DoubleValue POSITION_X = BUILDER
            .comment("With the CUSTOM anchor: how far across the screen the overlay sits, 0 at the left edge, 1 at the right.")
            .defineInRange("positionX", 0.5, 0.0, 1.0);

    private static final ModConfigSpec.DoubleValue POSITION_Y = BUILDER
            .comment("With the CUSTOM anchor: how far down the screen the overlay sits, 0 at the top edge, 1 at the bottom.")
            .defineInRange("positionY", 0.0, 0.0, 1.0);

    private static final ModConfigSpec.DoubleValue SCALE = BUILDER
            .comment("Size of the overlay, 1 is normal.")
            .defineInRange("scale", 1.0, 0.5, 2.0);

    private static final ModConfigSpec.BooleanValue SHOW_MOD_NAME = BUILDER
            .comment("Whether the name of the mod a block or entity comes from is shown under its name.")
            .define("showModName", true);

    private static final ModConfigSpec.BooleanValue HIDE_WHILE_SNEAKING = BUILDER
            .comment("Hide the overlay while sneaking, for building close to things.")
            .define("hideWhileSneaking", false);

    public static final ModConfigSpec SPEC = BUILDER.build();

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

    public static void setTheme(String name) {
        THEME.set(name);
    }

    public static void setOpacity(double value) {
        OPACITY.set(value);
    }

    public static void setHoldToShow(boolean value) {
        HOLD_TO_SHOW.set(value);
    }

    public static void setCompact(boolean value) {
        COMPACT.set(value);
    }

    public static void setEnabled(boolean value) {
        ENABLED.set(value);
    }

    /** Puts the overlay at a custom spot: fractions of the room it has, 0 to 1 across and down. */
    public static void setPosition(double x, double y) {
        POSITION_X.set(Math.max(0.0, Math.min(1.0, x)));
        POSITION_Y.set(Math.max(0.0, Math.min(1.0, y)));
        ANCHOR.set(Anchor.CUSTOM);
    }

    /** How far across the free width the custom position is. */
    public static double positionX() {
        return SPEC.isLoaded() ? POSITION_X.get() : 0.5;
    }

    /** How far down the free height the custom position is. */
    public static double positionY() {
        return SPEC.isLoaded() ? POSITION_Y.get() : 0.0;
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
    public static String theme() {
        return SPEC.isLoaded() ? THEME.get() : "modern";
    }

    /** How solid the backdrop is. */
    public static float opacity() {
        return SPEC.isLoaded() ? OPACITY.get().floatValue() : 0.8F;
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

    /** Whether the overlay shows only while the hold key is down. */
    public static boolean holdToShow() {
        return SPEC.isLoaded() && HOLD_TO_SHOW.get();
    }

    /** Whether the smaller overlay is used. */
    public static boolean compact() {
        return SPEC.isLoaded() && COMPACT.get();
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
