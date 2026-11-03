package com.itmecasey.smwti.client;

import com.itmecasey.smwti.SmwtiConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Lets the player drag the overlay anywhere on the screen. It shows a sample panel where the
 * overlay is now; dragging moves it, and closing the screen keeps the new spot (the anchor becomes
 * custom). While it is dragged it clings softly to the centers and the corners, keeps clear of
 * the screen's edges and stays off the hotbar (see {@link OverlayPlacement}). The spot is stored as fractions of the room the panel has
 * to move in, so it still fits when the window or the overlay's size changes.
 */
public final class PositionScreen extends Screen {
    private static final int GUIDE = 0x80FFFFFF;
    private static final int HOTBAR = 0x30FFFFFF;

    private final @Nullable Screen parent;
    private double fractionX = SmwtiConfig.positionX();
    private double fractionY = SmwtiConfig.positionY();
    /** Where the panel was, in GUI pixels: x, y, width, height; set each frame. */
    private int[] panel = {0, 0, 0, 0};
    private boolean dragging;
    /** Whether the panel is clinging to the middle of the screen across or down, which draws a guide line. */
    private boolean centeredX;
    private boolean centeredY;
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
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        Component hint = Component.translatable("smwti.position.hint");
        graphics.drawString(font, hint, (width - font.width(hint)) / 2, height - 46, 0xFFFFFFFF, true);
        // Where the hotbar will be in the game, which the panel keeps off.
        graphics.fill(width / 2 - 91, height - 22, width / 2 + 91, height, HOTBAR);
        if (centeredX) {
            graphics.fill(width / 2, 0, width / 2 + 1, height, GUIDE);
        }
        if (centeredY) {
            graphics.fill(0, height / 2, width, height / 2 + 1, GUIDE);
        }
        panel = OverlayLayer.drawSample(graphics, font, fractionX, fractionY);
    }

    private boolean onPanel(double x, double y) {
        return x >= panel[0] && x < panel[0] + panel[2] && y >= panel[1] && y < panel[1] + panel[3];
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && onPanel(mouseX, mouseY)) {
            dragging = true;
            grabX = mouseX - panel[0];
            grabY = mouseY - panel[1];
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (!dragging) {
            return super.mouseDragged(mouseX, mouseY, button, dx, dy);
        }
        // Where the panel's corner would be, clung to the centers and corners when it is near them,
        // and kept off the edges and the hotbar.
        OverlayPlacement.Snapped spot = OverlayPlacement.snap(width, height, panel[2], panel[3],
                (int) Math.round(mouseX - grabX), (int) Math.round(mouseY - grabY));
        double[] fractions = OverlayPlacement.toFractions(width, height, panel[2], panel[3], spot.x(), spot.y());
        fractionX = fractions[0];
        fractionY = fractions[1];
        centeredX = spot.centeredX();
        centeredY = spot.centeredY();
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        centeredX = false;
        centeredY = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        SmwtiConfig.setPosition(fractionX, fractionY);
        SmwtiConfig.save();
        minecraft.setScreen(parent);
    }
}
