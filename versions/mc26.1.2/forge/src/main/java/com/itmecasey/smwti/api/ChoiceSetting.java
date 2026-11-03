package com.itmecasey.smwti.api;

import java.util.List;
import net.minecraft.resources.Identifier;

/**
 * A setting with a few named choices, shown as a button that cycles through them: whether to show
 * energy as FE or as a percentage, say. Register it with {@link SmwtiApi#registerChoice} and ask
 * {@link #value()} in the provider.
 *
 * @param id           the setting's id, in the registering mod's namespace
 * @param choices      the allowed values, as short lowercase names; at least one
 * @param defaultValue the value until the player decides; one of the choices
 */
public record ChoiceSetting(Identifier id, List<String> choices, String defaultValue) implements Setting {
    public ChoiceSetting {
        choices = List.copyOf(choices);
        if (choices.isEmpty() || !choices.contains(defaultValue)) {
            throw new IllegalArgumentException("the default must be one of the choices");
        }
    }

    /** The current value, always one of the choices. */
    public String value() {
        return SettingValues.get(id) instanceof String chosen && choices.contains(chosen) ? chosen : defaultValue;
    }

    /** The translation key of one choice's label. */
    public String choiceKey(String choice) {
        return translationKey() + "." + choice;
    }
}
