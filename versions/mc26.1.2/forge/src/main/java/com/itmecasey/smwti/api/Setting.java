package com.itmecasey.smwti.api;

import net.minecraft.resources.Identifier;

/**
 * A setting a mod adds to SMWTI's settings screen, for one of its own providers. There are three
 * kinds: {@link Option} (on or off), {@link IntSetting} (a whole number in a range) and
 * {@link ChoiceSetting} (one of a few named choices). Register them with the {@code SmwtiApi.register...}
 * methods and ask the setting for its value inside a provider. The player's choices are kept in
 * {@code config/smwti-options.json}, and the screen names a setting with the translation key
 * {@code smwti.option.<namespace>.<path>} (a choice's values use that key plus {@code .<value>}).
 */
public sealed interface Setting permits Option, IntSetting, ChoiceSetting {
    /** The setting's id, in the registering mod's namespace. */
    Identifier id();

    /** The translation key of the setting's name in the settings screen. */
    default String translationKey() {
        return "smwti.option." + id().getNamespace() + "." + id().getPath();
    }
}
