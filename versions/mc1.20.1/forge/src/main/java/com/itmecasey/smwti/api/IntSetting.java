package com.itmecasey.smwti.api;

import net.minecraft.resources.ResourceLocation;

/**
 * A whole-number setting shown as a slider: how many items to list, say. Register it with
 * {@link SmwtiApi#registerInt} and ask {@link #value()} in the provider.
 *
 * @param id           the setting's id, in the registering mod's namespace
 * @param defaultValue the value until the player decides
 * @param min          the smallest value
 * @param max          the largest value
 */
public record IntSetting(ResourceLocation id, int defaultValue, int min, int max) implements Setting {
    public IntSetting {
        if (min > max) {
            throw new IllegalArgumentException("min " + min + " is above max " + max);
        }
    }

    /** The current value, always within the range. */
    public int value() {
        int chosen = SettingValues.get(id) instanceof Integer number ? number : defaultValue;
        return Math.max(min, Math.min(max, chosen));
    }
}
