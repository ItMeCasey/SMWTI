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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
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
 * name of its mod, a badge to the right of the name, and the lines the providers added in three
 * parts (head, body, tail). A line may have a bar under it and a small icon before it, and item
 * stacks listed one after another are drawn as a grid of icons with their counts.
 */
@Mod.EventBusSubscriber(modid = Smwti.MODID, value = Dist.CLIENT)
public final class OverlayLayer {
    private static final Identifier LAYER = Smwti.id("look_at");

    private static final int PADDING = 5;
    private static final int ICON = 16;
    /** Side of the small icon before an item line; the item is drawn at 16 and scaled to this. */
    private static final int SMALL_ICON = 12;
    /** A grid cell for a listed item stack: the 16 pixel icon and a pixel of air. */
    private static final int SLOT = 18;
    /** Stacks listed side by side per row. */
    private static final int STACKS_PER_ROW = 9;
    private static final float COUNT_SCALE = 0.6F;
    /** Space between the name and the badge. */
    private static final int BADGE_GAP = 8;
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
    private static <T> @Nullable T call(Supplier<T> override) {
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
        /** One drawn row: a single text line, or a run of item stacks shown side by side. */
        private record Row(List<Line> lines, boolean grid) {
        }

        private int textLeft() {
            return icon.isEmpty() ? 0 : ICON + PADDING;
        }

        /** The badge to the right of the name: the first badge line, if any provider added one. */
        private @Nullable Line badge() {
            List<Line> badges = tooltip.lines(Position.BADGE);
            return badges.isEmpty() ? null : badges.get(0);
        }

        /** The lines under the header in drawing order (head, body, tail), neighboring item stacks gathered into grid rows. */
        private List<Row> rows() {
            List<Row> rows = new ArrayList<>();
            for (Position position : List.of(Position.HEAD, Position.BODY, Position.TAIL)) {
                List<Line> run = new ArrayList<>();
                for (Line line : tooltip.lines(position)) {
                    if (line.isStack()) {
                        run.add(line);
                        continue;
                    }
                    flush(rows, run);
                    rows.add(new Row(List.of(line), false));
                }
                flush(rows, run);
            }
            return rows;
        }

        /** Cuts a run of stacks into rows of at most {@link #STACKS_PER_ROW}. */
        private static void flush(List<Row> rows, List<Line> run) {
            for (int from = 0; from < run.size(); from += STACKS_PER_ROW) {
                rows.add(new Row(List.copyOf(run.subList(from, Math.min(run.size(), from + STACKS_PER_ROW))), true));
            }
            run.clear();
        }

        private static int rowHeight(Font font, Row row) {
            if (row.grid()) {
                return SLOT;
            }
            Line line = row.lines().get(0);
            int text = line.icon().isEmpty() ? font.lineHeight : Math.max(font.lineHeight, SMALL_ICON);
            return text + (line.hasBar() ? BAR_HEIGHT + 1 : 0);
        }

        private static int lineWidth(Font font, Line line) {
            return font.width(line.text()) + (line.icon().isEmpty() ? 0 : SMALL_ICON + 3);
        }

        private static int rowWidth(Font font, Row row) {
            return row.grid() ? row.lines().size() * SLOT : lineWidth(font, row.lines().get(0));
        }

        int width(Font font) {
            Line badge = badge();
            int header = Math.max(font.width(name) + (badge == null ? 0 : BADGE_GAP + lineWidth(font, badge)), font.width(mod));
            int widest = header + textLeft();
            for (Row row : rows()) {
                widest = Math.max(widest, rowWidth(font, row));
            }
            return Math.max(widest, 60) + 2 * PADDING;
        }

        private int headerHeight(Font font) {
            int text = font.lineHeight + (mod.getString().isEmpty() ? 0 : font.lineHeight + 1);
            return Math.max(icon.isEmpty() ? 0 : ICON, text);
        }

        int height(Font font) {
            int total = 0;
            for (Row row : rows()) {
                total += rowHeight(font, row) + LINE_GAP;
            }
            return 2 * PADDING + headerHeight(font) + total;
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
            Line badge = badge();
            if (badge != null) {
                // Flush with the right edge of the panel, on the row of the name.
                drawLine(graphics, font, badge, x + innerWidth - lineWidth(font, badge), y - 1, 0);
            }
            int rowY = y + headerHeight(font) + LINE_GAP;
            for (Row row : rows()) {
                if (row.grid()) {
                    drawStacks(graphics, font, row, x, rowY);
                    rowY += SLOT + LINE_GAP;
                } else {
                    rowY = drawLine(graphics, font, row.lines().get(0), x, rowY, innerWidth);
                }
            }
        }

        /** Item stacks side by side: the icon, and the count small in its lower right corner when it is more than one. */
        private void drawStacks(GuiGraphicsExtractor graphics, Font font, Row row, int x, int y) {
            int cellX = x;
            for (Line line : row.lines()) {
                ItemStack stack = line.icon();
                graphics.fakeItem(stack, cellX, y);
                if (stack.getCount() > 1) {
                    Component count = Component.literal(shortCount(stack.getCount()));
                    // Small text whose right and bottom edges meet the icon's.
                    graphics.pose().pushMatrix();
                    graphics.pose().translate(cellX + ICON - font.width(count) * COUNT_SCALE + 1,
                            y + ICON - font.lineHeight * COUNT_SCALE + 1);
                    graphics.pose().scale(COUNT_SCALE, COUNT_SCALE);
                    graphics.text(font, count, 0, 0, TEXT, true);
                    graphics.pose().popMatrix();
                }
                cellX += SLOT;
            }
        }

        /** 1 to 999 as they are, then 1.2k style so a big stack never outgrows its icon. */
        private static String shortCount(int count) {
            if (count < 1000) {
                return Integer.toString(count);
            }
            return count < 10_000 ? String.format(Locale.ROOT, "%.1fk", count / 1000.0) : (count / 1000) + "k";
        }

        /** One text line with its icon and bar; returns where the next row starts. */
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
