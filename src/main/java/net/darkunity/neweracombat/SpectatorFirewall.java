package net.darkunity.neweracombat.firewall;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public final class SpectatorFirewall {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        if (!(event.getEntity() instanceof LocalPlayer player)) return;
        if (Minecraft.getInstance().player != player) return;

        if (!player.isSpectator()) return;

        // === ЖЁСТКИЙ ВАНИЛЬНЫЙ СБРОС ===

        // spectator ВСЕГДА летает
        player.getAbilities().flying = true;
        player.getAbilities().mayfly = true;

        // Никакой физики LivingEntity
        player.setDeltaMovement(Vec3.ZERO);

        // Никаких поз
        player.setPose(Pose.STANDING);

        // Никакой гравитации
        player.fallDistance = 0;
    }
}
