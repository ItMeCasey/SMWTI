package com.itmecasey.smwti.compat;

import com.itmecasey.smwti.api.BlockView;
import com.itmecasey.smwti.api.EntityView;
import com.itmecasey.smwti.api.Position;
import com.itmecasey.smwti.api.SmwtiApi;
import com.itmecasey.smwti.api.TargetOverride;
import com.itmecasey.smwti.api.Tooltip;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.EnchantmentTableBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The rest of the vanilla things Jade has a provider for that {@link VanillaProviders} does not
 * cover: enchanting power around a table, locked hoppers, unstable TNT, waxed blocks and signs,
 * player heads; falling blocks, display entities, paintings, a zombie villager being
 * cured, pet armor, and the tooltip of a dropped item in the long form.
 */
public final class VanillaMoreProviders {
    /** The most power the enchanting table can use. */
    private static final int MAX_POWER = 15;
    private static final int MAX_TOOLTIP_LINES = 8;

    private VanillaMoreProviders() {
    }

    /** Registers the providers; called once on the client. */
    public static void register() {
        SmwtiApi.registerBlock(VanillaMoreProviders::enchantingPower);
        SmwtiApi.registerBlock(VanillaMoreProviders::states);
        SmwtiApi.registerBlock(VanillaMoreProviders::containers);
        SmwtiApi.registerEntity(VanillaMoreProviders::paintings);
        SmwtiApi.registerEntity(VanillaMoreProviders::zombieVillager);
        SmwtiApi.registerEntity(VanillaMoreProviders::droppedItem);
        SmwtiApi.overrideEntity(new TargetOverride<EntityView>() {
            @Override
            public Component name(EntityView view) {
                if (view.entity() instanceof FallingBlockEntity falling) {
                    return falling.getBlockState().getBlock().getName();
                }
                if (view.entity() instanceof Display.BlockDisplay display && display.blockRenderState() != null) {
                    return display.blockRenderState().blockState().getBlock().getName();
                }
                if (view.entity() instanceof Display.ItemDisplay display && display.itemRenderState() != null
                        && !display.itemRenderState().itemStack().isEmpty()) {
                    return display.itemRenderState().itemStack().getHoverName();
                }
                return null;
            }

            @Override
            public ItemStack icon(EntityView view) {
                if (view.entity() instanceof FallingBlockEntity falling) {
                    return new ItemStack(falling.getBlockState().getBlock());
                }
                if (view.entity() instanceof Display.BlockDisplay display && display.blockRenderState() != null) {
                    return new ItemStack(display.blockRenderState().blockState().getBlock());
                }
                if (view.entity() instanceof Display.ItemDisplay display && display.itemRenderState() != null
                        && !display.itemRenderState().itemStack().isEmpty()) {
                    return display.itemRenderState().itemStack();
                }
                return null;
            }
        });
    }

    /** An enchanting table's power from the bookshelves around it, and that a bookshelf gives some. */
    private static void enchantingPower(Tooltip tooltip, BlockView view) {
        BlockState state = view.state();
        if (state.getBlock() instanceof EnchantmentTableBlock) {
            int power = 0;
            for (BlockPos offset : EnchantmentTableBlock.BOOKSHELF_OFFSETS) {
                if (EnchantmentTableBlock.isValidBookShelf(view.level(), view.pos(), offset)) {
                    power++;
                }
            }
            tooltip.bar(Component.translatable("smwti.enchant.power", power, MAX_POWER), Math.min(1.0, power / (double) MAX_POWER), 0x7B5CFF);
        } else if (state.is(BlockTags.ENCHANTMENT_POWER_PROVIDER)) {
            tooltip.add(Component.translatable("smwti.enchant.provides"));
        }
    }

    /** A locked hopper, unstable TNT, and anything waxed. */
    private static void states(Tooltip tooltip, BlockView view) {
        BlockState state = view.state();
        if (state.getBlock() instanceof HopperBlock && state.hasProperty(HopperBlock.ENABLED) && !state.getValue(HopperBlock.ENABLED)) {
            tooltip.add(Component.translatable("smwti.hopper.locked").withStyle(ChatFormatting.RED));
        }
        if (state.getBlock() instanceof TntBlock && state.hasProperty(TntBlock.UNSTABLE) && state.getValue(TntBlock.UNSTABLE)) {
            tooltip.add(Component.translatable("smwti.tnt.unstable").withStyle(ChatFormatting.RED));
        }
        boolean waxed = HoneycombItem.WAX_OFF_BY_BLOCK.get().containsKey(state.getBlock())
                || (view.entity() instanceof SignBlockEntity sign && sign.isWaxed());
        if (waxed) {
            tooltip.add(Component.translatable("smwti.waxed").withStyle(ChatFormatting.GOLD));
        }
    }

    /** Whose head a skull is. Shelves do not exist in this game version, so they have no provider. */
    private static void containers(Tooltip tooltip, BlockView view) {
        if (view.entity() instanceof SkullBlockEntity skull && skull.getOwnerProfile() != null) {
            String owner = skull.getOwnerProfile().getName();
            tooltip.add(Component.translatable("smwti.head.of", owner == null ? "?" : owner));
        }
    }

    /** A painting's title and artist. */
    private static void paintings(Tooltip tooltip, EntityView view) {
        if (view.entity() instanceof Painting painting) {
            // This version's variants carry no text; the title and artist live in the lang files under the variant's id.
            painting.getVariant().unwrapKey().ifPresent(key -> {
                String base = "painting." + key.location().getNamespace() + "." + key.location().getPath();
                tooltip.add(Component.translatable(base + ".title").withStyle(ChatFormatting.YELLOW));
                tooltip.add(Component.translatable(base + ".author").withStyle(ChatFormatting.GRAY));
            });
        }
    }

    /** That a zombie villager is being cured. */
    private static void zombieVillager(Tooltip tooltip, EntityView view) {
        if (view.entity() instanceof ZombieVillager zombie && zombie.isConverting()) {
            tooltip.add(Component.translatable("smwti.curing").withStyle(ChatFormatting.GREEN));
        }
    }

    /** In the long form: the lines of a dropped item's own tooltip (its lore, effects and so on), without its name. */
    private static void droppedItem(Tooltip tooltip, EntityView view) {
        if (!view.detailed() || !(view.entity() instanceof ItemEntity dropped)) {
            return;
        }
        Minecraft game = Minecraft.getInstance();
        if (game.player == null || game.level == null) {
            return;
        }
        List<Component> lines = dropped.getItem().getTooltipLines(game.player, TooltipFlag.NORMAL);
        for (int i = 1; i < Math.min(lines.size(), MAX_TOOLTIP_LINES + 1); i++) {
            tooltip.add(Position.TAIL, lines.get(i));
        }
    }
}
