package net.darkunity.neweracombat;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ServerboundPoseUpdatePacket(boolean crawling) implements CustomPacketPayload {
    public static final Type<ServerboundPoseUpdatePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("neweracombat", "pose_update"));
    
    public static final StreamCodec<FriendlyByteBuf, ServerboundPoseUpdatePacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, ServerboundPoseUpdatePacket::crawling,
            ServerboundPoseUpdatePacket::new
    );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(ServerboundPoseUpdatePacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) {
                if (msg.crawling()) {
                    PoseHandler.CRAWLING_PLAYERS.add(player.getUUID());
                } else {
                    PoseHandler.CRAWLING_PLAYERS.remove(player.getUUID());
                }
            }
        });
    }
}