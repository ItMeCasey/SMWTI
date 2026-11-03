package com.itmecasey.smwti.client;

import com.itmecasey.smwti.Smwti;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The themes the player can pick: the five built in and any a resource pack adds. A pack adds one
 * with a file at {@code assets/<namespace>/smwti/themes/<name>.json}, picked in the settings as
 * {@code <namespace>:<name>}:
 *
 * <pre>{@code {
 *   "panel": "#101018", "panel_alpha": 192,
 *   "edge": "#3A3A4C", "edge_alpha": 255,
 *   "text": "#FFFFFF", "sub": "#8E8EA6", "bar_back": "#2A2A34",
 *   "shadow": true, "accent": "#FF8A1F"
 * }}</pre>
 *
 * <p>Every field is optional. An {@code edge_alpha} of 0 means no outline (the backdrop then fills the
 * whole panel), and leaving {@code accent} out means no colored line. The backdrop opacity setting scales
 * the two alpha values.
 */
@Mod.EventBusSubscriber(modid = Smwti.MODID, value = Dist.CLIENT)
public final class Themes {
    /** The built-in theme names, in the order the settings screen cycles them. */
    public static final List<String> BUILT_IN = List.of("dark", "light", "vanilla", "clear", "modern");

    /** One theme file: colors as {@code #RRGGBB} text, alphas from 0 to 255. */
    record Def(String panel, int panelAlpha, String edge, int edgeAlpha, String text, String sub, String barBack,
               boolean shadow, String accent) {
        static final Codec<Integer> ALPHA = Codec.intRange(0, 255);
        static final Codec<String> COLOR = Codec.STRING.validate(text -> parse(text) >= 0
                ? DataResult.success(text) : DataResult.error(() -> "not a #RRGGBB color: " + text));
        static final Codec<Def> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                COLOR.optionalFieldOf("panel", "#101018").forGetter(Def::panel),
                ALPHA.optionalFieldOf("panel_alpha", 192).forGetter(Def::panelAlpha),
                COLOR.optionalFieldOf("edge", "#3A3A4C").forGetter(Def::edge),
                ALPHA.optionalFieldOf("edge_alpha", 255).forGetter(Def::edgeAlpha),
                COLOR.optionalFieldOf("text", "#FFFFFF").forGetter(Def::text),
                COLOR.optionalFieldOf("sub", "#8E8EA6").forGetter(Def::sub),
                COLOR.optionalFieldOf("bar_back", "#2A2A34").forGetter(Def::barBack),
                Codec.BOOL.optionalFieldOf("shadow", true).forGetter(Def::shadow),
                COLOR.optionalFieldOf("accent", "").forGetter(Def::accent)
        ).apply(instance, Def::new));
    }

    private static volatile Map<String, Def> custom = Map.of();

    private Themes() {
    }

    /** Every theme the player can pick: the built-in ones, then the packs'. */
    public static List<String> names() {
        List<String> all = new ArrayList<>(BUILT_IN);
        all.addAll(custom.keySet().stream().sorted().toList());
        return all;
    }

    /** A {@code #RRGGBB} color as a number, or -1 when the text is not one. */
    static int parse(String text) {
        if (text.isEmpty()) {
            return 0;
        }
        if (text.length() != 7 || text.charAt(0) != '#') {
            return -1;
        }
        try {
            return Integer.parseInt(text.substring(1), 16);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** The palette of a theme a pack added, with the opacity applied, or {@code null} when there is no such theme. */
    static Palette custom(String name, float opacity) {
        Def def = custom.get(name);
        if (def == null) {
            return null;
        }
        int accent = def.accent().isEmpty() ? 0 : 0xFF000000 | parse(def.accent());
        return new Palette(argb(def.panelAlpha(), opacity, parse(def.panel())), argb(def.edgeAlpha(), opacity, parse(def.edge())),
                0xFF000000 | parse(def.text()), 0xFF000000 | parse(def.sub()), 0xFF000000 | parse(def.barBack()), def.shadow(), accent);
    }

    private static int argb(int alpha, float opacity, int rgb) {
        return (Math.max(0, Math.min(255, Math.round(alpha * opacity))) << 24) | rgb;
    }

    @SubscribeEvent
    public static void addReloadListener(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new Loader());
    }

    /** Reads every theme file into the table, replacing the earlier ones whole. */
    private static final class Loader extends SimpleJsonResourceReloadListener {
        Loader() {
            super(new Gson(), "smwti/themes");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
            Map<String, Def> loaded = new HashMap<>();
            files.forEach((id, element) -> Def.CODEC.parse(JsonOps.INSTANCE, element)
                    .resultOrPartial(error -> Smwti.LOGGER.warn("Skipping look-at theme {}: {}", id, error))
                    .ifPresent(def -> loaded.put(id.toString(), def)));
            custom = Map.copyOf(loaded);
            Smwti.LOGGER.info("Read {} look-at theme(s) from resource packs", loaded.size());
        }
    }
}
