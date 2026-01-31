package net.darkunity.neweracombat;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Pose;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = "neweracombat", value = Dist.CLIENT)
public class WallRunSystem {
    private static boolean isWallRunning = false;
    private static boolean wasShiftDown = false;

    private static boolean isDisabled(net.minecraft.world.entity.player.Player player) {
        return player == null || 
               player.isSpectator() || 
               player.getAbilities().flying || 
               player.isFallFlying() ||
               player.isPassenger() ||
               player.isSleeping();
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.getEntity() != mc.player || mc.player == null) return;
        var player = mc.player;
        
        // ПЕРВЫЙ УРОВЕНЬ ЗАЩИТЫ
        if (isDisabled(player)) {
            isWallRunning = false;
            return;
        }

        if (isWallRunning) {
            player.setPose(Pose.STANDING);
            player.fallDistance = 0;
            if (mc.options.keyJump.isDown()) {
                Vec3 look = player.getLookAngle();
                player.setDeltaMovement(look.x * 0.8, 0.45, look.z * 0.8);
                isWallRunning = false;
            }
        }

        boolean isShiftDown = mc.options.keyShift.isDown();
        if (!isShiftDown && wasShiftDown && isWallRunning) isWallRunning = false;
        wasShiftDown = isShiftDown;
    }

    @SubscribeEvent
    public static void onRenderTick(RenderFrameEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.isPaused()) return;
        var player = mc.player;

        if (isDisabled(player)) {
            isWallRunning = false;
            return;
        }
        // ... (логика рендера)
        Vec3 velocity = player.getDeltaMovement();
        if (mc.options.keyShift.isDown() && !player.onGround() && velocity.horizontalDistanceSqr() > 0.001) {
            if (hasWallNearby(player)) {
                isWallRunning = true;
                player.setPose(Pose.STANDING);
                double targetY = player.getLookAngle().y * 0.28;
                player.setDeltaMovement(velocity.x, Math.max(-0.12, Math.min(0.12, targetY + 0.01)), velocity.z);
            } else {
                isWallRunning = false;
            }
        } else {
            isWallRunning = false;
        }
    }

    private static boolean hasWallNearby(net.minecraft.client.player.LocalPlayer player) {
        if (isDisabled(player)) return false; 
        
        double d = 0.8;
        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(i * 45);
            double dx = Math.cos(angle) * d;
            double dz = Math.sin(angle) * d;
            BlockPos pos = BlockPos.containing(player.getX() + dx, player.getY() + 0.5, player.getZ() + dz);
            if (player.level().getBlockState(pos).isCollisionShapeFullBlock(player.level(), pos)) return true;
        }
        return false;
    }

    // ВТОРОЙ УРОВЕНЬ ЗАЩИТЫ
    public static boolean isActive() { 
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        
        if (mc.player.isSpectator() || mc.player.getAbilities().flying) {
            isWallRunning = false;
            return false;
        }
        return isWallRunning; 
    }
}