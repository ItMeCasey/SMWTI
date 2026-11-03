package com.itmecasey.smwti.client;

import com.itmecasey.smwti.Smwti;
import com.itmecasey.smwti.SmwtiConfig;
import com.itmecasey.smwti.api.BlockProvider;
import com.itmecasey.smwti.api.BlockView;
import com.itmecasey.smwti.api.EntityProvider;
import com.itmecasey.smwti.api.Element;
import com.itmecasey.smwti.api.EntityView;
import com.itmecasey.smwti.api.Line;
import com.itmecasey.smwti.api.Position;
import com.itmecasey.smwti.api.RenderCallback;
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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

/**
 * Draws the overlay: a dark panel at the top of the screen with the target's icon and name, the
 * name of its mod, a badge to the right of the name, and the lines the providers added in three
 * parts (head, body, tail). A line may have a bar under it and a small icon before it, and item
 * stacks listed one after another are drawn as a grid of icons with their counts.
 */
@Mod.EventBusSubscriber(modid = Smwti.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OverlayLayer {
    private static final ResourceLocation LAYER = Smwti.id("look_at");

    private static final int PADDING = 5;
    private static final int PADDING_COMPACT = 3;
    private static final int ICON = 16;
    /** Side of the small icon before an item line; the item is drawn at 16 and scaled to this. */
    private static final int SMALL_ICON = 12;
    /** Side of the badge beside the name, in pixels; the 16 pixel icon scaled down. */
    private static final int BADGE_ICON = 11;
    /** A grid cell for a listed item stack: the 16 pixel icon and a pixel of air. */
    private static final int SLOT = 18;
    /** Stacks listed side by side per row. */
    private static final int STACKS_PER_ROW = 9;
    private static final float COUNT_SCALE = 0.6F;
    /** Space between the name and the badge. */
    private static final int BADGE_GAP = 8;
    private static final int LINE_GAP = 2;
    private static final int LINE_GAP_COMPACT = 1;
    private static final int BAR_HEIGHT = 3;
    /** Height of a bar filled with a fluid's texture, tall enough for the texture to show and the text to sit on it. */
    private static final int FLUID_BAR_HEIGHT = 12;

    /** The colors in use this frame; set at the start of each draw. */
    private static Palette palette = Palette.current();

    /** Room around the panel's contents; the compact overlay uses less. */
    private static int padding() {
        return SmwtiConfig.compact() ? PADDING_COMPACT : PADDING;
    }

    /** Space between rows. */
    private static int lineGap() {
        return SmwtiConfig.compact() ? LINE_GAP_COMPACT : LINE_GAP;
    }

    private OverlayLayer() {
    }

    /** Puts the overlay on top of the game's own layers. */
    @SubscribeEvent
    public static void addLayer(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().add(LAYER, OverlayLayer::extract);
    }

    private static void extract(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft game = Minecraft.getInstance();
        if (!SmwtiConfig.enabled() || (SmwtiConfig.holdToShow() && !ClientInit.holdKeyDown()) || game.level == null || game.player == null || game.options.hideGui
                || game.screen != null || (SmwtiConfig.hideWhileSneaking() && game.player.isShiftKeyDown())) {
            return;
        }
        Content content = content(game.level);
        if (content == null) {
            return;
        }
        palette = Palette.current();
        Font font = game.font;
        int width = content.width(font);
        int height = content.height(font);
        float scale = SmwtiConfig.scale();
        int[] at = position(graphics.guiWidth(), graphics.guiHeight(), width, height, scale, SmwtiConfig.anchor(),
                SmwtiConfig.positionX(), SmwtiConfig.positionY());
        int x = at[0];
        int y = at[1];

        RenderCallback.Panel panel = new RenderCallback.Panel(content.name(), x, y, width, height);
        for (RenderCallback callback : SmwtiApi.renderCallbacks()) {
            if (Boolean.FALSE.equals(call(() -> callback.before(panel)))) {
                return;
            }
        }
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0F);
        drawPanel(graphics, font, content, x, y, width, height);
        for (RenderCallback callback : SmwtiApi.renderCallbacks()) {
            run(() -> callback.after(graphics, panel));
        }
        graphics.pose().popPose();
    }

    /**
     * Where the panel's top left corner goes, in the overlay's own (scaled) units: an anchor along
     * the top, or a custom spot given as fractions of the room the panel has to move in.
     */
    private static int[] position(int guiWidth, int guiHeight, int width, int height, float scale, SmwtiConfig.Anchor anchor,
                                  double fractionX, double fractionY) {
        int screenWidth = (int) (guiWidth / scale);
        int screenHeight = (int) (guiHeight / scale);
        return switch (anchor) {
            case TOP_LEFT -> new int[] {6, 6};
            case TOP_CENTER -> new int[] {(screenWidth - width) / 2, 6};
            case TOP_RIGHT -> new int[] {screenWidth - width - 6, 6};
            case CUSTOM -> {
                // Worked out in screen pixels, where the margins and the hotbar are, then back to the overlay's units.
                int[] pixels = OverlayPlacement.fromFractions(guiWidth, guiHeight, Math.round(width * scale),
                        Math.round(height * scale), fractionX, fractionY);
                yield new int[] {Math.round(pixels[0] / scale), Math.round(pixels[1] / scale)};
            }
        };
    }

    private static void drawPanel(GuiGraphics graphics, Font font, Content content, int x, int y, int width, int height) {
        if ((palette.edge() >>> 24) == 0) {
            graphics.fill(x, y, x + width, y + height, palette.panel());
        } else {
            graphics.fill(x, y, x + width, y + height, palette.edge());
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, palette.panel());
        }
        if (palette.accent() != 0) {
            graphics.fill(x, y, x + width, y + 2, palette.accent());
        }
        content.draw(graphics, font, x + padding(), y + padding(), width - 2 * padding());
    }

    /**
     * Draws a sample panel at a custom spot for the move screen, and returns where it landed on
     * the screen in GUI pixels: x, y, width, height.
     */
    public static int[] drawSample(GuiGraphics graphics, Font font, double fractionX, double fractionY) {
        palette = Palette.current();
        Tooltip tooltip = new Tooltip();
        tooltip.bar(Component.translatable("smwti.sample.growth"), 0.6, 0x5DBB63);
        tooltip.stack(Position.BODY, new ItemStack(net.minecraft.world.item.Items.DIAMOND, 5));
        tooltip.stack(Position.BODY, new ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 32));
        Content sample = new Content(new ItemStack(net.minecraft.world.item.Items.GRASS_BLOCK),
                Component.translatable("smwti.sample.name"), modName("minecraft"), tooltip);
        int width = sample.width(font);
        int height = sample.height(font);
        float scale = SmwtiConfig.scale();
        int[] at = position(graphics.guiWidth(), graphics.guiHeight(), width, height, scale, SmwtiConfig.Anchor.CUSTOM,
                fractionX, fractionY);
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0F);
        drawPanel(graphics, font, sample, at[0], at[1], width, height);
        graphics.pose().popPose();
        return new int[] {Math.round(at[0] * scale), Math.round(at[1] * scale), Math.round(width * scale), Math.round(height * scale)};
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
        if (!SmwtiConfig.showModName() || SmwtiConfig.compact()) {
            return Component.empty();
        }
        String name = "minecraft".equals(namespace) ? "Minecraft"
                : ModList.get().getModContainerById(namespace).map(container -> container.getModInfo().getDisplayName()).orElse(namespace);
        return Component.literal(name).withStyle(ChatFormatting.ITALIC);
    }

    /** The icon, name, mod and lines of one target, laid out and drawn. */
    private record Content(ItemStack icon, Component name, Component mod, Tooltip tooltip) {
        /** One drawn row: a single text line, or a run of item stacks shown side by side. */
        private record Row(List<Line> lines, boolean grid, @Nullable Element element) {
        }

        private int textLeft() {
            return icon.isEmpty() ? 0 : ICON + padding();
        }

        /** The badge to the right of the name: the first badge line, if any provider added one. */
        private @Nullable Line badge() {
            List<Line> badges = tooltip.lines(Position.BADGE);
            return badges.isEmpty() ? null : badges.get(0);
        }

        /** The lines under the header in drawing order (head, body, tail), neighboring item stacks gathered into grid rows. */
        private List<Row> rows() {
            List<Row> rows = new ArrayList<>();
            // The compact overlay keeps only the body.
            for (Position position : SmwtiConfig.compact() ? List.of(Position.BODY) : List.of(Position.HEAD, Position.BODY, Position.TAIL)) {
                List<Line> run = new ArrayList<>();
                for (Line line : tooltip.lines(position)) {
                    if (line.isStack()) {
                        run.add(line);
                        continue;
                    }
                    flush(rows, run);
                    rows.add(new Row(List.of(line), false, null));
                }
                flush(rows, run);
                for (Element element : tooltip.elements(position)) {
                    rows.add(new Row(List.of(), false, element));
                }
            }
            return rows;
        }

        /** Cuts a run of stacks into rows of at most {@link #STACKS_PER_ROW}. */
        private static void flush(List<Row> rows, List<Line> run) {
            for (int from = 0; from < run.size(); from += STACKS_PER_ROW) {
                rows.add(new Row(List.copyOf(run.subList(from, Math.min(run.size(), from + STACKS_PER_ROW))), true, null));
            }
            run.clear();
        }

        private static int rowHeight(Font font, Row row) {
            if (row.element() != null) {
                return row.element().height(font);
            }
            if (row.grid()) {
                return SLOT;
            }
            Line line = row.lines().get(0);
            if (line.isFluidBar()) {
                return FLUID_BAR_HEIGHT;
            }
            int text = line.icon().isEmpty() ? font.lineHeight : Math.max(font.lineHeight, SMALL_ICON);
            return text + (line.hasBar() ? BAR_HEIGHT + 1 : 0);
        }

        private static int lineWidth(Font font, Line line) {
            if (line.isFluidBar()) {
                // The text sits on the bar with a little room at each end.
                return font.width(line.text()) + 8;
            }
            return font.width(line.text()) + (line.icon().isEmpty() ? 0 : SMALL_ICON + 3);
        }

        private static int rowWidth(Font font, Row row) {
            if (row.element() != null) {
                return row.element().width(font);
            }
            return row.grid() ? row.lines().size() * SLOT : lineWidth(font, row.lines().get(0));
        }

        int width(Font font) {
            Line badge = badge();
            int top = textLeft() + font.width(name) + (badge == null ? 0 : BADGE_GAP + badgeWidth(font, badge));
            int widest = Math.max(top, textLeft() + font.width(mod));
            for (Row row : rows()) {
                widest = Math.max(widest, rowWidth(font, row));
            }
            return Math.max(widest, 60) + 2 * padding();
        }

        /** A badge that is only an item stack is drawn small, so it stays a mark beside the name. */
        private static int badgeWidth(Font font, Line badge) {
            return badge.isStack() ? BADGE_ICON : lineWidth(font, badge);
        }

        /** The height of the top line, which holds the icon, the name and the badge side by side. */
        private int topLineHeight(Font font) {
            Line badge = badge();
            boolean tall = !icon.isEmpty() || (badge != null && badge.isStack());
            return Math.max(font.lineHeight, tall ? ICON : 0);
        }

        private int headerHeight(Font font) {
            return topLineHeight(font) + (mod.getString().isEmpty() ? 0 : font.lineHeight + 1);
        }

        int height(Font font) {
            int total = 0;
            for (Row row : rows()) {
                total += rowHeight(font, row) + lineGap();
            }
            return 2 * padding() + headerHeight(font) + total;
        }

        void draw(GuiGraphics graphics, Font font, int x, int y, int innerWidth) {
            if (!icon.isEmpty()) {
                graphics.renderFakeItem(icon, x, y);
            }
            // The icon, the name and the badge share the top line, each centered on it.
            int top = topLineHeight(font);
            int textX = x + textLeft();
            graphics.drawString(font, name, textX, y + (top - font.lineHeight) / 2, palette.text(), palette.shadow());
            if (!mod.getString().isEmpty()) {
                graphics.drawString(font, mod, textX, y + top + 1, palette.sub(), false);
            }
            Line badge = badge();
            if (badge != null) {
                // Flush with the right edge of the panel.
                int badgeX = x + innerWidth - badgeWidth(font, badge);
                if (badge.isStack()) {
                    // Drawn at the small size: the icon and its corner mark are scaled down together.
                    graphics.pose().pushPose();
                    graphics.pose().translate(badgeX, y + (top - BADGE_ICON) / 2.0F, 0.0F);
                    graphics.pose().scale(BADGE_ICON / (float) ICON, BADGE_ICON / (float) ICON, 1.0F);
                    drawStack(graphics, font, badge, 0, 0);
                    graphics.pose().popPose();
                } else {
                    drawLine(graphics, font, badge, badgeX, y + (top - Math.max(font.lineHeight, SMALL_ICON)) / 2, 0);
                }
            }
            int rowY = y + headerHeight(font) + lineGap();
            for (Row row : rows()) {
                if (row.element() != null) {
                    row.element().draw(graphics, font, x, rowY, innerWidth);
                    rowY += row.element().height(font) + lineGap();
                } else if (row.grid()) {
                    drawStacks(graphics, font, row, x, rowY);
                    rowY += SLOT + lineGap();
                } else {
                    rowY = drawLine(graphics, font, row.lines().get(0), x, rowY, innerWidth);
                }
            }
        }

        /** Item stacks side by side. */
        private void drawStacks(GuiGraphics graphics, Font font, Row row, int x, int y) {
            int cellX = x;
            for (Line line : row.lines()) {
                drawStack(graphics, font, line, cellX, y);
                cellX += SLOT;
            }
        }

        /**
         * One item icon with its corner text, small and at the lower right: the line's mark if it has
         * one, otherwise the count when there is more than one.
         */
        private void drawStack(GuiGraphics graphics, Font font, Line line, int x, int y) {
            ItemStack stack = line.icon();
            graphics.renderFakeItem(stack, x, y);
            boolean marked = !line.mark().isEmpty();
            if (!marked && stack.getCount() <= 1) {
                return;
            }
            Component corner = Component.literal(marked ? line.mark() : shortCount(stack.getCount()));
            int color = marked ? 0xFF000000 | line.markColor() : palette.text();
            // Small text whose right and bottom edges meet the icon's.
            graphics.pose().pushPose();
            graphics.pose().translate(x + ICON - font.width(corner) * COUNT_SCALE + 1, y + ICON - font.lineHeight * COUNT_SCALE + 1, 200.0F);
            graphics.pose().scale(COUNT_SCALE, COUNT_SCALE, 1.0F);
            graphics.drawString(font, corner, 0, 0, color, true);
            graphics.pose().popPose();
        }

        /** 1 to 999 as they are, then 1.2k style so a big stack never outgrows its icon. */
        private static String shortCount(int count) {
            if (count < 1000) {
                return Integer.toString(count);
            }
            return count < 10_000 ? String.format(Locale.ROOT, "%.1fk", count / 1000.0) : (count / 1000) + "k";
        }

        /** One text line with its icon and bar; returns where the next row starts. */
        /** A fluid bar: the dark backdrop, the fluid's texture for the filled part, and the text over it. */
        private int drawFluidBar(GuiGraphics graphics, Font font, Line line, int x, int rowY, int innerWidth) {
            graphics.fill(x, rowY, x + innerWidth, rowY + FLUID_BAR_HEIGHT, palette.barBack());
            Fluid fluid = FluidSprites.byName(line.fluid());
            if (fluid != null) {
                FluidSprites.drawTiled(graphics, fluid, x, rowY, Math.round(innerWidth * line.fraction()), FLUID_BAR_HEIGHT);
            }
            graphics.drawString(font, line.text(), x + 4, rowY + (FLUID_BAR_HEIGHT - font.lineHeight) / 2 + 1, 0xFFFFFFFF, true);
            return rowY + FLUID_BAR_HEIGHT + lineGap();
        }

        private int drawLine(GuiGraphics graphics, Font font, Line line, int x, int rowY, int innerWidth) {
            if (line.isFluidBar()) {
                return drawFluidBar(graphics, font, line, x, rowY, innerWidth);
            }
            ItemStack small = line.icon();
            int textX = x;
            int textY = rowY;
            if (!small.isEmpty()) {
                graphics.pose().pushPose();
                graphics.pose().translate(x, rowY, 0.0F);
                graphics.pose().scale(SMALL_ICON / 16.0F, SMALL_ICON / 16.0F, 1.0F);
                graphics.renderFakeItem(small, 0, 0);
                graphics.pose().popPose();
                textX = x + SMALL_ICON + 3;
                textY = rowY + (SMALL_ICON - font.lineHeight) / 2;
            }
            graphics.drawString(font, line.text(), textX, textY, palette.text(), false);
            int bottom = rowY + Math.max(font.lineHeight, small.isEmpty() ? 0 : SMALL_ICON);
            if (line.hasBar()) {
                graphics.fill(x, bottom + 1, x + innerWidth, bottom + 1 + BAR_HEIGHT, palette.barBack());
                graphics.fill(x, bottom + 1, x + Math.round(innerWidth * line.fraction()), bottom + 1 + BAR_HEIGHT,
                        0xFF000000 | line.color());
                bottom += BAR_HEIGHT + 1;
            }
            return bottom + lineGap();
        }
    }
}
