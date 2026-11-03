package com.itmecasey.smwti.api;

import net.minecraft.resources.Identifier;

/**
 * An on-off setting: "show the machine's recipe", say. Register it with
 * {@link SmwtiApi#registerOption} and ask {@link #enabled()} in the provider.
 *
 * @param id           the option's id, in the registering mod's namespace
 * @param defaultValue whether it is on until the player decides
 */
public record Option(Identifier id, boolean defaultValue) implements Setting {
    /** Whether the option is on right now. */
    public boolean enabled() {
        return SettingValues.get(id) instanceof Boolean chosen ? chosen : defaultValue;
    }
}
