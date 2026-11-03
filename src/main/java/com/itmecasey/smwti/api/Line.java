package com.itmecasey.smwti.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

/**
 * One line of the overlay: some text and, optionally, a bar under it. A bar has a fill between
 * 0 and 1 and a color; a plain text line has no bar ({@link #NO_BAR}).
 *
 * @param text     what the line says
 * @param fraction how full the bar is, from 0 to 1, or {@link #NO_BAR}
 * @param color    the bar's RGB color; ignored without a bar
 */
public record Line(Component text, float fraction, int color) {
    /** The fill of a line that draws no bar. */
    public static final float NO_BAR = -1.0F;

    /** How a line travels from the server to the client. */
    public static final Codec<Line> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ComponentSerialization.CODEC.fieldOf("text").forGetter(Line::text),
            Codec.FLOAT.optionalFieldOf("fraction", NO_BAR).forGetter(Line::fraction),
            Codec.INT.optionalFieldOf("color", 0xFFFFFF).forGetter(Line::color)
    ).apply(instance, Line::new));

    /** A line of plain text. */
    public static Line of(Component text) {
        return new Line(text, NO_BAR, 0xFFFFFF);
    }

    /** Whether the line draws a bar. */
    public boolean hasBar() {
        return fraction >= 0.0F;
    }
}
