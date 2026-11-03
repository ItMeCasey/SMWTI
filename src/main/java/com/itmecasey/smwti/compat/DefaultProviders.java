package com.itmecasey.smwti.compat;

import com.itmecasey.smwti.api.BlockView;
import com.itmecasey.smwti.api.EntityView;
import com.itmecasey.smwti.api.Position;
import com.itmecasey.smwti.api.SmwtiApi;
import com.itmecasey.smwti.api.Tooltip;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What SMWTI itself says about vanilla things, through the same API other mods use: how far a crop
 * has grown, the health of a living thing and which tool breaks a block. The name, icon and mod are
 * drawn by the overlay for every target and need no provider. What the server knows (inventories,
 * fluids, energy) is in {@link ServerProviders}.
 */
public final class DefaultProviders {
    private static final int HEALTH_RED = 0xE04848;
    private static final int GROWTH_GREEN = 0x5DBB63;

    private DefaultProviders() {
    }

    /** Registers the client-side providers; called once while the mod loads, on the client only. */
    public static void registerClient() {
        SmwtiApi.registerBlock(DefaultProviders::crop);
        SmwtiApi.registerBlock(DefaultProviders::tool);
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

    /** A tool kind: the tag of blocks it mines, an item to draw for it and its name in the lang file. */
    private record ToolKind(TagKey<Block> mines, Item icon, String id) {
    }

    private static final List<ToolKind> TOOLS = List.of(
            new ToolKind(BlockTags.MINEABLE_WITH_PICKAXE, Items.IRON_PICKAXE, "pickaxe"),
            new ToolKind(BlockTags.MINEABLE_WITH_AXE, Items.IRON_AXE, "axe"),
            new ToolKind(BlockTags.MINEABLE_WITH_SHOVEL, Items.IRON_SHOVEL, "shovel"),
            new ToolKind(BlockTags.MINEABLE_WITH_HOE, Items.IRON_HOE, "hoe"));

    /** The tool that breaks the block, green when the player already holds the right one. */
    private static void tool(Tooltip tooltip, BlockView view) {
        BlockState state = view.state();
        Player player = Minecraft.getInstance().player;
        if (player == null || state.getDestroySpeed(view.level(), view.pos()) < 0) {
            return;
        }
        for (ToolKind kind : TOOLS) {
            if (state.is(kind.mines())) {
                // Just the tool's picture, beside the name.
                tooltip.stack(Position.BADGE, new ItemStack(kind.icon()));
                return;
            }
        }
    }

    /** Health of a living entity. */
    private static void health(Tooltip tooltip, EntityView view) {
        if (view.entity() instanceof LivingEntity living && living.getMaxHealth() > 0) {
            tooltip.bar(Component.translatable("smwti.entity.health", Math.round(living.getHealth() * 10) / 10.0,
                    Math.round(living.getMaxHealth() * 10) / 10.0), living.getHealth() / living.getMaxHealth(), HEALTH_RED);
        }
    }
}
