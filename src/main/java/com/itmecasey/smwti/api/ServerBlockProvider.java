package com.itmecasey.smwti.api;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Works out lines about a block on the server, where its real state lives, and sends them to the
 * player looking at it. It is asked a few times a second per player while they look, and only for
 * blocks with a block entity within a few blocks of them. Keep it cheap and read-only.
 */
@FunctionalInterface
public interface ServerBlockProvider {
    /**
     * Adds the lines for the block entity at the position; add nothing when this provider does not know it.
     *
     * @param detailed whether the player asked for the long form
     */
    void collect(Tooltip tooltip, ServerLevel level, BlockPos pos, BlockEntity entity, boolean detailed);
}
