package com.itmecasey.smwti.api;

import java.util.OptionalDouble;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Reads how far a machine has got, as a fraction from 0 to 1, from its block entity. Return
 * {@link OptionalDouble#empty()} while the machine is idle. See {@link CommonRegistration#progress}.
 *
 * @param <T> the block entity type the source understands
 */
@FunctionalInterface
public interface ProgressSource<T extends BlockEntity> {
    /** The fraction done, or empty when there is nothing in progress. */
    OptionalDouble progress(T entity);
}
