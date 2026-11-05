package com.itmecasey.smwti.client;

import com.itmecasey.smwti.Smwti;
import com.itmecasey.smwti.SmwtiConfig;
import com.itmecasey.smwti.api.BlockRedirect;
import com.itmecasey.smwti.api.Line;
import com.itmecasey.smwti.api.SmwtiApi;
import com.itmecasey.smwti.net.LookChannel;
import com.itmecasey.smwti.net.LookTarget;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

/**
 * Follows what the player is looking at and keeps the answers the server sent about it. It reads
 * the game's own crosshair target once a tick, looks again for a fluid when the player has them
 * switched on (the game's crosshair looks through water), and while the target is a block with a
 * block entity or any entity it asks the server for its lines every {@link #ASK_INTERVAL} ticks,
 * so a machine's progress stays current without a request for every frame.
 */
@Mod.EventBusSubscriber(modid = Smwti.MODID, value = Dist.CLIENT)
public final class LookTracker {
    /** Ticks between two questions to the server about the same target. */
    private static final int ASK_INTERVAL = 10;
    /** Blocks added around a dropped item's box when testing the look ray: the box is tiny next to the drawn item. */
    private static final double ITEM_PICK_MARGIN = 0.3;

    private static @Nullable BlockPos blockPos;
    private static @Nullable Entity entity;
    /** Whether the long form is asked for, as of the last tick. */
    private static boolean detailed;
    /** The target the cached lines belong to, so lines of one target never show on another. */
    private static @Nullable LookTarget linesFor;
    private static List<Line> serverLines = List.of();
    private static int sinceAsked = ASK_INTERVAL;

    private LookTracker() {
    }

    /** The block being looked at, or {@code null} when the target is not a block. */
    public static @Nullable BlockPos block() {
        return blockPos;
    }

    /** The entity being looked at, or {@code null} when the target is not an entity. */
    public static @Nullable Entity entity() {
        return entity;
    }

    /** Whether providers should show their long form right now. */
    public static boolean detailed() {
        return detailed;
    }

    /** What the server last said about the current target; empty until it answers. */
    public static List<Line> serverLines() {
        LookTarget current = current();
        return current != null && current.equals(linesFor) ? serverLines : List.of();
    }

    /** Takes an answer from the server. */
    public static void received(LookTarget target, List<Line> lines) {
        linesFor = target;
        serverLines = lines;
    }

    private static @Nullable LookTarget current() {
        if (entity != null) {
            return LookTarget.entity(entity.getId());
        }
        return blockPos != null ? LookTarget.block(blockPos) : null;
    }

    /** The dropped item the crosshair is on. The game never picks them, so this tests their boxes along the look ray. */
    @Nullable
    private static Entity droppedItemOnLook(Minecraft game, HitResult hit) {
        Vec3 eye = game.player.getEyePosition();
        double reach = game.gameMode.getPickRange();
        Vec3 end = eye.add(game.player.getLookAngle().scale(reach));
        double nearestSqr = hit.getType() == HitResult.Type.BLOCK ? hit.getLocation().distanceToSqr(eye) : Double.MAX_VALUE;
        ItemEntity nearest = null;
        for (ItemEntity item : game.level.getEntitiesOfClass(ItemEntity.class, game.player.getBoundingBox().inflate(reach))) {
            Optional<Vec3> at = item.getBoundingBox().inflate(ITEM_PICK_MARGIN).clip(eye, end);
            if (at.isPresent() && at.get().distanceToSqr(eye) < nearestSqr) {
                nearest = item;
                nearestSqr = at.get().distanceToSqr(eye);
            }
        }
        return nearest;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft game = Minecraft.getInstance();
        blockPos = null;
        entity = null;
        if (game.level == null || game.player == null) {
            return;
        }
        boolean wasDetailed = detailed;
        detailed = SmwtiConfig.detailed(game.player.isShiftKeyDown());

        HitResult hit = game.hitResult;
        if (hit instanceof EntityHitResult entityHit) {
            entity = entityHit.getEntity();
        } else {
            // The game's own target skips fluids, so look again with them counted.
            if (SmwtiConfig.showFluids()) {
                HitResult withFluids = game.player.pick(game.gameMode.getPickRange(), 1.0F, true);
                if (withFluids instanceof BlockHitResult fluidHit && withFluids.getType() == HitResult.Type.BLOCK
                        && !game.level.getFluidState(fluidHit.getBlockPos()).isEmpty()) {
                    hit = withFluids;
                }
            }
            if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
                blockPos = redirected(game, blockHit.getBlockPos());
            }
        }

        if (entity == null) {
            entity = droppedItemOnLook(game, hit);
            if (entity != null) {
                // The item is in front of the block behind it, so the block is not the target.
                blockPos = null;
            }
        }
        LookTarget target = current();
        boolean worthAsking = entity != null || (blockPos != null && game.level.getBlockEntity(blockPos) != null);
        if (target == null || !worthAsking) {
            sinceAsked = ASK_INTERVAL;
            return;
        }
        // A change between the short and the long form is asked about at once.
        if (wasDetailed != detailed || ++sinceAsked >= ASK_INTERVAL) {
            sinceAsked = 0;
            LookChannel.ask(target, detailed);
        }
    }

    /** Asks the registered redirects whether another block should be shown in place of this one. */
    private static BlockPos redirected(Minecraft game, BlockPos pos) {
        for (BlockRedirect redirect : SmwtiApi.redirects()) {
            try {
                BlockPos other = redirect.redirect(game.level, pos, game.level.getBlockState(pos));
                if (other != null) {
                    return other;
                }
            } catch (RuntimeException e) {
                Smwti.LOGGER.warn("A block redirect failed", e);
            }
        }
        return pos;
    }

    /** Leaving a world forgets everything about it. */
    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        blockPos = null;
        entity = null;
        linesFor = null;
        serverLines = List.of();
    }
}
