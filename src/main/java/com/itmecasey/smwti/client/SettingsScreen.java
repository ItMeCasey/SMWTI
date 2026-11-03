package com.itmecasey.smwti.client;

import com.itmecasey.smwti.SmwtiConfig;
import com.itmecasey.smwti.api.Option;
import com.itmecasey.smwti.api.SmwtiApi;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * The settings screen: the overlay's own settings first, then one switch for every option a mod
 * has registered. Changes apply at once and are written when the screen closes.
 */
public final class SettingsScreen extends Screen {
    private static final int WIDTH = 220;
    private static final int ROW = 24;
    private static final List<Double> SCALES = List.of(0.5, 0.75, 1.0, 1.25, 1.5, 2.0);

    private final @Nullable Screen parent;

    public SettingsScreen(@Nullable Screen parent) {
        super(Component.translatable("smwti.settings.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = (width - WIDTH) / 2;
        int y = 36;
        addRenderableWidget(CycleButton.onOffBuilder(SmwtiConfig.enabled())
                .create(left, y, WIDTH, 20, Component.translatable("smwti.settings.enabled"),
                        (button, value) -> SmwtiConfig.setEnabled(value)));
        y += ROW;
        addRenderableWidget(CycleButton.builder(SettingsScreen::anchorName, SmwtiConfig.anchor())
                .withValues(SmwtiConfig.Anchor.values())
                .create(left, y, WIDTH, 20, Component.translatable("smwti.settings.anchor"),
                        (button, value) -> SmwtiConfig.setAnchor(value)));
        y += ROW;
        double current = SmwtiConfig.scale();
        double nearest = SCALES.stream().min((a, b) -> Double.compare(Math.abs(a - current), Math.abs(b - current))).orElse(1.0);
        addRenderableWidget(CycleButton.builder((Double scale) -> Component.literal(scale + "x"), nearest)
                .withValues(SCALES)
                .create(left, y, WIDTH, 20, Component.translatable("smwti.settings.scale"),
                        (button, value) -> SmwtiConfig.setScale(value)));
        y += ROW;
        addRenderableWidget(CycleButton.builder(SettingsScreen::detailName, SmwtiConfig.detail())
                .withValues(SmwtiConfig.Detail.values())
                .create(left, y, WIDTH, 20, Component.translatable("smwti.settings.detail"),
                        (button, value) -> SmwtiConfig.setDetail(value)));
        y += ROW;
        addRenderableWidget(CycleButton.onOffBuilder(SmwtiConfig.showModName())
                .create(left, y, WIDTH, 20, Component.translatable("smwti.settings.mod_name"),
                        (button, value) -> SmwtiConfig.setShowModName(value)));
        y += ROW;
        addRenderableWidget(CycleButton.onOffBuilder(SmwtiConfig.showFluids())
                .create(left, y, WIDTH, 20, Component.translatable("smwti.settings.fluids"),
                        (button, value) -> SmwtiConfig.setShowFluids(value)));
        y += ROW;
        addRenderableWidget(CycleButton.onOffBuilder(SmwtiConfig.hideWhileSneaking())
                .create(left, y, WIDTH, 20, Component.translatable("smwti.settings.hide_sneaking"),
                        (button, value) -> SmwtiConfig.setHideWhileSneaking(value)));
        y += ROW;
        for (Option option : SmwtiApi.options()) {
            addRenderableWidget(CycleButton.onOffBuilder(option.enabled())
                    .create(left, y, WIDTH, 20, Component.translatable(option.translationKey()),
                            (button, value) -> Option.choose(option.id(), value)));
            y += ROW;
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(left, Math.min(y + 8, height - 28), WIDTH, 20).build());
    }

    private static Component anchorName(SmwtiConfig.Anchor anchor) {
        return Component.translatable("smwti.settings.anchor." + anchor.name().toLowerCase(java.util.Locale.ROOT));
    }

    private static Component detailName(SmwtiConfig.Detail detail) {
        return Component.translatable("smwti.settings.detail." + detail.name().toLowerCase(java.util.Locale.ROOT));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.text(font, title, (width - font.width(title)) / 2, 16, 0xFFFFFFFF, true);
    }

    @Override
    public void onClose() {
        SmwtiConfig.save();
        OptionsFile.save();
        minecraft.setScreen(parent);
    }
}
