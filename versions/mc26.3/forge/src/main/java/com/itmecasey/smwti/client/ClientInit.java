package com.itmecasey.smwti.client;

import com.itmecasey.smwti.Smwti;
import com.itmecasey.smwti.SmwtiConfig;
import com.itmecasey.smwti.compat.DefaultProviders;
import com.itmecasey.smwti.compat.VanillaMoreProviders;
import com.itmecasey.smwti.compat.VanillaProviders;
import com.itmecasey.smwti.plugin.PluginLoader;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client-only setup: the providers that read the client's world, the pack rules, the saved
 * options and the two keys, one that opens the settings screen and one that switches the overlay
 * on and off.
 */
@Mod.EventBusSubscriber(modid = Smwti.MODID, value = Dist.CLIENT)
public final class ClientInit {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Smwti.id("main"));
    /** Control+F10 opens the settings screen. */
    private static final KeyMapping SETTINGS = new KeyMapping("key.smwti.settings", KeyConflictContext.UNIVERSAL,
            KeyModifier.CONTROL, InputConstants.Type.KEYBOARD, InputConstants.KEY_F10, CATEGORY, 0);
    /** F10 switches the overlay on and off. */
    private static final KeyMapping TOGGLE = new KeyMapping("key.smwti.toggle", KeyConflictContext.UNIVERSAL,
            KeyModifier.NONE, InputConstants.Type.KEYBOARD, InputConstants.KEY_F10, CATEGORY, 0);

    /** Left Alt, held to show the overlay when the hold-to-show setting is on. */
    private static final KeyMapping HOLD = new KeyMapping("key.smwti.hold", KeyConflictContext.UNIVERSAL,
            KeyModifier.NONE, InputConstants.Type.KEYBOARD, InputConstants.KEY_LALT, CATEGORY, 0);
    /** Shift+F10 hides what the crosshair is on from the overlay. */
    private static final KeyMapping HIDE = new KeyMapping("key.smwti.hide", KeyConflictContext.UNIVERSAL,
            KeyModifier.SHIFT, InputConstants.Type.KEYBOARD, InputConstants.KEY_F10, CATEGORY, 0);

    private ClientInit() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        DefaultProviders.registerClient();
        VanillaProviders.register();
        VanillaMoreProviders.register();
        PackRules.register();
        // Plugins registered their common parts during common setup, which runs first on the client too.
        event.enqueueWork(PluginLoader::loadClient);
        // The mod list's config button opens the settings screen.
        ModList.getModContainerById(Smwti.MODID).ifPresent(container -> container.registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((game, parent) -> new SettingsScreen(parent))));
        event.enqueueWork(OptionsFile::load);
        event.enqueueWork(UserHidden::load);
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(SETTINGS);
        event.register(TOGGLE);
        event.register(HOLD);
        event.register(HIDE);
    }

    /** Whether the hold-to-show key is down. */
    public static boolean holdKeyDown() {
        return HOLD.isDown();
    }

    /** Hides the block or entity type under the crosshair, and says so above the hotbar. */
    private static void hideTarget(Minecraft game) {
        String id = null;
        Component name = null;
        if (LookTracker.block() != null && game.level != null) {
            var block = game.level.getBlockState(LookTracker.block()).getBlock();
            id = BuiltInRegistries.BLOCK.getKey(block).toString();
            name = block.getName();
        } else if (LookTracker.entity() != null) {
            id = BuiltInRegistries.ENTITY_TYPE.getKey(LookTracker.entity().getType()).toString();
            name = LookTracker.entity().getType().getDescription();
        }
        if (id != null && game.player != null) {
            UserHidden.add(id);
            game.gui.hud.setOverlayMessage(Component.translatable("smwti.hidden.added", name), false);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        Minecraft game = Minecraft.getInstance();
        while (SETTINGS.consumeClick()) {
            if (game.gui.screen() == null) {
                game.gui.setScreen(new SettingsScreen(null));
            }
        }
        while (HIDE.consumeClick()) {
            if (game.gui.screen() == null) {
                hideTarget(game);
            }
        }
        while (TOGGLE.consumeClick()) {
            SmwtiConfig.setEnabled(!SmwtiConfig.enabled());
            SmwtiConfig.save();
        }
    }
}
