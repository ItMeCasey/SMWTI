package com.itmecasey.smwti.api;

import java.util.List;
import net.minecraft.resources.ResourceLocation;

/** What a plugin can register on the client. Each method does what the {@link SmwtiApi} method of the same purpose does. */
public interface ClientRegistration {
    /** Adds lines about a block; see {@link BlockProvider}. */
    void block(BlockProvider provider);

    /** Adds lines about an entity; see {@link EntityProvider}. */
    void entity(EntityProvider provider);

    /** Renames a block or gives it another icon; see {@link TargetOverride}. */
    void overrideBlock(TargetOverride<BlockView> override);

    /** Renames an entity or gives it another icon; see {@link TargetOverride}. */
    void overrideEntity(TargetOverride<EntityView> override);

    /** Points the overlay at another block than the one under the crosshair; see {@link BlockRedirect}. */
    void redirect(BlockRedirect redirect);

    /** Runs around the drawing of the overlay; see {@link RenderCallback}. */
    void renderCallback(RenderCallback callback);

    /** Adds an on-off setting to the settings screen; see {@link Option}. */
    Option option(ResourceLocation id, boolean defaultValue);

    /** Adds a slider to the settings screen; see {@link IntSetting}. */
    IntSetting intSetting(ResourceLocation id, int defaultValue, int min, int max);

    /** Adds a cycling choice to the settings screen; see {@link ChoiceSetting}. */
    ChoiceSetting choice(ResourceLocation id, List<String> choices, String defaultValue);
}
