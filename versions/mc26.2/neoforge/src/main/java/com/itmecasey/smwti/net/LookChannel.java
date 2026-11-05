package com.itmecasey.smwti.net;

import com.itmecasey.smwti.Smwti;
import com.itmecasey.smwti.SmwtiServerConfig;
import com.itmecasey.smwti.api.Line;
import com.itmecasey.smwti.api.ServerBlockProvider;
import com.itmecasey.smwti.api.ServerEntityProvider;
import com.itmecasey.smwti.api.SmwtiApi;
import com.itmecasey.smwti.api.Tooltip;
import com.itmecasey.smwti.client.LookTracker;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The two messages of the overlay. The client asks about the block or entity it is looking at; the
 * server runs the registered server providers and answers with the lines. The channel is optional,
 * so either side may be without SMWTI: a client without it never asks, and a server without it
 * never answers, which the overlay shows as no extra lines.
 */
public final class LookChannel {
    /** Bump when the messages change shape. */
    private static final String VERSION = "2";

    private LookChannel() {
    }

    /** Client to server: what do you know about this target? {@code detailed} asks for the long form. */
    private record Request(LookTarget target, boolean detailed) implements CustomPacketPayload {
        static final CustomPacketPayload.Type<Request> TYPE = new CustomPacketPayload.Type<>(Smwti.id("look_request"));
        static final StreamCodec<FriendlyByteBuf, Request> CODEC = StreamCodec.of(
                (buf, request) -> {
                    request.target().write(buf);
                    buf.writeBoolean(request.detailed());
                },
                buf -> new Request(LookTarget.read(buf), buf.readBoolean()));

        @Override
        public CustomPacketPayload.Type<Request> type() {
            return TYPE;
        }
    }

    /** Server to client: the lines for this target. */
    private record Reply(LookTarget target, List<Line> lines) implements CustomPacketPayload {
        static final CustomPacketPayload.Type<Reply> TYPE = new CustomPacketPayload.Type<>(Smwti.id("look_reply"));
        static final StreamCodec<FriendlyByteBuf, Reply> CODEC = StreamCodec.of(
                (buf, reply) -> {
                    reply.target().write(buf);
                    CompoundTag wrapper = new CompoundTag();
                    Line.CODEC.listOf().encodeStart(NbtOps.INSTANCE, reply.lines()).result()
                            .ifPresent(tag -> wrapper.put("lines", tag));
                    buf.writeNbt(wrapper);
                },
                buf -> {
                    LookTarget target = LookTarget.read(buf);
                    CompoundTag wrapper = buf.readNbt();
                    Tag tag = wrapper == null ? null : wrapper.get("lines");
                    List<Line> lines = tag == null ? List.of()
                            : Line.CODEC.listOf().parse(NbtOps.INSTANCE, tag).result().orElse(List.of());
                    return new Reply(target, lines);
                });

        @Override
        public CustomPacketPayload.Type<Reply> type() {
            return TYPE;
        }
    }

    /**
     * Registers the messages. NeoForge wants payloads registered while mods load, so the mod
     * constructor hands this to the mod bus.
     */
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(VERSION).optional()
                .playToServer(Request.TYPE, Request.CODEC, LookChannel::onRequest)
                .playToClient(Reply.TYPE, Reply.CODEC, LookChannel::onReply);
    }

    /** Asks the server about a target; called by the client while the player keeps looking at it. */
    public static void ask(LookTarget target, boolean detailed) {
        ClientPacketDistributor.sendToServer(new Request(target, detailed));
    }

    private static void onRequest(Request request, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            answer(player, request.target(), request.detailed());
        }
    }

    private static void onReply(Reply reply, IPayloadContext context) {
        LookTracker.received(reply.target(), reply.lines());
    }

    /** Runs the server providers for the target and sends the player what they said. */
    private static void answer(ServerPlayer player, LookTarget target, boolean detailed) {
        if (!SmwtiServerConfig.answer()) {
            return;
        }
        double reach = SmwtiServerConfig.reach();
        ServerLevel level = player.level();
        Tooltip tooltip = new Tooltip();
        if (target.entity()) {
            Entity entity = level.getEntity(target.entityId());
            if (entity == null || player.distanceToSqr(entity) > reach * reach
                    || SmwtiServerConfig.blacklisted(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString())) {
                return;
            }
            for (ServerEntityProvider provider : SmwtiApi.serverEntityProviders()) {
                try {
                    provider.collect(tooltip, level, entity, detailed);
                } catch (RuntimeException e) {
                    // One mod's broken provider must not take the others, or the player, down with it.
                    Smwti.LOGGER.warn("A server entity provider failed for {}", entity.getType(), e);
                }
            }
        } else {
            // Only blocks near the player that are actually loaded: a request is just numbers from the network.
            BlockPos pos = target.pos();
            if (!level.isLoaded(pos) || player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > reach * reach) {
                return;
            }
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity == null || SmwtiServerConfig.blacklisted(BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString())
                    || SmwtiServerConfig.blacklisted(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(entity.getType()).toString())) {
                return;
            }
            for (ServerBlockProvider provider : SmwtiApi.serverBlockProviders()) {
                try {
                    provider.collect(tooltip, level, pos, entity, detailed);
                } catch (RuntimeException e) {
                    Smwti.LOGGER.warn("A server block provider failed for {} at {}", entity.getType(), pos, e);
                }
            }
        }
        PacketDistributor.sendToPlayer(player, new Reply(target, tooltip.lines()));
    }
}
