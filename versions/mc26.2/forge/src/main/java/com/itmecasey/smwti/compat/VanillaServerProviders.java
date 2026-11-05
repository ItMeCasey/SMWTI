package com.itmecasey.smwti.compat;

import com.itmecasey.smwti.SmwtiServerConfig;
import com.itmecasey.smwti.api.SmwtiApi;
import com.itmecasey.smwti.api.Tooltip;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

/**
 * What SMWTI says about vanilla things only the server knows: how far a furnace or a brewing stand
 * has got, what a spawner spawns, a beacon's level, how many bees a hive holds; and for animals how
 * long until they grow up or can breed again, and who owns a pet. Block entities are read through
 * their saved data, which every version of the game keeps under names this class knows, so nothing
 * here depends on private fields.
 */
public final class VanillaServerProviders {
    private static final int FIRE_ORANGE = 0xE8963A;
    private static final int PROGRESS_GREEN = 0x5DBB63;

    private VanillaServerProviders() {
    }

    /** Registers the providers; called once while the mod loads, on both sides. */
    public static void register() {
        SmwtiApi.registerServerBlock(VanillaServerProviders::blockEntity);
        SmwtiApi.registerServerEntity(VanillaServerProviders::entity);
    }

    private static void blockEntity(Tooltip tooltip, ServerLevel level, BlockPos pos, BlockEntity entity, boolean detailed) {
        if (!(entity instanceof AbstractFurnaceBlockEntity || entity instanceof BrewingStandBlockEntity
                || entity instanceof SpawnerBlockEntity || entity instanceof BeaconBlockEntity
                || entity instanceof BeehiveBlockEntity || entity instanceof CommandBlockEntity)) {
            return;
        }
        CompoundTag data = entity.saveCustomOnly(level.registryAccess());
        switch (entity) {
            case AbstractFurnaceBlockEntity furnace -> furnace(tooltip, data);
            case BrewingStandBlockEntity stand -> brewing(tooltip, data);
            case SpawnerBlockEntity spawner -> spawner(tooltip, data);
            case BeaconBlockEntity beacon -> beacon(tooltip, data);
            case BeehiveBlockEntity hive -> hive(tooltip, data);
            case CommandBlockEntity command -> {
                if (SmwtiServerConfig.showCommands()) {
                    data.getString("Command").filter(text -> !text.isEmpty()).ifPresent(text -> tooltip.add(Component.translatable("smwti.command", text)));
                }
            }
            default -> {
            }
        }
    }

    /** An int from the saved data under whichever of the names this game version uses. */
    private static int intOf(CompoundTag data, int fallback, String... names) {
        for (String name : names) {
            var found = data.getInt(name);
            if (found.isPresent()) {
                return found.get();
            }
        }
        return fallback;
    }

    private static void number(Tooltip tooltip, String key, CompoundTag data, int fallback, String... names) {
        int value = intOf(data, fallback, names);
        if (value > 0) {
            tooltip.add(Component.translatable(key, value));
        }
    }

    /** Cooking progress as a bar, and the fuel left as seconds. */
    private static void furnace(Tooltip tooltip, CompoundTag data) {
        int spent = intOf(data, 0, "cooking_time_spent", "CookTime");
        int total = intOf(data, 0, "cooking_total_time", "CookTimeTotal");
        int fuel = intOf(data, 0, "lit_time_remaining", "BurnTime");
        if (total > 0 && spent > 0) {
            tooltip.bar(Component.translatable("smwti.furnace.progress", Math.round(100.0 * spent / total)),
                    (double) spent / total, PROGRESS_GREEN);
        }
        if (fuel > 0) {
            tooltip.bar(Component.translatable("smwti.furnace.fuel", seconds(fuel)), Math.min(1.0, fuel / 1600.0), FIRE_ORANGE);
        }
    }

    /** A brewing stand's progress; a brew takes 400 ticks. */
    private static void brewing(Tooltip tooltip, CompoundTag data) {
        int brew = intOf(data, 0, "BrewTime", "brew_time");
        int fuel = intOf(data, 0, "Fuel", "fuel");
        if (brew > 0) {
            tooltip.bar(Component.translatable("smwti.brewing.progress", seconds(brew)), 1.0 - brew / 400.0, PROGRESS_GREEN);
        }
        tooltip.add(Component.translatable("smwti.brewing.fuel", fuel));
    }

    /** The mob a spawner makes, from its spawn data. */
    private static void spawner(Tooltip tooltip, CompoundTag data) {
        data.getCompound("SpawnData").flatMap(spawn -> spawn.getCompound("entity")).flatMap(entity -> entity.getString("id"))
                .ifPresent(id -> tooltip.add(Component.translatable("smwti.spawner", id.replaceFirst("^minecraft:", ""))));
        // The ticks until the next spawn.
        data.getShort("Delay").filter(delay -> delay > 0).ifPresent(delay -> tooltip.add(Component.translatable("smwti.spawner.next", seconds(delay))));
    }

    /** A beacon's level and the effects it gives. */
    private static void beacon(Tooltip tooltip, CompoundTag data) {
        number(tooltip, "smwti.beacon", data, 0, "Levels", "levels");
        for (String key : new String[] {"primary_effect", "Primary"}) {
            data.getString(key).ifPresent(id -> tooltip.add(Component.translatable("smwti.beacon.primary", id.replaceFirst("^minecraft:", ""))));
        }
        for (String key : new String[] {"secondary_effect", "Secondary"}) {
            data.getString(key).ifPresent(id -> tooltip.add(Component.translatable("smwti.beacon.secondary", id.replaceFirst("^minecraft:", ""))));
        }
    }

    /** How many bees a hive or nest holds. */
    private static void hive(Tooltip tooltip, CompoundTag data) {
        int bees = data.getList("bees").map(list -> list.size()).orElse(0);
        tooltip.add(Component.translatable("smwti.bees", bees));
    }

    private static String seconds(int ticks) {
        int seconds = ticks / 20;
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }

    // --- Entities ---------------------------------------------------------------------------

    private static void entity(Tooltip tooltip, ServerLevel level, Entity entity, boolean detailed) {
        if (entity instanceof OwnableEntity pet && pet.getOwner() != null) {
            tooltip.add(Component.translatable("smwti.owner", pet.getOwner().getName()));
        }
        if (entity instanceof AgeableMob mob) {
            int age = mob.getAge();
            if (age < 0) {
                tooltip.add(Component.translatable("smwti.grows_up", seconds(-age)));
            } else if (age > 0) {
                tooltip.add(Component.translatable("smwti.breed_cooldown", seconds(age)));
            }
        }
        if (entity instanceof Chicken chicken && chicken.eggTime > 0) {
            tooltip.add(Component.translatable("smwti.egg.next", seconds(chicken.eggTime)));
        }
        if (entity instanceof Animal animal && animal.isInLove()) {
            tooltip.add(Component.translatable("smwti.in_love"));
        }
    }
}
