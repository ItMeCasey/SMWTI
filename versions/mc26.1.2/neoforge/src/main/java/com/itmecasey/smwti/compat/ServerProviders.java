package com.itmecasey.smwti.compat;

import com.itmecasey.smwti.SmwtiServerConfig;
import com.itmecasey.smwti.api.Position;
import com.itmecasey.smwti.api.SmwtiApi;
import com.itmecasey.smwti.api.Tooltip;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * The server-side lines SMWTI works out for any block entity that exposes the standard NeoForge
 * capabilities: what is in its inventory, what its tanks hold and how much energy it stores. A
 * block that shows these through its own provider can still do so; the overlay just lists both.
 * The long form (the player sneaking, by default) lists more items.
 */
public final class ServerProviders {
    private static final int ITEMS_SHORT = 5;
    private static final int ITEMS_LONG = 20;
    private static final int TANKS_SHOWN = 4;
    private static final int FLUID_BLUE = 0x3F76E4;
    private static final int ENERGY_RED = 0xD94A4A;

    private ServerProviders() {
    }

    /** Registers the capability providers; called once while the mod loads, on both sides. */
    public static void register() {
        SmwtiApi.registerServerBlock(ServerProviders::collect);
    }

    private static void collect(Tooltip tooltip, ServerLevel level, BlockPos pos, BlockEntity entity, boolean detailed) {
        EnergyHandler energy = capability(level, pos, Capabilities.Energy.BLOCK);
        if (energy != null) {
            energy(tooltip, energy);
        }
        ResourceHandler<FluidResource> fluids = capability(level, pos, Capabilities.Fluid.BLOCK);
        if (fluids != null) {
            fluids(tooltip, fluids);
        }
        if (SmwtiServerConfig.showInventories()) {
            ResourceHandler<ItemResource> items = capability(level, pos, Capabilities.Item.BLOCK);
            if (items != null) {
                items(tooltip, items, detailed);
            }
        }
    }

    /**
     * The block's capability, asked for on no side first and then on each side in turn. Many tanks and
     * machines only hand out their handler on the sides that connect to something, so asking on one
     * side would miss them.
     */
    private static <T> T capability(ServerLevel level, BlockPos pos, BlockCapability<T, Direction> type) {
        T found = level.getCapability(type, pos, null);
        for (int i = 0; found == null && i < Direction.values().length; i++) {
            found = level.getCapability(type, pos, Direction.values()[i]);
        }
        return found;
    }

    private static void energy(Tooltip tooltip, EnergyHandler energy) {
        int capacity = energy.getCapacityAsInt();
        if (capacity > 0) {
            int stored = energy.getAmountAsInt();
            tooltip.bar(Component.translatable("smwti.energy", stored, capacity),
                    (double) stored / capacity, ENERGY_RED);
        }
    }

    private static void fluids(Tooltip tooltip, ResourceHandler<FluidResource> fluids) {
        for (int tank = 0; tank < Math.min(fluids.size(), TANKS_SHOWN); tank++) {
            FluidResource stored = fluids.getResource(tank);
            int amount = fluids.getAmountAsInt(tank);
            int capacity = fluids.getCapacityAsInt(tank, stored);
            if (capacity <= 0) {
                continue;
            }
            if (stored.isEmpty()) {
                tooltip.bar(Component.translatable("smwti.fluid", Component.translatable("smwti.fluid.empty"), 0, capacity),
                        0.0, FLUID_BLUE);
            } else {
                // The overlay fills this bar with the fluid's own texture and tint.
                tooltip.fluidBar(Position.BODY, Component.translatable("smwti.fluid", stored.toStack(amount).getHoverName(), amount, capacity),
                        (double) amount / capacity, stored.getFluid());
            }
        }
    }

    /** Lists the stacks, merged by item, the biggest piles first, as many as the form allows. */
    private static void items(Tooltip tooltip, ResourceHandler<ItemResource> items, boolean detailed) {
        List<ItemStack> merged = new ArrayList<>();
        for (int slot = 0; slot < items.size(); slot++) {
            ItemResource resource = items.getResource(slot);
            int amount = items.getAmountAsInt(slot);
            if (resource.isEmpty() || amount <= 0) {
                continue;
            }
            ItemStack stack = resource.toStack(amount);
            boolean found = false;
            for (ItemStack seen : merged) {
                if (ItemStack.isSameItemSameComponents(seen, stack)) {
                    seen.grow(stack.getCount());
                    found = true;
                    break;
                }
            }
            if (!found) {
                merged.add(stack);
            }
        }
        merged.sort((first, second) -> Integer.compare(second.getCount(), first.getCount()));
        int limit = detailed ? ITEMS_LONG : ITEMS_SHORT;
        for (int i = 0; i < Math.min(limit, merged.size()); i++) {
            ItemStack stack = merged.get(i);
            tooltip.stack(Position.BODY, stack);
        }
        if (merged.size() > limit) {
            tooltip.add(Component.translatable("smwti.items.more", merged.size() - limit));
        }
    }
}
