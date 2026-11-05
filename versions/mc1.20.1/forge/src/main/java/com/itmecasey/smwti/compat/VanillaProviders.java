package com.itmecasey.smwti.compat;

import com.itmecasey.smwti.api.BlockView;
import com.itmecasey.smwti.api.EntityView;
import com.itmecasey.smwti.api.Position;
import com.itmecasey.smwti.api.SmwtiApi;
import com.itmecasey.smwti.api.TargetOverride;
import com.itmecasey.smwti.api.Tooltip;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * What SMWTI says about vanilla blocks and entities that the client can work out for itself:
 * growth of crops and other plants, redstone power, a note block's note, how full a composter, a
 * cauldron or a beehive is, a jukebox's disc, a flower pot's flower; and for entities armor, active
 * effects, riders, a horse's speed and jump, and what a dropped item or an item frame holds. The
 * long form (sneaking, by default) adds the registry id and the block state. What only the server
 * knows is in {@link VanillaServerProviders}.
 */
public final class VanillaProviders {
    private static final int GROWTH_GREEN = 0x5DBB63;
    private static final int COMPOST_BROWN = 0x8C6A3C;
    private static final int WATER_BLUE = 0x3F76E4;
    private static final int HONEY_GOLD = 0xE8B23A;
    private static final int MAX_EFFECTS = 5;
    private static final int MAX_PROPERTIES = 8;

    private VanillaProviders() {
    }

    /** Registers the providers; called once on the client. */
    public static void register() {
        SmwtiApi.registerBlock(VanillaProviders::growth);
        SmwtiApi.registerBlock(VanillaProviders::levels);
        SmwtiApi.registerBlock(VanillaProviders::redstone);
        SmwtiApi.registerBlock(VanillaProviders::contents);
        SmwtiApi.registerBlock(VanillaProviders::blockDetails);
        SmwtiApi.registerBlock(VanillaProviders::harvest);
        SmwtiApi.registerBlock(VanillaProviders::texts);
        SmwtiApi.registerEntity(VanillaProviders::armor);
        SmwtiApi.registerEntity(VanillaProviders::effects);
        SmwtiApi.registerEntity(VanillaProviders::riders);
        SmwtiApi.registerEntity(VanillaProviders::horse);
        SmwtiApi.registerEntity(VanillaProviders::heldItems);
        SmwtiApi.registerEntity((tooltip, view) -> {
            if (view.detailed() && view.entity() instanceof ItemEntity dropped) {
                enchantments(tooltip, dropped.getItem());
            } else if (view.detailed() && view.entity() instanceof ItemFrame frame) {
                enchantments(tooltip, frame.getItem());
            }
        });
        SmwtiApi.registerEntity(VanillaProviders::villager);
        SmwtiApi.registerEntity(VanillaProviders::entityDetails);
        SmwtiApi.registerEntity(VanillaProviders::equipment);
        SmwtiApi.overrideEntity(new TargetOverride<EntityView>() {
            @Override
            public Component name(EntityView view) {
                return view.entity() instanceof ItemEntity item ? item.getItem().getHoverName() : null;
            }

            @Override
            public ItemStack icon(EntityView view) {
                return view.entity() instanceof ItemEntity item ? item.getItem() : null;
            }
        });
    }

    // --- Blocks -----------------------------------------------------------------------------

    /** How far a plant has grown, for crops and the other plants that grow in stages. */
    private static void growth(Tooltip tooltip, BlockView view) {
        BlockState state = view.state();
        // Plain crops are handled by DefaultProviders; these are the others that grow in stages.
        boolean staged = state.getBlock() instanceof NetherWartBlock || state.getBlock() instanceof CocoaBlock
                || state.getBlock() instanceof SweetBerryBushBlock
                || (state.is(BlockTags.CROPS) && !(state.getBlock() instanceof CropBlock));
        if (!staged) {
            return;
        }
        for (Property<?> property : state.getProperties()) {
            if (property instanceof IntegerProperty age && age.getName().equals("age")) {
                int value = state.getValue(age);
                int max = age.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElse(value);
                tooltip.bar(value >= max ? Component.translatable("smwti.crop.mature")
                                : Component.translatable("smwti.crop.growth", Math.round(100.0 * value / max)),
                        max == 0 ? 1.0 : (double) value / max, GROWTH_GREEN);
                return;
            }
        }
    }

