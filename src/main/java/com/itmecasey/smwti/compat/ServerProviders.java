package com.itmecasey.smwti.compat;

import com.itmecasey.smwti.api.Position;
import com.itmecasey.smwti.api.SmwtiApi;
import com.itmecasey.smwti.api.Tooltip;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;

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
        entity.getCapability(ForgeCapabilities.ENERGY, null).ifPresent(energy -> energy(tooltip, energy));
        entity.getCapability(ForgeCapabilities.FLUID_HANDLER, null).ifPresent(fluids -> fluids(tooltip, fluids));
        entity.getCapability(ForgeCapabilities.ITEM_HANDLER, null).ifPresent(items -> items(tooltip, items, detailed));
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
            Component name = stored.isEmpty() ? Component.translatable("smwti.fluid.empty") : stored.getDisplayName();
            tooltip.bar(Component.translatable("smwti.fluid", name, stored.getAmount(), capacity),
                    (double) stored.getAmount() / capacity, FLUID_BLUE);
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
