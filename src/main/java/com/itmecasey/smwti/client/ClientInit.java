package com.itmecasey.smwti.client;

import com.itmecasey.smwti.Smwti;
import com.itmecasey.smwti.SmwtiConfig;
import com.itmecasey.smwti.compat.DefaultProviders;
import com.itmecasey.smwti.compat.VanillaProviders;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

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
            KeyModifier.CONTROL, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F10, CATEGORY, 0);
    /** F10 switches the overlay on and off. */
    private static final KeyMapping TOGGLE = new KeyMapping("key.smwti.toggle", KeyConflictContext.UNIVERSAL,
            KeyModifier.NONE, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F10, CATEGORY, 0);

    private ClientInit() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        DefaultProviders.registerClient();
        VanillaProviders.register();
        PackRules.register();
        event.enqueueWork(OptionsFile::load);
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(SETTINGS);
        event.register(TOGGLE);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        Minecraft game = Minecraft.getInstance();
        while (SETTINGS.consumeClick()) {
            if (game.screen == null) {
                game.setScreen(new SettingsScreen(null));
            }
        }
        while (TOGGLE.consumeClick()) {
            SmwtiConfig.setEnabled(!SmwtiConfig.enabled());
            SmwtiConfig.save();
        }
    }
}
