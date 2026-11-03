package com.itmecasey.smwti.api;

import java.util.List;
import net.minecraft.world.entity.Entity;

/**
 * The entity the player is looking at, as a client-side provider sees it.
 *
 * @param entity      the entity as the client has it
 * @param detailed    whether the player asked for the long form (sneaking, by default)
 * @param serverLines what the server-side providers said about this entity (see {@link ServerEntityProvider}),
 *                    empty until the first answer arrives
 */
public record EntityView(Entity entity, boolean detailed, List<Line> serverLines) {
}
