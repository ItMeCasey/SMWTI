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
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * The server-side lines SMWTI works out for any block entity that exposes the standard Forge
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
        IEnergyStorage energy = capability(level, pos, Capabilities.EnergyStorage.BLOCK);
        if (energy != null) {
            energy(tooltip, energy);
        }
        IFluidHandler fluids = capability(level, pos, Capabilities.FluidHandler.BLOCK);
        if (fluids != null) {
            fluids(tooltip, fluids);
        }
        if (SmwtiServerConfig.showInventories()) {
            IItemHandler items = capability(level, pos, Capabilities.ItemHandler.BLOCK);
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

    private static void energy(Tooltip tooltip, IEnergyStorage energy) {
        if (energy.getMaxEnergyStored() > 0) {
            tooltip.bar(Component.translatable("smwti.energy", energy.getEnergyStored(), energy.getMaxEnergyStored()),
                    (double) energy.getEnergyStored() / energy.getMaxEnergyStored(), ENERGY_RED);
        }
    }

    private static void fluids(Tooltip tooltip, IFluidHandler fluids) {
        for (int tank = 0; tank < Math.min(fluids.getTanks(), TANKS_SHOWN); tank++) {
            FluidStack stored = fluids.getFluidInTank(tank);
            int capacity = fluids.getTankCapacity(tank);
            if (capacity <= 0) {
                continue;
            }
            if (stored.isEmpty()) {
                tooltip.bar(Component.translatable("smwti.fluid", Component.translatable("smwti.fluid.empty"), 0, capacity),
                        0.0, FLUID_BLUE);
            } else {
                // The overlay fills this bar with the fluid's own texture and tint.
                tooltip.fluidBar(Position.BODY, Component.translatable("smwti.fluid", stored.getDisplayName(), stored.getAmount(), capacity),
                        (double) stored.getAmount() / capacity, stored.getFluid());
            }
        }
    }

    /** Lists the stacks, merged by item, the biggest piles first, as many as the form allows. */
    private static void items(Tooltip tooltip, IItemHandler items, boolean detailed) {
        List<ItemStack> merged = new ArrayList<>();
        for (int slot = 0; slot < items.getSlots(); slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            boolean found = false;
            for (ItemStack seen : merged) {
                if (ItemStack.isSameItemSameComponents(seen, stack)) {
                    seen.grow(stack.getCount());
                    found = true;
                    break;
                }
            }
            if (!found) {
                merged.add(stack.copy());
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
