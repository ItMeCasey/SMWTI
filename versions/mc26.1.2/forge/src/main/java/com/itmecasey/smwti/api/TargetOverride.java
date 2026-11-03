package com.itmecasey.smwti.api;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Replaces what the overlay calls a target or draws as its icon. Each method answers
 * {@code null} to leave the default alone; the first registered override with an answer wins.
 *
 * @param <T> the view the override looks at: a {@link BlockView} or an {@link EntityView}
 */
public interface TargetOverride<T> {
    /** The name to show, or {@code null} for the default name. */
    default @Nullable Component name(T view) {
        return null;
    }

    /** The item to draw as the icon, or {@code null} for the default icon. */
    default @Nullable ItemStack icon(T view) {
        return null;
    }
}
