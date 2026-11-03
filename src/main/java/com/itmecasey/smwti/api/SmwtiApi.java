package com.itmecasey.smwti.api;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Where other mods add lines to the overlay. Call these once while your mod loads (the mod
 * constructor or {@code FMLCommonSetupEvent}); register server providers on both sides, since the
 * client only needs them to exist and the server runs them. A mod that wants SMWTI as an optional
 * dependency keeps its calls in a class that is loaded only when {@code smwti} is present.
 *
 * <p>Providers run for every target, so each one checks what the target is and adds nothing when it
 * does not apply.
 */
public final class SmwtiApi {
    private static final List<BlockProvider> BLOCK_PROVIDERS = new CopyOnWriteArrayList<>();
    private static final List<ServerBlockProvider> SERVER_PROVIDERS = new CopyOnWriteArrayList<>();
    private static final List<EntityProvider> ENTITY_PROVIDERS = new CopyOnWriteArrayList<>();

    private SmwtiApi() {
    }

    /** Adds a client-side provider for blocks. */
    public static void registerBlock(BlockProvider provider) {
        BLOCK_PROVIDERS.add(provider);
    }

    /** Adds a server-side provider for block entities; its lines reach the client as {@link BlockView#serverLines()}. */
    public static void registerServerBlock(ServerBlockProvider provider) {
        SERVER_PROVIDERS.add(provider);
    }

    /** Adds a client-side provider for entities. */
    public static void registerEntity(EntityProvider provider) {
        ENTITY_PROVIDERS.add(provider);
    }

    /** The client block providers, in registration order. */
    public static List<BlockProvider> blockProviders() {
        return BLOCK_PROVIDERS;
    }

    /** The server block providers, in registration order. */
    public static List<ServerBlockProvider> serverBlockProviders() {
        return SERVER_PROVIDERS;
    }

    /** The client entity providers, in registration order. */
    public static List<EntityProvider> entityProviders() {
        return ENTITY_PROVIDERS;
    }
}
