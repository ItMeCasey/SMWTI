package com.itmecasey.smwti.api;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Something a mod draws itself inside the overlay: a progress arrow, a small diagram, a row of
 * icons. It exists only on the client, so it is added by client-side providers (not sent from the
 * server); a server provider sends the numbers and the client provider draws them.
 *
 * <p>The overlay asks for the size first and then draws the element in a box of that size, with
 * the top left corner at the given point.
 */
public interface Element {
    /** The width the element needs, in pixels. */
    int width(Font font);

    /** The height the element needs, in pixels. */
    int height(Font font);

    /**
     * Draws the element.
     *
     * @param width the room the overlay has for it, which is at least {@link #width}
     */
    void draw(GuiGraphics graphics, Font font, int x, int y, int width);
}
