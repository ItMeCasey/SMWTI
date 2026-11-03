package com.itmecasey.smwti.net;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

/**
 * What a question or an answer is about: a block at a position, or an entity by its id in the
 * level. Exactly one of the two is meaningful, as {@link #entity()} says.
 *
 * @param entity   whether the target is an entity
 * @param pos      the block's position (ignored for an entity)
 * @param entityId the entity's id in the level (ignored for a block)
 */
public record LookTarget(boolean entity, BlockPos pos, int entityId) {
    /** A block target. */
    public static LookTarget block(BlockPos pos) {
        return new LookTarget(false, pos, 0);
    }

    /** An entity target. */
    public static LookTarget entity(int id) {
        return new LookTarget(true, BlockPos.ZERO, id);
    }

    void write(FriendlyByteBuf buf) {
        buf.writeBoolean(entity);
        if (entity) {
            buf.writeVarInt(entityId);
        } else {
            buf.writeBlockPos(pos);
        }
    }

    static LookTarget read(FriendlyByteBuf buf) {
        return buf.readBoolean() ? entity(buf.readVarInt()) : block(buf.readBlockPos());
    }
}
