package com.itmecasey.smwti.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ThemesTest {
    @Test
    void colorsAreReadAsHex() {
        assertEquals(0xFF8A1F, Themes.parse("#FF8A1F"));
        assertEquals(0, Themes.parse("#000000"));
    }

    @Test
    void anEmptyColorMeansNone() {
        assertEquals(0, Themes.parse(""));
    }

    @Test
    void badColorsAreRejected() {
        assertEquals(-1, Themes.parse("FF8A1F"));
        assertEquals(-1, Themes.parse("#FF8A1"));
        assertEquals(-1, Themes.parse("#GGGGGG"));
    }

    @Test
    void theBuiltInThemesAreListedFirst() {
        assertEquals("dark", Themes.names().get(0));
        assertEquals("modern", Themes.BUILT_IN.get(Themes.BUILT_IN.size() - 1));
    }
}
