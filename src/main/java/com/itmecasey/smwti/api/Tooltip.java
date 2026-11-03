package com.itmecasey.smwti.api;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;

/**
 * The lines a provider adds to the overlay, below the name and the mod. Providers only ever add;
 * the overlay decides how the lines are laid out.
 */
public final class Tooltip {
    private final List<Line> lines = new ArrayList<>();

    /** Adds a line of text. */
    public Tooltip add(Component text) {
        lines.add(Line.of(text));
        return this;
    }

    /** Adds a line of text with a bar under it, {@code fraction} full (clamped to 0 to 1) in the given RGB color. */
    public Tooltip bar(Component text, double fraction, int color) {
        lines.add(new Line(text, (float) Math.max(0.0, Math.min(1.0, fraction)), color));
        return this;
    }

    /** Adds a line built elsewhere, such as one a server sent. */
    public Tooltip add(Line line) {
        lines.add(line);
        return this;
    }

    /** The lines so far, in the order they were added. */
    public List<Line> lines() {
        return lines;
    }

    /** Whether nothing has been added. */
    public boolean isEmpty() {
        return lines.isEmpty();
    }
}
