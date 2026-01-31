package net.darkunity.neweracombat;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = "neweracombat", value = Dist.CLIENT)
public class WallParkourSystem {
    private static boolean isAttached = false;
    private static Vec3 attachedDirection = null;
    private static int hangTicks = 0;
    private static boolean wasShiftDown = false;

    private static final int[] OFFSETS_X = { -1, 1, 0, 0, 1, -1, 1, -1 };
    private static final int[] OFFSETS_Z = { 0, 0, 1, -1, 1, 1, -1, -1 };

    private static boolean isDisabled(Player player) {
        if (player == null) return true;
        return player.isSpectator() || 
               player.getAbilities().flying || 
               player.isFallFlying() || 
               player.isInWater() || 
               player.isSleeping() || 
               player.isPassenger();
    }

    @SubscribeEvent
    public static void onPlayerJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof Player player) || isDisabled(player)) return;
        // ... (логика прыжка без изменений)
        Vec3 velocity = player.getDeltaMovement();
        double horizontalSpeed = new Vec3(velocity.x, 0, velocity.z).length();
        Vec3 look = player.getLookAngle().normalize();

        boolean wallInRange = false;
        for (double d = 0.5; d <= 2.0; d += 0.5) {
            if (checkWallAt(player, look.scale(d), 0.0, 1.2)) {
                wallInRange = true;
                break;
            }
        }

        if (wallInRange) {
            if (horizontalSpeed > 0.18) {
                player.setDeltaMovement(velocity.x * 2.0, velocity.y + 0.12, velocity.z * 2.0);
            } else {
                player.setDeltaMovement(velocity.x, velocity.y + 0.14, velocity.z);
            }
        } else if (shouldBoostClose(player)) {
            player.setDeltaMovement(velocity.x, velocity.y + 0.12, velocity.z);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // ПЕРВЫЙ УРОВЕНЬ ЗАЩИТЫ: Мгновенный сброс при изменении режима
        if (isDisabled(player)) {
            if (isAttached) {
                isAttached = false;
                attachedDirection = null;
                hangTicks = 0;
            }
            return;
        }

        boolean isShiftDown = player.isShiftKeyDown();
        if (isShiftDown && !player.onGround()) {
            if (!isAttached) {
                Vec3 look = player.getLookAngle().normalize().scale(0.85);
                if (Math.abs(player.getLookAngle().y) < 0.8 && checkWallAt(player, look, 0.2, 1.7)) {
                    isAttached = true;
                    attachedDirection = new Vec3(look.x, 0, look.z).normalize().scale(0.85);
                    hangTicks = 0;
                }
            }
            if (isAttached) {
                player.fallDistance = 0;
                double slideSpeed = (hangTicks > 40) ? Math.min(0.2, (hangTicks - 40) * 0.01) : 0.01;
                player.setDeltaMovement(0, -slideSpeed, 0);
                hangTicks++;
                if (!checkWallAt(player, attachedDirection, 0.2, 1.7)) isAttached = false;
            }
        } else {
            if (!isShiftDown && wasShiftDown && isAttached) {
                Vec3 look = player.getLookAngle();
                player.setDeltaMovement(look.x * 0.4, 0.45, look.z * 0.4);
            }
            isAttached = false;
            attachedDirection = null;
            hangTicks = 0;
        }
        wasShiftDown = isShiftDown;
    }

    private static boolean shouldBoostClose(Player player) {
        if (isDisabled(player)) return false;
        CollisionContext ctx = CollisionContext.of(player);
        for (int i = 0; i < OFFSETS_X.length; i++) {
            BlockPos pos = BlockPos.containing(player.getX() + OFFSETS_X[i], player.getY(), player.getZ() + OFFSETS_Z[i]);
            var state = player.level().getBlockState(pos);
            if (state.getBlock() instanceof FenceBlock || state.getBlock() instanceof WallBlock || state.getBlock() instanceof FenceGateBlock ||
                (!state.isAir() && !state.getCollisionShape(player.level(), pos, ctx).isEmpty())) return true;
        }
        return false;
    }

    private static boolean checkWallAt(Player player, Vec3 direction, double minH, double maxH) {
        if (isDisabled(player)) return false;
        CollisionContext ctx = CollisionContext.of(player);
        for (double h = minH; h <= maxH; h += 0.5) {
            BlockPos pos = BlockPos.containing(player.getX() + direction.x, player.getY() + h, player.getZ() + direction.z);
            var state = player.level().getBlockState(pos);
            if (!state.isAir() && !state.getCollisionShape(player.level(), pos, ctx).isEmpty()) return true;
        }
        return false;
    }

    // ВТОРОЙ УРОВЕНЬ ЗАЩИТЫ: Геттер учитывает режим
    public static boolean isHanging() { 
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        
        if (mc.player.isSpectator() || mc.player.getAbilities().flying) {
            isAttached = false; 
            return false;
        }
        return isAttached; 
    }
}