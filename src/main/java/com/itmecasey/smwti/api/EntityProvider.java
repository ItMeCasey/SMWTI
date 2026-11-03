package com.itmecasey.smwti.api;

import net.minecraft.world.entity.Entity;

/** Adds lines about an entity the player is looking at, on the client. */
@FunctionalInterface
public interface EntityProvider {
    /** Adds this provider's lines for the entity; add nothing when it has nothing to say. */
    void append(Tooltip tooltip, Entity entity);
}
