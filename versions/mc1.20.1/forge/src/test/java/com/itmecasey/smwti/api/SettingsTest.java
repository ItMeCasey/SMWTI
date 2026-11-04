package com.itmecasey.smwti.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.OptionalDouble;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class SettingsTest {
    private static ResourceLocation id(String path) {
        return new ResourceLocation("testmod", path);
    }

    @Test
    void anOptionIsItsDefaultUntilChosen() {
        Option option = new Option(id("flag_a"), true);
        assertTrue(option.enabled());
        SettingValues.set(id("flag_a"), false);
        assertFalse(option.enabled());
    }

    @Test
    void aSliderStaysInItsRange() {
        IntSetting rows = new IntSetting(id("rows"), 3, 1, 8);
        assertEquals(3, rows.value());
        SettingValues.set(id("rows"), 99);
        assertEquals(8, rows.value());
        SettingValues.set(id("rows"), -4);
        assertEquals(1, rows.value());
    }

    @Test
    void aSliderRefusesAnEmptyRange() {
        assertThrows(IllegalArgumentException.class, () -> new IntSetting(id("bad"), 1, 5, 2));
    }

    @Test
    void aChoiceFallsBackWhenTheSavedValueIsGone() {
        ChoiceSetting unit = new ChoiceSetting(id("unit"), List.of("fe", "percent"), "fe");
        SettingValues.set(id("unit"), "joules");
        assertEquals("fe", unit.value());
        SettingValues.set(id("unit"), "percent");
        assertEquals("percent", unit.value());
        assertEquals("smwti.option.testmod.unit.percent", unit.choiceKey("percent"));
    }

    @Test
    void aChoiceNeedsItsDefaultAmongTheChoices() {
        assertThrows(IllegalArgumentException.class, () -> new ChoiceSetting(id("odd"), List.of("a"), "b"));
        assertThrows(IllegalArgumentException.class, () -> new ChoiceSetting(id("none"), List.of(), "a"));
    }

    @Test
    void progressIsAFractionOfTheTimeNeeded() {
        assertEquals(OptionalDouble.of(0.25), Progress.ratio(50, 200));
        assertEquals(OptionalDouble.of(1.0), Progress.ratio(300, 200));
        assertEquals(OptionalDouble.empty(), Progress.ratio(0, 200));
        assertEquals(OptionalDouble.empty(), Progress.ratio(10, 0));
    }
}
