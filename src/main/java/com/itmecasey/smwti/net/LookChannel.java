package com.itmecasey.smwti.net;

import com.itmecasey.smwti.Smwti;
import com.itmecasey.smwti.api.Line;
import com.itmecasey.smwti.api.ServerBlockProvider;
import com.itmecasey.smwti.api.SmwtiApi;
import com.itmecasey.smwti.api.Tooltip;
import com.itmecasey.smwti.client.LookTracker;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

/**
 * The two messages of the overlay. The client asks about the block it is looking at; the server
 * runs the registered {@link ServerBlockProvider}s and answers with the lines. The channel is
 * optional, so either side may be without SMWTI: a client without it never asks, and a server
 * without it never answers, which the overlay shows as no extra lines.
 */
public final class LookChannel {
    /** Farthest a player may be from a block and still ask about it, in blocks. */
    private static final double REACH = 12.0;

    private static SimpleChannel channel;

    private LookChannel() {
    }

    /** Client to server: what do you know about the block at this position? */
    private record Request(BlockPos pos) {
    }

    /** Server to client: the lines for the block at this position. */
    private record Reply(BlockPos pos, List<Line> lines) {
    }

    /** Builds the channel. Forge wants channels made while mods load, so the mod constructor calls this. */
    public static void register() {
        channel = ChannelBuilder.named(Smwti.id("look"))
                .networkProtocolVersion(1)
                .optional()
                .simpleChannel()
                .messageBuilder(Request.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder((request, buf) -> buf.writeBlockPos(request.pos()))
                .decoder(buf -> new Request(buf.readBlockPos()))
                .consumerMainThread((request, context) -> {
                    answer(context.getSender(), request.pos());
                    context.setPacketHandled(true);
                })
                .add()
                .messageBuilder(Reply.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((reply, buf) -> {
                    buf.writeBlockPos(reply.pos());
                    CompoundTag wrapper = new CompoundTag();
                    Line.CODEC.listOf().encodeStart(NbtOps.INSTANCE, reply.lines()).result()
                            .ifPresent(tag -> wrapper.put("lines", tag));
                    buf.writeNbt(wrapper);
                })
                .decoder(buf -> {
                    BlockPos pos = buf.readBlockPos();
                    CompoundTag wrapper = buf.readNbt();
                    Tag tag = wrapper == null ? null : wrapper.get("lines");
                    List<Line> lines = tag == null ? List.of()
                            : Line.CODEC.listOf().parse(NbtOps.INSTANCE, tag).result().orElse(List.of());
                    return new Reply(pos, lines);
                })
                .consumerMainThread((reply, context) -> {
                    LookTracker.received(reply.pos(), reply.lines());
                    context.setPacketHandled(true);
                })
                .add();
    }

    /** Asks the server about a block; called by the client while the player keeps looking at it. */
    public static void ask(BlockPos pos) {
        if (channel != null) {
            channel.send(new Request(pos), PacketDistributor.SERVER.noArg());
        }
    }

    /** Runs the server providers for the block and sends the player what they said. */
    private static void answer(ServerPlayer player, BlockPos pos) {
        if (player == null || SmwtiApi.serverBlockProviders().isEmpty()) {
            return;
        }
        // Only blocks near the player that are actually loaded: a request is just numbers from the network.
        ServerLevel level = player.level();
        if (!level.isLoaded(pos) || player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > REACH * REACH) {
            return;
        }
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity == null) {
            return;
        }
        Tooltip tooltip = new Tooltip();
        for (ServerBlockProvider provider : SmwtiApi.serverBlockProviders()) {
            try {
                provider.collect(tooltip, level, pos, entity);
            } catch (RuntimeException e) {
                // One mod's broken provider must not take the others, or the player, down with it.
                Smwti.LOGGER.warn("A server provider failed for {} at {}", entity.getType(), pos, e);
            }
        }
        channel.send(new Reply(pos, tooltip.lines()), PacketDistributor.PLAYER.with(player));
    }
}
