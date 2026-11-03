package com.itmecasey.smwti.api;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.item.ItemStack;

/**
 * The lines a provider adds to the overlay. Providers only ever add; the overlay decides how the
 * parts are laid out. Every method without a {@link Position} adds to the body.
 */
public final class Tooltip {
    private final List<Line> lines = new ArrayList<>();
    private final List<PlacedElement> elements = new ArrayList<>();

    /** An element and the part of the overlay it goes in. */
    public record PlacedElement(Position position, Element element) {
    }

    /** Adds a line of text to the body. */
    public Tooltip add(Component text) {
        return add(Position.BODY, text);
    }

    /** Adds a line of text. */
    public Tooltip add(Position position, Component text) {
        lines.add(Line.of(text).at(position));
        return this;
    }

    /** Adds a line of text with a bar under it to the body; see {@link #bar(Position, Component, double, int)}. */
    public Tooltip bar(Component text, double fraction, int color) {
        return bar(Position.BODY, text, fraction, color);
    }

    /** Adds a line of text with a bar under it, {@code fraction} full (clamped to 0 to 1) in the given RGB color. */
    public Tooltip bar(Position position, Component text, double fraction, int color) {
        lines.add(new Line(text, (float) Math.max(0.0, Math.min(1.0, fraction)), color, position, "", 1));
        return this;
    }

    /**
     * Adds a bar filled with a fluid's own texture and tint, with the text over it: the level of a tank.
     * {@code fraction} is how full it is, clamped to 0 to 1.
     */
    public Tooltip fluidBar(Position position, Component text, double fraction, Fluid fluid) {
        String id = BuiltInRegistries.FLUID.getKey(fluid).toString();
        lines.add(new Line(text, (float) Math.max(0.0, Math.min(1.0, fraction)), 0xFFFFFF, position, "", 1, "", 0xFFFFFF, id));
        return this;
    }

    /** Adds a line of text with a small icon of the stack before it, to the body. */
    public Tooltip item(ItemStack stack, Component text) {
        return item(Position.BODY, stack, text);
    }

    /** Adds a line of text with a small icon of the stack before it. Only the item and its count are used. */
    public Tooltip item(Position position, ItemStack stack, Component text) {
        String id = stack.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        lines.add(new Line(text, Line.NO_BAR, 0xFFFFFF, position, id, stack.getCount()));
        return this;
    }

    /**
     * Adds an item stack to list: just its icon with the count small in the lower right corner, no name.
     * Stacks added one after another are laid out side by side in a grid.
     */
    public Tooltip stack(Position position, ItemStack stack) {
        return item(position, stack, Component.empty());
    }

    /**
     * Adds an item stack to list with a short mark in its lower right corner in place of the count,
     * such as a check in green: {@code stack(Position.BADGE, tool, "✔", 0x55FF55)}.
     */
    public Tooltip stack(Position position, ItemStack stack, String mark, int markColor) {
        String id = stack.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        lines.add(new Line(Component.empty(), Line.NO_BAR, 0xFFFFFF, position, id, stack.getCount(), mark, markColor));
        return this;
    }

    /**
     * Adds something the mod draws itself, after the lines of that part of the overlay. Client-side
     * providers only: an element cannot be sent from the server.
     */
    public Tooltip element(Position position, Element element) {
        elements.add(new PlacedElement(position, element));
        return this;
    }

    /** The elements of one part of the overlay, in the order they were added. */
    public List<Element> elements(Position position) {
        return elements.stream().filter(placed -> placed.position() == position).map(PlacedElement::element).toList();
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

    /** The lines of one part of the overlay, in the order they were added. */
    public List<Line> lines(Position position) {
        return lines.stream().filter(line -> line.position() == position).toList();
    }

    /** Whether nothing has been added. */
    public boolean isEmpty() {
        return lines.isEmpty();
    }
}
