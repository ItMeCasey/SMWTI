package com.itmecasey.smwti.api;

/** What a plugin can register on the server (and on the client, where it only needs to exist). */
public interface CommonRegistration {
    /** Adds a provider that works out lines about a block entity on the server; see {@link ServerBlockProvider}. */
    void serverBlock(ServerBlockProvider provider);

    /** Adds a provider that works out lines about an entity on the server; see {@link ServerEntityProvider}. */
    void serverEntity(ServerEntityProvider provider);
}
