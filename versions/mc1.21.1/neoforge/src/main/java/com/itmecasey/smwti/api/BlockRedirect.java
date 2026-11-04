package com.itmecasey.smwti.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Points the overlay at another block than the one under the crosshair, so a part of a
 * multiblock can show its controller, say. The first registered redirect that answers wins, and
 * the answer is used for everything: the name, the icon, the providers and the server's lines.
 */
@FunctionalInterface
public interface BlockRedirect {
    /** The position to show instead, or {@code null} to leave the target alone. */
    @Nullable BlockPos redirect(Level level, BlockPos pos, BlockState state);
}
