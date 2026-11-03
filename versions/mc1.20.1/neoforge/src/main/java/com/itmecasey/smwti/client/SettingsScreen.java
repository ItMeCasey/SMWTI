package com.itmecasey.smwti.client;

import com.itmecasey.smwti.SmwtiConfig;
import com.itmecasey.smwti.api.ChoiceSetting;
import com.itmecasey.smwti.api.IntSetting;
import com.itmecasey.smwti.api.Option;
import com.itmecasey.smwti.api.Setting;
import com.itmecasey.smwti.api.SettingValues;
import com.itmecasey.smwti.api.SmwtiApi;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * The settings screen: the overlay's own settings first, then one control for every setting a mod
 * has registered (a switch, a slider or a cycling choice), laid out in two columns. Changes apply
 * at once and are written when the screen closes.
 */
public final class SettingsScreen extends Screen {
    private static final int COLUMN = 150;
    private static final int GAP = 8;
    private static final int ROW = 24;
    private static final List<Double> SCALES = List.of(0.5, 0.75, 1.0, 1.25, 1.5, 2.0);

    private final @Nullable Screen parent;
    /** How many controls have been placed, which decides the next one's column and row. */
    private int placed;

    public SettingsScreen(@Nullable Screen parent) {
        super(Component.translatable("smwti.settings.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        placed = 0;
        addControl(CycleButton.onOffBuilder(SmwtiConfig.enabled()), "smwti.settings.enabled",
                (button, value) -> SmwtiConfig.setEnabled(value));
        addControl(CycleButton.builder(SettingsScreen::anchorName)
                .withValues(SmwtiConfig.Anchor.values()).withInitialValue(SmwtiConfig.anchor()), "smwti.settings.anchor",
                (button, value) -> SmwtiConfig.setAnchor(value));
        addRenderableWidget(Button.builder(Component.translatable("smwti.settings.move"),
                button -> minecraft.setScreen(new PositionScreen(this))).bounds(columnX(placed), rowY(placed), COLUMN, 20).build());
        placed++;
        addControl(CycleButton.builder((Double scale) -> Component.literal(scale + "x"))
                .withValues(SCALES).withInitialValue(nearest(SCALES, SmwtiConfig.scale())), "smwti.settings.scale", (button, value) -> SmwtiConfig.setScale(value));
        addControl(CycleButton.builder(SettingsScreen::themeName)
                .withValues(Themes.names()).withInitialValue(Themes.names().contains(SmwtiConfig.theme())
                        ? SmwtiConfig.theme() : "modern"), "smwti.settings.theme",
                (button, value) -> SmwtiConfig.setTheme(value));
        addRenderableWidget(new OpacitySlider(columnX(placed), rowY(placed)));
        placed++;
        addControl(CycleButton.builder(SettingsScreen::detailName)
                .withValues(SmwtiConfig.Detail.values()).withInitialValue(SmwtiConfig.detail()), "smwti.settings.detail",
                (button, value) -> SmwtiConfig.setDetail(value));
        addControl(CycleButton.onOffBuilder(SmwtiConfig.showModName()), "smwti.settings.mod_name",
                (button, value) -> SmwtiConfig.setShowModName(value));
        addControl(CycleButton.onOffBuilder(SmwtiConfig.holdToShow()), "smwti.settings.hold",
                (button, value) -> SmwtiConfig.setHoldToShow(value));
        addControl(CycleButton.onOffBuilder(SmwtiConfig.compact()), "smwti.settings.compact",
                (button, value) -> SmwtiConfig.setCompact(value));
        addControl(CycleButton.onOffBuilder(SmwtiConfig.showFluids()), "smwti.settings.fluids",
                (button, value) -> SmwtiConfig.setShowFluids(value));
        addControl(CycleButton.onOffBuilder(SmwtiConfig.hideWhileSneaking()), "smwti.settings.hide_sneaking",
                (button, value) -> SmwtiConfig.setHideWhileSneaking(value));

        addRenderableWidget(Button.builder(Component.translatable("smwti.settings.hidden"),
                button -> minecraft.setScreen(new HiddenScreen(this))).bounds(columnX(placed), rowY(placed), COLUMN, 20).build());
        placed++;

        for (Setting setting : SmwtiApi.settings()) {
            if (setting instanceof Option option) {
                addControl(CycleButton.onOffBuilder(option.enabled()), option.translationKey(),
                        (button, value) -> SettingValues.set(option.id(), value));
            } else if (setting instanceof ChoiceSetting choice) {
                addControl(CycleButton.builder(
                                (String value) -> Component.translatable(choice.choiceKey(value)))
                        .withValues(choice.choices()).withInitialValue(choice.value()), choice.translationKey(),
                        (button, value) -> SettingValues.set(choice.id(), value));
            } else if (setting instanceof IntSetting number) {
                addRenderableWidget(new IntSlider(columnX(placed), rowY(placed), number));
            }
            if (setting instanceof IntSetting) {
                placed++;
            }
        }
        int bottom = rowY(placed + 1) + ROW;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds((width - COLUMN) / 2, Math.min(bottom, height - 28), COLUMN, 20).build());
    }

    private int columnX(int index) {
        int left = (width - (2 * COLUMN + GAP)) / 2;
        return left + (index % 2) * (COLUMN + GAP);
    }

    private int rowY(int index) {
        return 36 + (index / 2) * ROW;
    }

    /** Places a cycling control in the next free spot. */
    private <T> void addControl(CycleButton.Builder<T> builder, String labelKey, CycleButton.OnValueChange<T> onChange) {
        addRenderableWidget(builder.create(columnX(placed), rowY(placed), COLUMN, 20, Component.translatable(labelKey), onChange));
        placed++;
    }

    private static double nearest(List<Double> values, double current) {
        return values.stream().min((a, b) -> Double.compare(Math.abs(a - current), Math.abs(b - current))).orElse(values.get(0));
    }

    private static Component anchorName(SmwtiConfig.Anchor anchor) {
        return Component.translatable("smwti.settings.anchor." + anchor.name().toLowerCase(Locale.ROOT));
    }

    private static Component themeName(String theme) {
        return Themes.BUILT_IN.contains(theme) ? Component.translatable("smwti.settings.theme." + theme) : Component.literal(theme);
    }

    private static Component detailName(SmwtiConfig.Detail detail) {
        return Component.translatable("smwti.settings.detail." + detail.name().toLowerCase(Locale.ROOT));
    }

    /** A slider for how solid the backdrop is, from 20% to 100%. */
    private static final class OpacitySlider extends AbstractSliderButton {
        private static final double MIN = 0.2;

        OpacitySlider(int x, int y) {
            super(x, y, COLUMN, 20, Component.empty(), (SmwtiConfig.opacity() - MIN) / (1.0 - MIN));
            updateMessage();
        }

        private double opacity() {
            return MIN + value * (1.0 - MIN);
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("smwti.settings.opacity").append(": " + Math.round(opacity() * 100) + "%"));
        }

        @Override
        protected void applyValue() {
            SmwtiConfig.setOpacity(Math.round(opacity() * 100) / 100.0);
        }
    }

    /** A slider over a whole-number range. */
    private static final class IntSlider extends AbstractSliderButton {
        private final IntSetting setting;

        IntSlider(int x, int y, IntSetting setting) {
            super(x, y, COLUMN, 20, Component.empty(), fraction(setting, setting.value()));
            this.setting = setting;
            updateMessage();
        }

        private static double fraction(IntSetting setting, int value) {
            return setting.max() == setting.min() ? 0.0 : (double) (value - setting.min()) / (setting.max() - setting.min());
        }

        private int current() {
            return setting.min() + (int) Math.round(value * (setting.max() - setting.min()));
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable(setting.translationKey()).append(": " + current()));
        }

        @Override
        protected void applyValue() {
            SettingValues.set(setting.id(), current());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawString(font, title, (width - font.width(title)) / 2, 16, 0xFFFFFFFF, true);
    }

    @Override
    public void onClose() {
        SmwtiConfig.save();
        OptionsFile.save();
        minecraft.setScreen(parent);
    }
}
