package com.itmecasey.smwti.client;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * The things the player hid from the overlay, a page at a time, each with a button to show it
 * again. Hiding happens with the hide key while looking at something.
 */
public final class HiddenScreen extends Screen {
    private static final int ROWS = 8;
    private static final int WIDTH = 240;

    private final @Nullable Screen parent;
    private int page;

    public HiddenScreen(@Nullable Screen parent) {
        super(Component.translatable("smwti.hidden.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
        List<String> hidden = UserHidden.all();
        int pages = Math.max(1, (hidden.size() + ROWS - 1) / ROWS);
        page = Math.min(page, pages - 1);
        int left = (width - WIDTH) / 2;
        for (int row = 0; row < ROWS; row++) {
            int index = page * ROWS + row;
            if (index >= hidden.size()) {
                break;
            }
            String id = hidden.get(index);
            addRenderableWidget(Button.builder(Component.translatable("smwti.hidden.show"), button -> {
                UserHidden.remove(id);
                init();
            }).bounds(left + WIDTH - 70, 36 + row * 22, 70, 20).build());
        }
        int bottom = 36 + ROWS * 22 + 6;
        Button previous = addRenderableWidget(Button.builder(Component.literal("<"), button -> {
            page--;
            init();
        }).bounds(left, bottom, 20, 20).build());
        previous.active = page > 0;
        Button next = addRenderableWidget(Button.builder(Component.literal(">"), button -> {
            page++;
            init();
        }).bounds(left + WIDTH - 20, bottom, 20, 20).build());
        next.active = page < pages - 1;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(width / 2 - 50, bottom, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawString(font, title, (width - font.width(title)) / 2, 16, 0xFFFFFFFF, true);
        List<String> hidden = UserHidden.all();
        if (hidden.isEmpty()) {
            return;
        }
        int left = (width - WIDTH) / 2;
        for (int row = 0; row < ROWS && page * ROWS + row < hidden.size(); row++) {
            graphics.drawString(font, hidden.get(page * ROWS + row), left, 36 + row * 22 + 6, 0xFFFFFFFF, false);
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
