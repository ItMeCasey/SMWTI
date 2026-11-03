package com.itmecasey.smwti.client;

import com.itmecasey.smwti.SmwtiConfig;

/**
 * The colors of the overlay for one theme, as ARGB values with the backdrop's opacity already
 * applied.
 *
 * @param panel   the backdrop
 * @param edge    the border around it
 * @param text    the main text
 * @param sub     the quieter text, such as the mod name
 * @param barBack the empty part of a bar
 * @param shadow  whether text gets a drop shadow (it does on dark backdrops, not on light ones)
 */
public record Palette(int panel, int edge, int text, int sub, int barBack, boolean shadow) {
    /** The palette for the theme and opacity the player chose. */
    public static Palette current() {
        float opacity = SmwtiConfig.opacity();
        return switch (SmwtiConfig.theme()) {
            case DARK -> new Palette(withAlpha(0xC0, opacity, 0x101018), withAlpha(0xFF, opacity, 0x3A3A4C), 0xFFFFFFFF,
                    0xFF8E8EA6, 0xFF2A2A34, true);
            case LIGHT -> new Palette(withAlpha(0xE0, opacity, 0xF0F0F4), withAlpha(0xFF, opacity, 0x9A9AAA), 0xFF202028,
                    0xFF6A6A7A, 0xFFC8C8D4, false);
            case VANILLA -> new Palette(withAlpha(0xF0, opacity, 0x100010), withAlpha(0xF0, opacity, 0x5000FF), 0xFFFFFFFF,
                    0xFFAAAAAA, 0xFF2A1A3A, true);
            case CLEAR -> new Palette(withAlpha(0x50, opacity, 0x000000), 0x00000000, 0xFFFFFFFF, 0xFFCCCCCC, 0x80000000,
                    true);
        };
    }

    /** An RGB color with an alpha of the base value scaled by the opacity. */
    private static int withAlpha(int baseAlpha, float opacity, int rgb) {
        int alpha = Math.max(0, Math.min(255, Math.round(baseAlpha * opacity)));
        return (alpha << 24) | rgb;
    }
}
