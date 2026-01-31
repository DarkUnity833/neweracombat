package net.darkunity.neweracombat;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ServerboundSitPacket(boolean sitting) implements CustomPacketPayload {
    public static final Type<ServerboundSitPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("neweracombat", "sit_update"));
    
    public static final StreamCodec<FriendlyByteBuf, ServerboundSitPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, ServerboundSitPacket::sitting,
            ServerboundSitPacket::new
    );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(ServerboundSitPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) {
                if (msg.sitting()) {
                    PoseHandler.SITTING_PLAYERS.add(player.getUUID());
                } else {
                    PoseHandler.SITTING_PLAYERS.remove(player.getUUID());
                }
            }
        });
    }
}