package com.itmecasey.smwti.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;

/**
 * Paints a fluid's own texture and tint into the overlay, the sprite and tint the fluid has in the
 * world, so a resource pack that retextures a fluid retextures its bars too.
 */
final class FluidSprites {
    /** Edge of a fluid sprite tile, in pixels. */
    private static final int TILE = 16;

    private FluidSprites() {
    }

    /** The fluid with this registry name, or {@code null} when the name is not one. */
    static Fluid byName(String name) {
        Identifier id = Identifier.tryParse(name);
        Fluid fluid = id == null ? null : BuiltInRegistries.FLUID.getValue(id);
        return fluid == null || fluid == Fluids.EMPTY ? null : fluid;
    }

    /**
     * Fills a rectangle with the fluid's still sprite, tiled from the left and clipped, and tinted.
     * The tint is the fluid's own, kept opaque.
     */
    static void drawTiled(GuiGraphicsExtractor graphics, Fluid fluid, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid);
        TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS)
                .getSprite(extensions.getStillTexture());
        int argb = extensions.getTintColor() | 0xFF000000;
        graphics.enableScissor(x, y, x + width, y + height);
        for (int tileX = x; tileX < x + width; tileX += TILE) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, tileX, y + height - TILE, TILE, TILE, argb);
        }
        graphics.disableScissor();
    }
}
