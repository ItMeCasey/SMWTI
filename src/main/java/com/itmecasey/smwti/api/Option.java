package com.itmecasey.smwti.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.Identifier;

/**
 * An on-off setting a mod adds to SMWTI's settings screen, for one of its own providers: "show the
 * machine's recipe", say. Register it with {@link SmwtiApi#registerOption} and ask {@link #enabled()}
 * in the provider. The player's choice is kept in {@code config/smwti-options.json}, and the screen
 * names it with the translation key {@code smwti.option.<namespace>.<path>}.
 *
 * @param id           the option's id, in the registering mod's namespace
 * @param defaultValue whether it is on until the player decides
 */
public record Option(Identifier id, boolean defaultValue) {
    /** The player's choices by option id; written by the client, never by a mod. */
    private static final Map<Identifier, Boolean> CHOICES = new ConcurrentHashMap<>();

    /** Whether the option is on right now. */
    public boolean enabled() {
        return CHOICES.getOrDefault(id, defaultValue);
    }

    /** The translation key of the option's name in the settings screen. */
    public String translationKey() {
        return "smwti.option." + id.getNamespace() + "." + id.getPath();
    }

    /** Records the player's choice; called by the settings screen and the file loader. */
    public static void choose(Identifier id, boolean value) {
        CHOICES.put(id, value);
    }

    /** Every recorded choice, for saving. */
    public static Map<Identifier, Boolean> choices() {
        return CHOICES;
    }
}
