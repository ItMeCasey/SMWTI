package com.itmecasey.smwti.client;

import com.itmecasey.smwti.Smwti;
import com.itmecasey.smwti.SmwtiConfig;
import com.itmecasey.smwti.api.BlockProvider;
import com.itmecasey.smwti.api.BlockView;
import com.itmecasey.smwti.api.EntityProvider;
import com.itmecasey.smwti.api.EntityView;
import com.itmecasey.smwti.api.Line;
import com.itmecasey.smwti.api.Position;
import com.itmecasey.smwti.api.SmwtiApi;
import com.itmecasey.smwti.api.TargetOverride;
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
import org.jetbrains.annotations.Nullable;

/**
 * Draws the overlay: a dark panel at the top of the screen with the target's icon and name, the
 * name of its mod, and the lines the providers added in three parts (head, body, tail), a bar
 * under those that have one and a small icon before those that carry an item.
 */
@Mod.EventBusSubscriber(modid = Smwti.MODID, value = Dist.CLIENT)
public final class OverlayLayer {
    private static final Identifier LAYER = Smwti.id("look_at");

    private static final int PADDING = 5;
    private static final int ICON = 16;
    /** Side of the small icon before an item line; the item is drawn at 16 and scaled to this. */
    private static final int SMALL_ICON = 12;
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
    private static @Nullable Content content(Level level) {
        boolean detailed = LookTracker.detailed();
        BlockPos pos = LookTracker.block();
        if (pos != null) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || Hidden.block(state.getBlock())) {
                return null;
            }
            BlockEntity entity = level.getBlockEntity(pos);
            BlockView view = new BlockView(level, pos, state, entity, detailed, LookTracker.serverLines());
            Tooltip tooltip = new Tooltip();
            for (BlockProvider provider : SmwtiApi.blockProviders()) {
                run(() -> provider.append(tooltip, view));
            }
            view.serverLines().forEach(tooltip::add);
            Component name = state.getBlock().getName();
            ItemStack icon = new ItemStack(state.getBlock());
            for (TargetOverride<BlockView> override : SmwtiApi.blockOverrides()) {
                Component renamed = call(() -> override.name(view));
                if (renamed != null) {
                    name = renamed;
                    break;
                }
            }
            for (TargetOverride<BlockView> override : SmwtiApi.blockOverrides()) {
                ItemStack drawn = call(() -> override.icon(view));
                if (drawn != null) {
                    icon = drawn;
                    break;
                }
            }
            return new Content(icon, name, modName(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace()), tooltip);
        }
        Entity target = LookTracker.entity();
        if (target != null && !Hidden.entity(target.getType())) {
            EntityView view = new EntityView(target, detailed, LookTracker.serverLines());
            Tooltip tooltip = new Tooltip();
            for (EntityProvider provider : SmwtiApi.entityProviders()) {
                run(() -> provider.append(tooltip, view));
            }
            view.serverLines().forEach(tooltip::add);
            Component name = target.getName();
            ItemStack icon = ItemStack.EMPTY;
            for (TargetOverride<EntityView> override : SmwtiApi.entityOverrides()) {
                Component renamed = call(() -> override.name(view));
                if (renamed != null) {
                    name = renamed;
                    break;
                }
            }
            for (TargetOverride<EntityView> override : SmwtiApi.entityOverrides()) {
                ItemStack drawn = call(() -> override.icon(view));
                if (drawn != null) {
                    icon = drawn;
                    break;
                }
            }
            return new Content(icon, name, modName(BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).getNamespace()), tooltip);
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

    /** Like {@link #run} for an override that answers; a failure counts as no answer. */
    private static <T> @Nullable T call(java.util.function.Supplier<T> override) {
        try {
            return override.get();
        } catch (RuntimeException e) {
            Smwti.LOGGER.warn("A look-at override failed", e);
            return null;
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
    private record Content(ItemStack icon, Component name, Component mod, Tooltip tooltip) {
        private int textLeft() {
            return icon.isEmpty() ? 0 : ICON + PADDING;
        }

        private List<Line> section(Position position) {
            return tooltip.lines(position);
        }

        private static int rowHeight(Font font, Line line) {
            int text = line.icon().isEmpty() ? font.lineHeight : Math.max(font.lineHeight, SMALL_ICON);
            return text + (line.hasBar() ? BAR_HEIGHT + 1 : 0);
        }

        private int linesHeight(Font font, List<Line> lines) {
            int total = 0;
            for (Line line : lines) {
                total += rowHeight(font, line) + LINE_GAP;
            }
            return total;
        }

        int width(Font font) {
            int widest = Math.max(font.width(name), font.width(mod)) + textLeft();
            for (Line line : tooltip.lines()) {
                widest = Math.max(widest, font.width(line.text()) + (line.icon().isEmpty() ? 0 : SMALL_ICON + 3));
            }
            return Math.max(widest, 60) + 2 * PADDING;
        }

        private int headerHeight(Font font) {
            int text = font.lineHeight + (mod.getString().isEmpty() ? 0 : font.lineHeight + 1);
            return Math.max(icon.isEmpty() ? 0 : ICON, text);
        }

        int height(Font font) {
            int lines = linesHeight(font, section(Position.HEAD)) + linesHeight(font, section(Position.BODY))
                    + linesHeight(font, section(Position.TAIL));
            return 2 * PADDING + headerHeight(font) + (lines > 0 ? lines : LINE_GAP) - LINE_GAP + LINE_GAP;
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
            int rowY = y + headerHeight(font) + LINE_GAP;
            for (Position position : Position.values()) {
                for (Line line : section(position)) {
                    rowY = drawLine(graphics, font, line, x, rowY, innerWidth);
                }
            }
        }

        private int drawLine(GuiGraphicsExtractor graphics, Font font, Line line, int x, int rowY, int innerWidth) {
            ItemStack small = line.icon();
            int textX = x;
            int textY = rowY;
            if (!small.isEmpty()) {
                graphics.pose().pushMatrix();
                graphics.pose().translate(x, rowY);
                graphics.pose().scale(SMALL_ICON / 16.0F, SMALL_ICON / 16.0F);
                graphics.fakeItem(small, 0, 0);
                graphics.pose().popMatrix();
                textX = x + SMALL_ICON + 3;
                textY = rowY + (SMALL_ICON - font.lineHeight) / 2;
            }
            graphics.text(font, line.text(), textX, textY, TEXT, false);
            int bottom = rowY + Math.max(font.lineHeight, small.isEmpty() ? 0 : SMALL_ICON);
            if (line.hasBar()) {
                graphics.fill(x, bottom + 1, x + innerWidth, bottom + 1 + BAR_HEIGHT, BAR_BACK);
                graphics.fill(x, bottom + 1, x + Math.round(innerWidth * line.fraction()), bottom + 1 + BAR_HEIGHT,
                        0xFF000000 | line.color());
                bottom += BAR_HEIGHT + 1;
            }
            return bottom + LINE_GAP;
        }
    }
}
