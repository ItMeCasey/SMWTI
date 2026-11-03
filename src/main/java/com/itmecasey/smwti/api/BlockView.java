package com.itmecasey.smwti.api;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The block the player is looking at, as a client-side provider sees it.
 *
 * @param level       the client's level
 * @param pos         the block's position
 * @param state       the block's state
 * @param entity      the block entity as the client has it, or {@code null} when the block has none
 * @param serverLines what the server-side providers said about this block (see {@link ServerBlockProvider}),
 *                    empty until the first answer arrives
 */
public record BlockView(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity entity, List<Line> serverLines) {
}
