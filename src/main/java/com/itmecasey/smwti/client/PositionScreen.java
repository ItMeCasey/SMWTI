package com.itmecasey.smwti.client;

import com.itmecasey.smwti.SmwtiConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Lets the player drag the overlay anywhere on the screen. It shows a sample panel where the
 * overlay is now; dragging moves it, and closing the screen keeps the new spot (the anchor becomes
 * custom). The spot is stored as fractions of the room the panel has to move in, so it still fits
 * when the window or the overlay's size changes.
 */
public final class PositionScreen extends Screen {
    private final @Nullable Screen parent;
    private double fractionX = SmwtiConfig.positionX();
    private double fractionY = SmwtiConfig.positionY();
    /** Where the panel was, in GUI pixels: x, y, width, height; set each frame. */
    private int[] panel = {0, 0, 0, 0};
    private boolean dragging;
    private double grabX;
    private double grabY;

    public PositionScreen(@Nullable Screen parent) {
        super(Component.translatable("smwti.position.title"));
        this.parent = parent;
        // Start from where the overlay really is now, even when it hangs from an anchor.
        if (SmwtiConfig.anchor() != SmwtiConfig.Anchor.CUSTOM) {
            fractionY = 0.0;
            fractionX = switch (SmwtiConfig.anchor()) {
                case TOP_LEFT -> 0.0;
                case TOP_RIGHT -> 1.0;
                default -> 0.5;
            };
        }
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("smwti.position.reset"), button -> {
            fractionX = 0.5;
            fractionY = 0.0;
        }).bounds(width / 2 - 104, height - 28, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(width / 2 + 4, height - 28, 100, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        Component hint = Component.translatable("smwti.position.hint");
        graphics.text(font, hint, (width - font.width(hint)) / 2, height - 46, 0xFFFFFFFF, true);
        panel = OverlayLayer.drawSample(graphics, font, fractionX, fractionY);
    }

    private boolean onPanel(double x, double y) {
        return x >= panel[0] && x < panel[0] + panel[2] && y >= panel[1] && y < panel[1] + panel[3];
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && onPanel(event.x(), event.y())) {
            dragging = true;
            grabX = event.x() - panel[0];
            grabY = event.y() - panel[1];
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (!dragging) {
            return super.mouseDragged(event, dx, dy);
        }
        // The panel can move across the room left over once its own size is taken out.
        double roomX = width - panel[2];
        double roomY = height - panel[3];
        fractionX = roomX <= 0 ? 0.0 : Math.max(0.0, Math.min(1.0, (event.x() - grabX) / roomX));
        fractionY = roomY <= 0 ? 0.0 : Math.max(0.0, Math.min(1.0, (event.y() - grabY) / roomY));
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = false;
        return super.mouseReleased(event);
    }

    @Override
    public void onClose() {
        SmwtiConfig.setPosition(fractionX, fractionY);
        SmwtiConfig.save();
        minecraft.setScreen(parent);
    }
}
