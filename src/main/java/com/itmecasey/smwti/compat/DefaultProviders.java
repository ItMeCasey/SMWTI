package com.itmecasey.smwti.compat;

import com.itmecasey.smwti.api.BlockView;
import com.itmecasey.smwti.api.SmwtiApi;
import com.itmecasey.smwti.api.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What SMWTI itself says about vanilla things, through the same API other mods use: how far a crop
 * has grown, and the health of a living thing. The name, icon and mod are drawn by the overlay for
 * every target and need no provider.
 */
public final class DefaultProviders {
    private static final int HEALTH_RED = 0xE04848;
    private static final int GROWTH_GREEN = 0x5DBB63;

    private DefaultProviders() {
    }

    /** Registers the built-in providers; called once while the mod loads. */
    public static void register() {
        SmwtiApi.registerBlock(DefaultProviders::crop);
        SmwtiApi.registerEntity(DefaultProviders::health);
    }

    /** A crop's growth as a percentage with a bar. */
    private static void crop(Tooltip tooltip, BlockView view) {
        BlockState state = view.state();
        if (state.getBlock() instanceof CropBlock crop) {
            int age = crop.getAge(state);
            int max = crop.getMaxAge();
            if (age >= max) {
                tooltip.bar(Component.translatable("smwti.crop.mature"), 1.0, GROWTH_GREEN);
            } else {
                tooltip.bar(Component.translatable("smwti.crop.growth", Math.round(100.0 * age / max)),
                        (double) age / max, GROWTH_GREEN);
            }
        }
    }

    /** Health of a living entity, in hearts of two points each. */
    private static void health(Tooltip tooltip, Entity entity) {
        if (entity instanceof LivingEntity living && living.getMaxHealth() > 0) {
            tooltip.bar(Component.translatable("smwti.entity.health", Math.round(living.getHealth() * 10) / 10.0,
                    Math.round(living.getMaxHealth() * 10) / 10.0), living.getHealth() / living.getMaxHealth(), HEALTH_RED);
        }
    }
}
