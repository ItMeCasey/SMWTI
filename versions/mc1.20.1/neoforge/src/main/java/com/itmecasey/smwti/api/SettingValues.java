package com.itmecasey.smwti.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;

/**
 * The player's choices for the settings mods register (see {@link Setting}), by setting id. A value
 * is a {@link Boolean}, an {@link Integer} or a {@link String} depending on the kind of setting.
 * The client fills this from {@code config/smwti-options.json} and the settings screen; mods never
 * write to it, they ask their {@link Setting}.
 */
public final class SettingValues {
    private static final Map<ResourceLocation, Object> VALUES = new ConcurrentHashMap<>();

    private SettingValues() {
    }

    /** The recorded value, or {@code null} when the player has not chosen. */
    static Object get(ResourceLocation id) {
        return VALUES.get(id);
    }

    /** Records the player's choice; called by the settings screen and the file loader. */
    public static void set(ResourceLocation id, Object value) {
        VALUES.put(id, value);
    }

    /** Every recorded choice, for saving. */
    public static Map<ResourceLocation, Object> all() {
        return VALUES;
    }
}
