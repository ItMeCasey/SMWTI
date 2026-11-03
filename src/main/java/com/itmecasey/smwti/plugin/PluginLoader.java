package com.itmecasey.smwti.plugin;

import com.itmecasey.smwti.Smwti;
import com.itmecasey.smwti.api.BlockProvider;
import com.itmecasey.smwti.api.BlockRedirect;
import com.itmecasey.smwti.api.BlockView;
import com.itmecasey.smwti.api.ChoiceSetting;
import com.itmecasey.smwti.api.ClientRegistration;
import com.itmecasey.smwti.api.CommonRegistration;
import com.itmecasey.smwti.api.EntityProvider;
import com.itmecasey.smwti.api.EntityView;
import com.itmecasey.smwti.api.ISmwtiPlugin;
import com.itmecasey.smwti.api.IntSetting;
import com.itmecasey.smwti.api.Option;
import com.itmecasey.smwti.api.RenderCallback;
import com.itmecasey.smwti.api.ServerBlockProvider;
import com.itmecasey.smwti.api.ServerEntityProvider;
import com.itmecasey.smwti.api.SmwtiApi;
import com.itmecasey.smwti.api.SmwtiPlugin;
import com.itmecasey.smwti.api.TargetOverride;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.ModFileScanData;
import org.objectweb.asm.Type;

/**
 * Finds the classes marked {@link SmwtiPlugin} in every installed mod, makes one of each and lets
 * it register. The scan reads the class files Forge has already indexed, so no plugin class is
 * loaded unless its required mod is there. A plugin that fails to load or throws is logged and
 * skipped; it never stops the others or the game.
 */
public final class PluginLoader {
    private static final String ANNOTATION = Type.getDescriptor(SmwtiPlugin.class);

    /** One plugin class found by the scan and the mod it needs, if any. */
    private record Found(String className, String requiredMod) {
    }

    private static List<ISmwtiPlugin> plugins = List.of();

    private PluginLoader() {
    }

    /** Finds and creates the plugins and runs their common registration; on both sides, once. */
    public static void loadCommon() {
        List<ISmwtiPlugin> loaded = new ArrayList<>();
        for (Found found : scan()) {
            if (!found.requiredMod().isEmpty() && !ModList.isLoaded(found.requiredMod())) {
                continue;
            }
            try {
                Class<?> type = Class.forName(found.className(), true, PluginLoader.class.getClassLoader());
                loaded.add((ISmwtiPlugin) type.getDeclaredConstructor().newInstance());
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                Smwti.LOGGER.warn("Could not load the SMWTI plugin {}", found.className(), e);
            }
        }
        plugins = List.copyOf(loaded);
        for (ISmwtiPlugin plugin : plugins) {
            try {
                plugin.register(COMMON);
            } catch (RuntimeException | LinkageError e) {
                Smwti.LOGGER.warn("The SMWTI plugin {} failed to register", plugin.getClass().getName(), e);
            }
        }
        Smwti.LOGGER.info("Loaded {} SMWTI plugin(s)", plugins.size());
    }

    /** Runs the client registration of the plugins loaded by {@link #loadCommon}; on the client, once. */
    public static void loadClient() {
        for (ISmwtiPlugin plugin : plugins) {
            try {
                plugin.registerClient(CLIENT);
            } catch (RuntimeException | LinkageError e) {
                Smwti.LOGGER.warn("The SMWTI plugin {} failed to register on the client", plugin.getClass().getName(), e);
            }
        }
    }

    private static List<Found> scan() {
        List<Found> found = new ArrayList<>();
        for (ModFileScanData data : ModList.getAllScanData()) {
            for (ModFileScanData.AnnotationData annotation : data.getAnnotations()) {
                if (annotation.annotationType().getDescriptor().equals(ANNOTATION)
                        && annotation.targetType() == java.lang.annotation.ElementType.TYPE) {
                    Object required = annotation.annotationData().get("value");
                    found.add(new Found(annotation.clazz().getClassName(), required instanceof String id ? id : ""));
                }
            }
        }
        return found;
    }

    private static final CommonRegistration COMMON = new CommonRegistration() {
        @Override
        public void serverBlock(ServerBlockProvider provider) {
            SmwtiApi.registerServerBlock(provider);
        }

        @Override
        public void serverEntity(ServerEntityProvider provider) {
            SmwtiApi.registerServerEntity(provider);
        }
    };

    private static final ClientRegistration CLIENT = new ClientRegistration() {
        @Override
        public void block(BlockProvider provider) {
            SmwtiApi.registerBlock(provider);
        }

        @Override
        public void entity(EntityProvider provider) {
            SmwtiApi.registerEntity(provider);
        }

        @Override
        public void overrideBlock(TargetOverride<BlockView> override) {
            SmwtiApi.overrideBlock(override);
        }

        @Override
        public void overrideEntity(TargetOverride<EntityView> override) {
            SmwtiApi.overrideEntity(override);
        }

        @Override
        public void redirect(BlockRedirect redirect) {
            SmwtiApi.registerRedirect(redirect);
        }

        @Override
        public void renderCallback(RenderCallback callback) {
            SmwtiApi.registerRenderCallback(callback);
        }

        @Override
        public Option option(Identifier id, boolean defaultValue) {
            return SmwtiApi.registerOption(id, defaultValue);
        }

        @Override
        public IntSetting intSetting(Identifier id, int defaultValue, int min, int max) {
            return SmwtiApi.registerInt(id, defaultValue, min, max);
        }

        @Override
        public ChoiceSetting choice(Identifier id, List<String> choices, String defaultValue) {
            return SmwtiApi.registerChoice(id, choices, defaultValue);
        }
    };
}
