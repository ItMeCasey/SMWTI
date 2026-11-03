package com.itmecasey.smwti.client;

import com.itmecasey.smwti.Smwti;
import com.itmecasey.smwti.api.Line;
import com.itmecasey.smwti.net.LookChannel;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

/**
 * Follows what the player is looking at and keeps the answers the server sent about it. It reads
 * the game's own crosshair target once a tick; while the target is a block with a block entity it
 * asks the server for that block's lines every {@link #ASK_INTERVAL} ticks, so a machine's progress
 * stays current without a request for every frame.
 */
@Mod.EventBusSubscriber(modid = Smwti.MODID, value = Dist.CLIENT)
public final class LookTracker {
    /** Ticks between two questions to the server about the same block. */
    private static final int ASK_INTERVAL = 10;

    private static @Nullable BlockPos blockPos;
    private static @Nullable Entity entity;
    /** The block the cached lines belong to, so lines of one block never show on another. */
    private static @Nullable BlockPos linesFor;
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

    /** What the server last said about the targeted block; empty until it answers. */
    public static List<Line> serverLines() {
        return blockPos != null && blockPos.equals(linesFor) ? serverLines : List.of();
    }

    /** Takes an answer from the server. */
    public static void received(BlockPos pos, List<Line> lines) {
        linesFor = pos;
        serverLines = lines;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        Minecraft game = Minecraft.getInstance();
        HitResult hit = game.hitResult;
        blockPos = null;
        entity = null;
        if (game.level == null || hit == null) {
            return;
        }
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            blockPos = blockHit.getBlockPos();
        } else if (hit instanceof EntityHitResult entityHit) {
            entity = entityHit.getEntity();
        }
        if (blockPos == null) {
            sinceAsked = ASK_INTERVAL;
            return;
        }
        BlockEntity blockEntity = game.level.getBlockEntity(blockPos);
        if (blockEntity != null && ++sinceAsked >= ASK_INTERVAL) {
            sinceAsked = 0;
            LookChannel.ask(blockPos);
        }
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
