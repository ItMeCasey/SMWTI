package com.itmecasey.smwti.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

/** What a provider can put in a tooltip, and the registry mods hand their providers to. */
class TooltipTest {
    @Test
    void aBarFractionIsKeptBetweenZeroAndOne() {
        Tooltip tooltip = new Tooltip();
        tooltip.bar(Component.literal("over"), 2.5, 0xFF0000);
        tooltip.bar(Component.literal("under"), -3.0, 0xFF0000);
        tooltip.bar(Component.literal("half"), 0.5, 0xFF0000);
        assertEquals(1.0F, tooltip.lines().get(0).fraction());
        assertEquals(0.0F, tooltip.lines().get(1).fraction());
        assertEquals(0.5F, tooltip.lines().get(2).fraction());
    }

    @Test
    void linesAreKeptInOrderAndSplitByPosition() {
        Tooltip tooltip = new Tooltip();
        tooltip.add(Position.TAIL, Component.literal("t"));
        tooltip.add(Position.HEAD, Component.literal("h"));
        tooltip.add(Component.literal("b"));
        assertEquals(3, tooltip.lines().size());
        assertEquals("t", tooltip.lines().get(0).text().getString());
        assertEquals(1, tooltip.lines(Position.HEAD).size());
        assertEquals("b", tooltip.lines(Position.BODY).get(0).text().getString());
        assertEquals(1, tooltip.lines(Position.TAIL).size());
        assertTrue(tooltip.lines(Position.BADGE).isEmpty());
    }

    @Test
    void anEmptyTooltipSaysSo() {
        Tooltip tooltip = new Tooltip();
        assertTrue(tooltip.isEmpty());
        tooltip.add(Component.literal("x"));
        assertFalse(tooltip.isEmpty());
    }

    @Test
    void anElementIsListedUnderItsPositionOnly() {
        Tooltip tooltip = new Tooltip();
        Element element = new Element() {
            @Override
            public int width(Font font) {
                return 1;
            }

            @Override
            public int height(Font font) {
                return 1;
            }

            @Override
            public void draw(GuiGraphics graphics, Font font, int x, int y, int width) {
            }
        };
        tooltip.element(Position.TAIL, element);
        assertEquals(List.of(element), tooltip.elements(Position.TAIL));
        assertTrue(tooltip.elements(Position.BODY).isEmpty());
    }

    @Test
    void registeredProvidersAreKeptInTheOrderTheyCameIn() {
        BlockProvider first = (tooltip, view) -> { };
        BlockProvider second = (tooltip, view) -> { };
        int before = SmwtiApi.blockProviders().size();
        SmwtiApi.registerBlock(first);
        SmwtiApi.registerBlock(second);
        List<BlockProvider> all = SmwtiApi.blockProviders();
        assertSame(first, all.get(before));
        assertSame(second, all.get(before + 1));
    }
}
