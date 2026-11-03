package com.itmecasey.smwti.client;

import com.itmecasey.smwti.Smwti;
import com.itmecasey.smwti.SmwtiConfig;
import com.itmecasey.smwti.api.BlockView;
import com.itmecasey.smwti.api.Line;
import com.itmecasey.smwti.api.SmwtiApi;
import com.itmecasey.smwti.api.Tooltip;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

/**
 * Draws the overlay: a dark panel at the top of the screen with the target's icon and name, the
 * name of its mod, and the lines the providers added, a bar under those that have one.
 */
@Mod.EventBusSubscriber(modid = Smwti.MODID, value = Dist.CLIENT)
public final class OverlayLayer {
    private static final Identifier LAYER = Smwti.id("look_at");

    private static final int PADDING = 5;
    private static final int ICON = 16;
    private static final int LINE_GAP = 2;
    private static final int BAR_HEIGHT = 3;
    private static final int PANEL = 0xC0101018;
    private static final int EDGE = 0xFF3A3A4C;
    private static final int BAR_BACK = 0xFF2A2A34;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MOD_TEXT = 0xFF8E8EA6;

    private OverlayLayer() {
    }

    /** Puts the overlay on top of the game's own layers. */
    @SubscribeEvent
    public static void addLayer(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().add(LAYER, OverlayLayer::extract);
    }

    private static void extract(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft game = Minecraft.getInstance();
        if (!SmwtiConfig.enabled() || game.level == null || game.player == null || game.options.hideGui
                || game.screen != null || (SmwtiConfig.hideWhileSneaking() && game.player.isShiftKeyDown())) {
            return;
        }
        Content content = content(game.level);
        if (content == null) {
            return;
        }
        Font font = game.font;
        int width = content.width(font);
        int height = content.height(font);
        float scale = SmwtiConfig.scale();
        int screenWidth = (int) (graphics.guiWidth() / scale);
        int x = switch (SmwtiConfig.anchor()) {
            case TOP_LEFT -> 6;
            case TOP_CENTER -> (screenWidth - width) / 2;
            case TOP_RIGHT -> screenWidth - width - 6;
        };
        int y = 6;

        graphics.pose().pushMatrix();
        graphics.pose().scale(scale, scale);
        graphics.fill(x, y, x + width, y + height, EDGE);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, PANEL);
        content.draw(graphics, font, x + PADDING, y + PADDING, width - 2 * PADDING);
        graphics.pose().popMatrix();
    }

    /** What the overlay shows for the current target, or {@code null} when there is none. */
    private static Content content(Level level) {
        BlockPos pos = LookTracker.block();
        if (pos != null) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                return null;
            }
            BlockEntity entity = level.getBlockEntity(pos);
            Tooltip tooltip = new Tooltip();
            BlockView view = new BlockView(level, pos, state, entity, LookTracker.serverLines());
            for (var provider : SmwtiApi.blockProviders()) {
                run(() -> provider.append(tooltip, view));
            }
            for (Line line : view.serverLines()) {
                tooltip.add(line);
            }
            return new Content(new ItemStack(state.getBlock()), state.getBlock().getName(),
                    modName(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace()),
                    tooltip.lines());
        }
        Entity target = LookTracker.entity();
        if (target != null) {
            Tooltip tooltip = new Tooltip();
            for (var provider : SmwtiApi.entityProviders()) {
                run(() -> provider.append(tooltip, target));
            }
            return new Content(ItemStack.EMPTY, target.getName(),
                    modName(BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).getNamespace()),
                    tooltip.lines());
        }
        return null;
    }

    /** A provider that throws is skipped, so one broken mod cannot take the overlay down. */
    private static void run(Runnable provider) {
        try {
            provider.run();
        } catch (RuntimeException e) {
            Smwti.LOGGER.warn("A look-at provider failed", e);
        }
    }

    /** The display name of a mod, or the namespace itself when the mod is not known. */
    private static Component modName(String namespace) {
        if (!SmwtiConfig.showModName()) {
            return Component.empty();
        }
        String name = "minecraft".equals(namespace) ? "Minecraft"
                : ModList.getModContainerById(namespace).map(container -> container.getModInfo().getDisplayName()).orElse(namespace);
        return Component.literal(name).withStyle(ChatFormatting.ITALIC);
    }

    /** The icon, name, mod and lines of one target, laid out and drawn. */
    private record Content(ItemStack icon, Component name, Component mod, List<Line> lines) {
        private int textLeft() {
            return icon.isEmpty() ? 0 : ICON + PADDING;
        }

        int width(Font font) {
            int widest = Math.max(font.width(name), font.width(mod));
            for (Line line : lines) {
                widest = Math.max(widest, font.width(line.text()));
            }
            return Math.max(widest + textLeft(), 60) + 2 * PADDING;
        }

        int height(Font font) {
            return 2 * PADDING + Math.max(icon.isEmpty() ? 0 : ICON, headerHeight(font))
                    + (lines.isEmpty() ? 0 : LINE_GAP + bodyHeight(font));
        }

        private int headerHeight(Font font) {
            return font.lineHeight + (mod.getString().isEmpty() ? 0 : font.lineHeight + 1);
        }

        private int bodyHeight(Font font) {
            int total = 0;
            for (Line line : lines) {
                total += font.lineHeight + (line.hasBar() ? BAR_HEIGHT + 1 : 0) + LINE_GAP;
            }
            return Math.max(0, total - LINE_GAP);
        }

        void draw(GuiGraphicsExtractor graphics, Font font, int x, int y, int innerWidth) {
            if (!icon.isEmpty()) {
                graphics.fakeItem(icon, x, y);
            }
            int textX = x + textLeft();
            graphics.text(font, name, textX, y, TEXT, true);
            if (!mod.getString().isEmpty()) {
                graphics.text(font, mod, textX, y + font.lineHeight + 1, MOD_TEXT, false);
            }
            int rowY = y + Math.max(icon.isEmpty() ? 0 : ICON, headerHeight(font)) + LINE_GAP;
            for (Line line : lines) {
                graphics.text(font, line.text(), x, rowY, TEXT, false);
                rowY += font.lineHeight;
                if (line.hasBar()) {
                    graphics.fill(x, rowY + 1, x + innerWidth, rowY + 1 + BAR_HEIGHT, BAR_BACK);
                    graphics.fill(x, rowY + 1, x + Math.round(innerWidth * line.fraction()), rowY + 1 + BAR_HEIGHT,
                            0xFF000000 | line.color());
                    rowY += BAR_HEIGHT + 1;
                }
                rowY += LINE_GAP;
            }
        }
    }
}
