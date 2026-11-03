package com.itmecasey.smwti.api;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Runs around the drawing of the overlay, for a mod that wants to hide it for a moment or draw
 * something next to it.
 */
public interface RenderCallback {
    /** What is about to be drawn and where. */
    record Panel(Component name, int x, int y, int width, int height) {
    }

    /** Called before the panel is drawn; answer {@code false} to skip it this frame. */
    default boolean before(Panel panel) {
        return true;
    }

    /** Called after the panel is drawn, with the same graphics, so a mod can add to the picture. */
    default void after(GuiGraphics graphics, Panel panel) {
    }
}