    /** How full a composter, a cauldron, a beehive or a respawn anchor is. */
    private static void levels(Tooltip tooltip, BlockView view) {
        BlockState state = view.state();
        if (state.hasProperty(ComposterBlock.LEVEL)) {
            int level = state.getValue(ComposterBlock.LEVEL);
            tooltip.bar(Component.translatable("smwti.composter", level, ComposterBlock.MAX_LEVEL), (double) level / ComposterBlock.MAX_LEVEL,
                    COMPOST_BROWN);
        } else if (state.hasProperty(BlockStateProperties.LEVEL_CAULDRON)) {
            int level = state.getValue(BlockStateProperties.LEVEL_CAULDRON);
            tooltip.bar(Component.translatable("smwti.cauldron", level, 3), level / 3.0, WATER_BLUE);
        } else if (state.hasProperty(BlockStateProperties.LEVEL_HONEY)) {
            int level = state.getValue(BlockStateProperties.LEVEL_HONEY);
            tooltip.bar(Component.translatable("smwti.honey", level, 5), level / 5.0, HONEY_GOLD);
        } else if (state.hasProperty(BlockStateProperties.RESPAWN_ANCHOR_CHARGES)) {
            int charges = state.getValue(BlockStateProperties.RESPAWN_ANCHOR_CHARGES);
            tooltip.bar(Component.translatable("smwti.anchor", charges, 4), charges / 4.0, 0xB066FF);
        }
    }

    /** Redstone power and a note block's note. */
    private static void redstone(Tooltip tooltip, BlockView view) {
        BlockState state = view.state();
        if (state.hasProperty(BlockStateProperties.POWER)) {
            tooltip.add(Component.translatable("smwti.power", state.getValue(BlockStateProperties.POWER)));
        }
        if (state.hasProperty(BlockStateProperties.NOTE)) {
            tooltip.add(Component.translatable("smwti.note", state.getValue(BlockStateProperties.NOTE)));
        }
    }

    /** A jukebox's disc and a flower pot's plant. */
    private static void contents(Tooltip tooltip, BlockView view) {
        if (view.entity() instanceof JukeboxBlockEntity jukebox && !jukebox.getItem(0).isEmpty()) {
            tooltip.item(jukebox.getItem(0), jukebox.getItem(0).getHoverName());
        }
        if (view.state().getBlock() instanceof FlowerPotBlock pot && pot.getContent() != net.minecraft.world.level.block.Blocks.AIR) {
            ItemStack plant = new ItemStack(pot.getContent());
            tooltip.item(plant, plant.getHoverName());
        }
    }

