package com.itmecasey.smwti.devplugin;

import com.itmecasey.smwti.api.ClientRegistration;
import com.itmecasey.smwti.api.Element;
import com.itmecasey.smwti.api.ISmwtiPlugin;
import com.itmecasey.smwti.api.Position;
import com.itmecasey.smwti.api.RenderCallback;
import com.itmecasey.smwti.api.SmwtiPlugin;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;

/**
 * A plugin for dev runs only: it uses the parts of the API no other mod uses yet (a custom element, a block
 * redirect, a render callback, a slider and a choice setting) so the screenshot tour can check each draws.
 * It is compiled into the dev run only with {@code -PdevPlugin}; a release build never contains it.
 */
@SmwtiPlugin
public final class TestPlugin implements ISmwtiPlugin {
    @Override
    public void registerClient(ClientRegistration registration) {
        registration.option(Identifier.fromNamespaceAndPath("smwti", "devtest_flag"), true);
        registration.intSetting(Identifier.fromNamespaceAndPath("smwti", "devtest_slider"), 5, 0, 10);
        registration.choice(Identifier.fromNamespaceAndPath("smwti", "devtest_choice"), List.of("one", "two", "three"), "two");

        // A custom element on iron ore: a green bar with a label, drawn by the plugin itself.
        registration.block((tooltip, view) -> {
            if (view.state().is(Blocks.IRON_ORE)) {
                tooltip.element(Position.BODY, new Element() {
                    @Override
                    public int width(Font font) {
                        return font.width("devtest element") + 6;
                    }

                    @Override
                    public int height(Font font) {
                        return font.lineHeight + 4;
                    }

                    @Override
                    public void draw(GuiGraphicsExtractor graphics, Font font, int x, int y, int width) {
                        graphics.fill(x, y, x + width, y + height(font), 0xFF1E7A1E);
                        graphics.text(font, "devtest element", x + 3, y + 2, 0xFFFFFFFF, false);
                    }
                });
            }
        });

        // A redirect: looking at stone shows the beacon the tour builds at x=24.
        registration.redirect((level, pos, state) -> state.is(Blocks.STONE) ? new BlockPos(24, -60, 0) : null);

        // A render callback: a magenta frame around the panel.
        registration.renderCallback(new RenderCallback() {
            @Override
            public void after(GuiGraphicsExtractor graphics, Panel panel) {
                int color = 0xFFFF00FF;
                graphics.fill(panel.x() - 1, panel.y() - 1, panel.x() + panel.width() + 1, panel.y(), color);
                graphics.fill(panel.x() - 1, panel.y() + panel.height(), panel.x() + panel.width() + 1,
                        panel.y() + panel.height() + 1, color);
                graphics.fill(panel.x() - 1, panel.y(), panel.x(), panel.y() + panel.height(), color);
                graphics.fill(panel.x() + panel.width(), panel.y(), panel.x() + panel.width() + 1, panel.y() + panel.height(), color);
            }
        });
    }
}
