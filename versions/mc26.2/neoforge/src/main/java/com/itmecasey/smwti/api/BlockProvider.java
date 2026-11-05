package com.itmecasey.smwti.api;

/**
 * Adds lines about a block, on the client. It can read the block's state and the block entity as
 * the client knows them; for anything the client does not have (a machine's progress, what a tank
 * holds) use a {@link ServerBlockProvider}, whose lines arrive in {@link BlockView#serverLines()}.
 */
@FunctionalInterface
public interface BlockProvider {
    /** Adds this provider's lines for the block; add nothing when it has nothing to say. */
    void append(Tooltip tooltip, BlockView view);
}
