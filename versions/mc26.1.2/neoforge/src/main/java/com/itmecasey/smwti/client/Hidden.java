package com.itmecasey.smwti.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;

/**
 * The blocks and entities the overlay never shows, from the resource pack rules read by
 * {@link PackRules} and the player's own list ({@link UserHidden}). A hidden target is treated as if the player were looking at nothing.
 */
public final class Hidden {
    private Hidden() {
    }

    /** Whether the overlay stays away from this block. */
    public static boolean block(Block block) {
        return PackRules.hides(block.builtInRegistryHolder()) || UserHidden.contains(BuiltInRegistries.BLOCK.getKey(block).toString());
    }

    /** Whether the overlay stays away from this kind of entity. */
    public static boolean entity(EntityType<?> type) {
        return PackRules.hides(type.builtInRegistryHolder()) || UserHidden.contains(BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());
    }
}
