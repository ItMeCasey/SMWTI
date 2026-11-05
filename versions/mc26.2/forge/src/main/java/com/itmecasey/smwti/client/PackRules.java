package com.itmecasey.smwti.client;

import com.itmecasey.smwti.Smwti;
import com.itmecasey.smwti.api.BlockView;
import com.itmecasey.smwti.api.EntityView;
import com.itmecasey.smwti.api.SmwtiApi;
import com.itmecasey.smwti.api.TargetOverride;
import com.itmecasey.smwti.api.Tooltip;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Rules from resource packs, so a pack author can tune the overlay without a mod. A file at
 * {@code assets/<namespace>/smwti/rules/<name>.json} may hold any of:
 *
 * <pre>{@code {
 *   "hide": ["minecraft:barrier", "#minecraft:beds"],
 *   "rename": {"minecraft:stone": "Plain Rock", "minecraft:cow": "entity.example.moo"},
 *   "extra_lines": {"minecraft:furnace": ["Burns what you feed it"]}
 * }}</pre>
 *
 * <p>{@code hide} names blocks and entity types (ids, or {@code #tags}) the overlay stays away from.
 * {@code rename} gives a block or entity another name: plain text, or a translation key. {@code extra_lines}
 * adds lines of text (plain or translation keys) to the body. Ids that name nothing do nothing, so a
 * pack can list mods that are not installed. Rules are read again whenever resources reload (F3+T).
 */
@Mod.EventBusSubscriber(modid = Smwti.MODID, value = Dist.CLIENT)
public final class PackRules {
    /** One file's worth of rules. */
    private record Rules(List<String> hide, Map<String, String> rename, Map<String, List<String>> extraLines) {
        static final Codec<Rules> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.listOf().optionalFieldOf("hide", List.of()).forGetter(Rules::hide),
                Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("rename", Map.of()).forGetter(Rules::rename),
                Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf()).optionalFieldOf("extra_lines", Map.of())
                        .forGetter(Rules::extraLines)
        ).apply(instance, Rules::new));
    }

    private static volatile List<String> hidden = List.of();
    private static volatile Map<String, String> renames = Map.of();
    private static volatile Map<String, List<String>> extraLines = Map.of();

    private PackRules() {
    }

    /** Whether a pack asked the overlay to stay away from this block or entity type. */
    public static <T> boolean hides(Holder.Reference<T> holder) {
        for (String entry : hidden) {
            if (entry.startsWith("#")) {
                Identifier tag = Identifier.tryParse(entry.substring(1));
                if (tag != null && holder.is(TagKey.create(holder.key().registryKey(), tag))) {
                    return true;
                }
            } else if (entry.equals(holder.key().identifier().toString())) {
                return true;
            }
        }
        return false;
    }

    /** Wires the rules into the overlay through the same API other mods use. */
    public static void register() {
        SmwtiApi.overrideBlock(new TargetOverride<BlockView>() {
            @Override
            public Component name(BlockView view) {
                return rename(BuiltInRegistries.BLOCK.getKey(view.state().getBlock()));
            }
        });
        SmwtiApi.overrideEntity(new TargetOverride<EntityView>() {
            @Override
            public Component name(EntityView view) {
                return rename(BuiltInRegistries.ENTITY_TYPE.getKey(view.entity().getType()));
            }
        });
        SmwtiApi.registerBlock((tooltip, view) -> lines(tooltip, BuiltInRegistries.BLOCK.getKey(view.state().getBlock())));
        SmwtiApi.registerEntity((tooltip, view) -> lines(tooltip, BuiltInRegistries.ENTITY_TYPE.getKey(view.entity().getType())));
    }

    private static Component rename(Identifier id) {
        String name = renames.get(id.toString());
        return name == null ? null : Component.translatable(name);
    }

    private static void lines(Tooltip tooltip, Identifier id) {
        for (String line : extraLines.getOrDefault(id.toString(), List.of())) {
            tooltip.add(Component.translatable(line));
        }
    }

    @SubscribeEvent
    public static void addReloadListener(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new Loader());
    }

    /** Reads every rule file into the static tables, replacing the earlier ones whole. */
    private static final class Loader extends SimpleJsonResourceReloadListener<Rules> {
        Loader() {
            super(Rules.CODEC, FileToIdConverter.json("smwti/rules"));
        }

        @Override
        protected void apply(Map<Identifier, Rules> files, ResourceManager manager, ProfilerFiller profiler) {
            List<String> hide = new ArrayList<>();
            Map<String, String> rename = new HashMap<>();
            Map<String, List<String>> extra = new HashMap<>();
            for (Rules rules : files.values()) {
                hide.addAll(rules.hide());
                rename.putAll(rules.rename());
                rules.extraLines().forEach((id, lines) -> extra.computeIfAbsent(id, key -> new ArrayList<>()).addAll(lines));
            }
            hidden = List.copyOf(hide);
            renames = Map.copyOf(rename);
            extraLines = Map.copyOf(extra);
            Smwti.LOGGER.info("Read look-at rules from {} file(s)", files.size());
        }
    }
}
