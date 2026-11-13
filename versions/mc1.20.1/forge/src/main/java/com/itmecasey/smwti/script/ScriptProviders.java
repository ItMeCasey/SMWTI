package com.itmecasey.smwti.script;

import com.itmecasey.smwti.api.BlockView;
import com.itmecasey.smwti.api.EntityView;
import com.itmecasey.smwti.api.SmwtiApi;
import com.itmecasey.smwti.api.TargetOverride;
import com.itmecasey.smwti.api.Tooltip;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/** Connects {@link ScriptRules} to the overlay: one block provider, one entity provider and one override of each, added once. */
final class ScriptProviders {
    private static boolean registered;

    private ScriptProviders() {
    }

    static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        SmwtiApi.registerBlock((Tooltip tooltip, BlockView view) -> {
            for (ScriptRules.Placed line : ScriptRules.blockLines(info(view))) {
                tooltip.add(line.position(), Component.literal(line.text()));
            }
        });
        SmwtiApi.registerEntity((Tooltip tooltip, EntityView view) -> {
            for (ScriptRules.Placed line : ScriptRules.entityLines(info(view))) {
                tooltip.add(line.position(), Component.literal(line.text()));
            }
        });
        SmwtiApi.overrideBlock(new TargetOverride<BlockView>() {
            @Override
            public Component name(BlockView view) {
                String name = ScriptRules.blockName(id(view.state()));
                return name == null ? null : Component.literal(name);
            }
        });
        SmwtiApi.overrideEntity(new TargetOverride<EntityView>() {
            @Override
            public Component name(EntityView view) {
                String name = ScriptRules.entityName(BuiltInRegistries.ENTITY_TYPE.getKey(view.entity().getType()).toString());
                return name == null ? null : Component.literal(name);
            }
        });
    }

    static String id(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    }

    static ScriptRules.BlockInfo info(BlockView view) {
        Map<String, String> properties = new LinkedHashMap<>();
        for (Property<?> property : view.state().getProperties()) {
            properties.put(property.getName(), String.valueOf(view.state().getValue(property)));
        }
        return new ScriptRules.BlockInfo(id(view.state()), view.pos().getX(), view.pos().getY(), view.pos().getZ(), properties,
                view.detailed());
    }

    static ScriptRules.EntityInfo info(EntityView view) {
        return new ScriptRules.EntityInfo(BuiltInRegistries.ENTITY_TYPE.getKey(view.entity().getType()).toString(),
                view.entity().getName().getString(), view.entity().getX(), view.entity().getY(), view.entity().getZ(),
                view.detailed());
    }
}
