package com.itmecasey.smwti.api;

import net.minecraft.world.level.block.entity.BlockEntity;

/** What a plugin can register on the server (and on the client, where it only needs to exist). */
public interface CommonRegistration {
    /** Adds a provider that works out lines about a block entity on the server; see {@link ServerBlockProvider}. */
    void serverBlock(ServerBlockProvider provider);

    /** Adds a provider that works out lines about an entity on the server; see {@link ServerEntityProvider}. */
    void serverEntity(ServerEntityProvider provider);

    /**
     * Shows a progress bar for machines of the given block entity type: the source says how far the
     * machine has got (a fraction, or empty when idle) and SMWTI draws "Progress: 40%" with a bar in
     * the given RGB color. Use {@link Progress#fromData} when the machine keeps its times in a
     * {@code ContainerData}.
     */
    <T extends BlockEntity> void progress(Class<T> type, ProgressSource<T> source, int color);
}
