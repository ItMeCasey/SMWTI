package com.itmecasey.smwti;

import com.itmecasey.smwti.compat.ServerProviders;
import com.itmecasey.smwti.compat.VanillaServerProviders;
import com.itmecasey.smwti.net.LookChannel;
import com.itmecasey.smwti.plugin.PluginLoader;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Entry point for Show Me What This Is (Forge 1.20.1), a look-at overlay: point at a block or an
 * entity and see what it is, which mod it comes from and, where a mod says so, what state it is in.
 * Other mods add lines through {@link com.itmecasey.smwti.api.SmwtiApi}.
 */
@Mod(Smwti.MODID)
public final class Smwti {
    /** Mod id; must match {@code mod_id} in the metadata template. */
    public static final String MODID = "smwti";

    /** Shared logger for the mod. */
    public static final Logger LOGGER = LogUtils.getLogger();

    public Smwti(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.CLIENT, SmwtiConfig.SPEC);
        context.registerConfig(ModConfig.Type.SERVER, SmwtiServerConfig.SPEC);
        LookChannel.register();
        // Plugins other mods ship are found and created once every mod has been constructed.
        context.getModEventBus().addListener((FMLCommonSetupEvent event) -> PluginLoader.loadCommon());
        ServerProviders.register();
        VanillaServerProviders.register();
        LOGGER.info("Show Me What This Is loaded (Forge 1.20.1)");
    }

    /** An identifier in this mod's namespace. */
    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }
}