    /** In the long form: the registry id and the block state's properties. */
    private static void blockDetails(Tooltip tooltip, BlockView view) {
        if (!view.detailed()) {
            return;
        }
        tooltip.add(Position.TAIL, Component.literal(BuiltInRegistries.BLOCK.getKey(view.state().getBlock()).toString())
                .withStyle(ChatFormatting.GRAY));
        int shown = 0;
        for (Property<?> property : view.state().getProperties()) {
            if (shown++ == MAX_PROPERTIES) {
                break;
            }
            tooltip.add(Position.TAIL, Component.literal(property.getName() + ": " + view.state().getValue(property))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    /** In the long form: the tool tier the block needs, how hard it is and how well it stands up to explosions. */
    private static void harvest(Tooltip tooltip, BlockView view) {
        if (!view.detailed()) {
            return;
        }
        BlockState state = view.state();
        float hardness = state.getDestroySpeed(view.level(), view.pos());
        if (hardness < 0) {
            tooltip.add(Position.TAIL, Component.translatable("smwti.unbreakable"));
            return;
        }
        String tier = state.is(BlockTags.NEEDS_DIAMOND_TOOL) ? "diamond" : state.is(BlockTags.NEEDS_IRON_TOOL) ? "iron"
                : state.is(BlockTags.NEEDS_STONE_TOOL) ? "stone" : null;
        if (tier != null) {
            tooltip.add(Position.TAIL, Component.translatable("smwti.tier." + tier));
        }
        tooltip.add(Position.TAIL, Component.translatable("smwti.hardness", String.format(Locale.ROOT, "%.1f", hardness),
                String.format(Locale.ROOT, "%.1f", state.getBlock().getExplosionResistance())));
    }

    /** What is written on a sign, what book a lectern holds and what is cooking on a campfire. */
    private static void texts(Tooltip tooltip, BlockView view) {
        if (view.entity() instanceof SignBlockEntity sign) {
            for (Component line : sign.getFrontText().getMessages(false)) {
                if (!line.getString().isBlank()) {
                    tooltip.add(line);
                }
            }
        } else if (view.entity() instanceof LecternBlockEntity lectern && !lectern.getBook().isEmpty()) {
            tooltip.item(lectern.getBook(), lectern.getBook().getHoverName());
        } else if (view.entity() instanceof CampfireBlockEntity campfire) {
            campfire.getItems().stream().filter(stack -> !stack.isEmpty()).forEach(stack -> tooltip.stack(Position.BODY, stack));
        }
    }

    // --- Entities ---------------------------------------------------------------------------

    /** Armor points, shown with a chestplate. */
    private static void armor(Tooltip tooltip, EntityView view) {
        if (view.entity() instanceof LivingEntity living && living.getArmorValue() > 0) {
            tooltip.item(new ItemStack(Items.IRON_CHESTPLATE), Component.translatable("smwti.armor", living.getArmorValue()));
        }
    }

    /** Active potion effects, colored by whether they help or hurt, with their time left. */
    private static void effects(Tooltip tooltip, EntityView view) {
        if (!(view.entity() instanceof LivingEntity living)) {
            return;
        }
        Collection<MobEffectInstance> active = living.getActiveEffects();
        int shown = 0;
        for (MobEffectInstance effect : active) {
            if (shown++ == MAX_EFFECTS) {
                tooltip.add(Component.translatable("smwti.items.more", active.size() - MAX_EFFECTS));
                break;
            }
            ChatFormatting color = switch (effect.getEffect().getCategory()) {
                case BENEFICIAL -> ChatFormatting.GREEN;
                case HARMFUL -> ChatFormatting.RED;
                case NEUTRAL -> ChatFormatting.GRAY;
            };
            Component name = effect.getEffect().getDisplayName().copy();
            if (effect.getAmplifier() > 0) {
                name = name.copy().append(" " + (effect.getAmplifier() + 1));
            }
            String time = effect.isInfiniteDuration() ? "∞" : duration(effect.getDuration());
            tooltip.add(Component.empty().append(name).append("  " + time).withStyle(color));
        }
    }

    private static String duration(int ticks) {
        int seconds = ticks / 20;
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }

    /** What the entity rides and who rides it. */
    private static void riders(Tooltip tooltip, EntityView view) {
        Entity entity = view.entity();
        if (entity.getVehicle() != null) {
            tooltip.add(Component.translatable("smwti.riding", entity.getVehicle().getName()));
        }
        List<Entity> passengers = entity.getPassengers();
        for (Entity passenger : passengers) {
            tooltip.add(Component.translatable("smwti.rider", passenger.getName()));
        }
    }

    /** A horse's speed and jump height, in blocks per second and blocks. */
    private static void horse(Tooltip tooltip, EntityView view) {
        if (view.entity() instanceof AbstractHorse horse) {
            double speed = horse.getAttributeValue(Attributes.MOVEMENT_SPEED) * 43.17;
            double jump = horse.getAttributeValue(Attributes.JUMP_STRENGTH);
            // The height a jump of that strength reaches, from the game's own physics.
            double height = -0.1817584952 * jump * jump * jump + 3.689713992 * jump * jump + 2.128599134 * jump - 0.343930367;
            tooltip.add(Component.translatable("smwti.horse.speed", String.format(Locale.ROOT, "%.1f", speed)));
            tooltip.add(Component.translatable("smwti.horse.jump", String.format(Locale.ROOT, "%.1f", height)));
        }
    }

    /** The enchantments on a stack, one line each, in the long form. */
    private static void enchantments(Tooltip tooltip, ItemStack stack) {
        for (Map.Entry<Enchantment, Integer> entry : EnchantmentHelper.getEnchantments(stack).entrySet()) {
            tooltip.add(Position.TAIL, entry.getKey().getFullname(entry.getValue()));
        }
    }

    /** A villager's profession and level, such as "Farmer, Journeyman". */
    private static void villager(Tooltip tooltip, EntityView view) {
        if (view.entity() instanceof Villager villager) {
            VillagerData data = villager.getVillagerData();
            ResourceLocation profession = BuiltInRegistries.VILLAGER_PROFESSION.getKey(data.getProfession());
            Component job = Component.translatable("entity.minecraft.villager." + profession.getPath());
            if (villager.isBaby()) {
                return;
            }
            tooltip.add(Component.empty().append(job).append(", ").append(Component.translatable("merchant.level." + data.getLevel())));
        }
    }

    /** What an item frame holds. */
    private static void heldItems(Tooltip tooltip, EntityView view) {
        if (view.entity() instanceof ItemFrame frame && !frame.getItem().isEmpty()) {
            tooltip.item(frame.getItem(), frame.getItem().getHoverName());
        }
    }

    /** The items a mob or an armor stand holds and wears, as a row of icons. */
    private static void equipment(Tooltip tooltip, EntityView view) {
        if (!(view.entity() instanceof LivingEntity living) || living instanceof net.minecraft.world.entity.player.Player) {
            return;
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = living.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                tooltip.stack(Position.BODY, stack.copyWithCount(1));
            }
        }
    }

    /** In the long form: the entity type's registry id. */
    private static void entityDetails(Tooltip tooltip, EntityView view) {
        if (view.detailed()) {
            tooltip.add(Position.TAIL, Component.literal(BuiltInRegistries.ENTITY_TYPE.getKey(view.entity().getType()).toString())
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
