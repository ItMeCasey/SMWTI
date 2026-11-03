package com.itmecasey.smwti.api;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Where other mods add to the overlay. Call these once while your mod loads (the mod constructor
 * or {@code FMLCommonSetupEvent}); register server providers on both sides, since the client only
 * needs them to exist and the server runs them. A mod that wants SMWTI as an optional dependency
 * keeps its calls in a class that is loaded only when {@code smwti} is present.
 *
 * <p>Providers run for every target, so each one checks what the target is and adds nothing when it
 * does not apply.
 */
public final class SmwtiApi {
    private static final List<BlockProvider> BLOCK_PROVIDERS = new CopyOnWriteArrayList<>();
    private static final List<ServerBlockProvider> SERVER_BLOCK_PROVIDERS = new CopyOnWriteArrayList<>();
    private static final List<EntityProvider> ENTITY_PROVIDERS = new CopyOnWriteArrayList<>();
    private static final List<ServerEntityProvider> SERVER_ENTITY_PROVIDERS = new CopyOnWriteArrayList<>();
    private static final List<TargetOverride<BlockView>> BLOCK_OVERRIDES = new CopyOnWriteArrayList<>();
    private static final List<TargetOverride<EntityView>> ENTITY_OVERRIDES = new CopyOnWriteArrayList<>();
    private static final List<Option> OPTIONS = new CopyOnWriteArrayList<>();

    private SmwtiApi() {
    }

    /** Adds a client-side provider for blocks. */
    public static void registerBlock(BlockProvider provider) {
        BLOCK_PROVIDERS.add(provider);
    }

    /** Adds a server-side provider for block entities; its lines reach the client as {@link BlockView#serverLines()}. */
    public static void registerServerBlock(ServerBlockProvider provider) {
        SERVER_BLOCK_PROVIDERS.add(provider);
    }

    /** Adds a client-side provider for entities. */
    public static void registerEntity(EntityProvider provider) {
        ENTITY_PROVIDERS.add(provider);
    }

    /** Adds a server-side provider for entities; its lines reach the client as {@link EntityView#serverLines()}. */
    public static void registerServerEntity(ServerEntityProvider provider) {
        SERVER_ENTITY_PROVIDERS.add(provider);
    }

    /** Lets a mod rename a block in the overlay or give it another icon. */
    public static void overrideBlock(TargetOverride<BlockView> override) {
        BLOCK_OVERRIDES.add(override);
    }

    /** Lets a mod rename an entity in the overlay or give it another icon. */
    public static void overrideEntity(TargetOverride<EntityView> override) {
        ENTITY_OVERRIDES.add(override);
    }

    /** Adds an on-off option to the settings screen; see {@link Option}. */
    public static Option registerOption(net.minecraft.resources.Identifier id, boolean defaultValue) {
        Option option = new Option(id, defaultValue);
        OPTIONS.add(option);
        return option;
    }

    /** The registered options, in registration order. */
    public static List<Option> options() {
        return OPTIONS;
    }

    /** The client block providers, in registration order. */
    public static List<BlockProvider> blockProviders() {
        return BLOCK_PROVIDERS;
    }

    /** The server block providers, in registration order. */
    public static List<ServerBlockProvider> serverBlockProviders() {
        return SERVER_BLOCK_PROVIDERS;
    }

    /** The client entity providers, in registration order. */
    public static List<EntityProvider> entityProviders() {
        return ENTITY_PROVIDERS;
    }

    /** The server entity providers, in registration order. */
    public static List<ServerEntityProvider> serverEntityProviders() {
        return SERVER_ENTITY_PROVIDERS;
    }

    /** The block overrides, in registration order. */
    public static List<TargetOverride<BlockView>> blockOverrides() {
        return BLOCK_OVERRIDES;
    }

    /** The entity overrides, in registration order. */
    public static List<TargetOverride<EntityView>> entityOverrides() {
        return ENTITY_OVERRIDES;
    }
}
