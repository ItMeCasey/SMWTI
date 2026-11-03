package com.itmecasey.smwti.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * One line of the overlay: some text, optionally a bar under it, optionally a small item icon
 * before it. A bar has a fill between 0 and 1 and a color; a plain text line has no bar
 * ({@link #NO_BAR}). An item line carries only the item and a count, never its components, so it
 * travels from the server as a few bytes.
 *
 * @param text     what the line says
 * @param fraction how full the bar is, from 0 to 1, or {@link #NO_BAR}
 * @param color    the bar's RGB color; ignored without a bar
 * @param position which part of the overlay the line belongs to
 * @param item     the registry name of the icon's item, or an empty string for no icon
 * @param count    how many of the item the icon shows
 */
public record Line(Component text, float fraction, int color, Position position, String item, int count) {
    /** The fill of a line that draws no bar. */
    public static final float NO_BAR = -1.0F;

    /** How a line travels from the server to the client. */
    public static final Codec<Line> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ComponentSerialization.CODEC.fieldOf("text").forGetter(Line::text),
            Codec.FLOAT.optionalFieldOf("fraction", NO_BAR).forGetter(Line::fraction),
            Codec.INT.optionalFieldOf("color", 0xFFFFFF).forGetter(Line::color),
            Codec.STRING.xmap(Position::valueOf, Position::name).optionalFieldOf("position", Position.BODY)
                    .forGetter(Line::position),
            Codec.STRING.optionalFieldOf("item", "").forGetter(Line::item),
            Codec.INT.optionalFieldOf("count", 1).forGetter(Line::count)
    ).apply(instance, Line::new));

    /** A line of plain text in the body. */
    public static Line of(Component text) {
        return new Line(text, NO_BAR, 0xFFFFFF, Position.BODY, "", 1);
    }

    /** Whether the line draws a bar. */
    public boolean hasBar() {
        return fraction >= 0.0F;
    }

    /** Whether the line is only an item icon with its count, as {@link Tooltip#stack} adds it. */
    public boolean isStack() {
        return !item.isEmpty() && text.getString().isEmpty() && !hasBar();
    }

    /** The icon's item stack, or {@link ItemStack#EMPTY} when the line has none or the item is unknown. */
    public ItemStack icon() {
        if (item.isEmpty()) {
            return ItemStack.EMPTY;
        }
        Identifier id = Identifier.tryParse(item);
        Item found = id == null ? null : BuiltInRegistries.ITEM.getValue(id);
        return found == null ? ItemStack.EMPTY : new ItemStack(found, Math.max(1, count));
    }

    /** The same line moved to another part of the overlay. */
    public Line at(Position where) {
        return new Line(text, fraction, color, where, item, count);
    }
}
