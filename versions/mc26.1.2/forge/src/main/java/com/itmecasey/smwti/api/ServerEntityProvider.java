package com.itmecasey.smwti.api;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/**
 * Works out lines about an entity on the server and sends them to the player looking at it, for
 * what the client does not know (a villager's trades or level, a pet's owner, a machine entity's
 * state). Asked a few times a second per player while they look, for entities within a few blocks.
 */
@FunctionalInterface
public interface ServerEntityProvider {
    /**
     * Adds the lines for the entity; add nothing when this provider does not know it.
     *
     * @param detailed whether the player asked for the long form
     */
    void collect(Tooltip tooltip, ServerLevel level, Entity entity, boolean detailed);
}
