package com.itmecasey.smwti;

import com.itmecasey.smwti.compat.ServerProviders;
import com.itmecasey.smwti.net.LookChannel;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Entry point for Show Me What This Is (Forge 26.1.2), a look-at overlay: point at a block or an
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
        LookChannel.register();
        ServerProviders.register();
        LOGGER.info("Show Me What This Is loaded (Forge 26.1.2)");
    }

    /** An identifier in this mod's namespace. */
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
