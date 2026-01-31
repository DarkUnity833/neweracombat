package net.darkunity.neweracombat.mixin;

import net.darkunity.neweracombat.PoseHandler;
import net.darkunity.neweracombat.WallParkourSystem;
import net.darkunity.neweracombat.WallRunSystem;
import net.darkunity.neweracombat.HandlebarSystem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    // === УТИЛИТА ДЛЯ БЕЗОПАСНОГО ПРОПУСКА ===
    private boolean neweracombat$shouldIgnore(Player player) {
        return player.isSpectator()
                || player.isCreative()
                || player.getAbilities().mayfly
                || player.getAbilities().flying;
    }

    @Inject(method = {"setPose", "m_20124_"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void neweracombat$lockPose(Pose pose, CallbackInfo ci) {
        if ((Object) this instanceof Player player) {

            // 🔥 КРИТИЧНЫЙ ФИКС
            if (neweracombat$shouldIgnore(player)) return;

            if (WallParkourSystem.isHanging()
                    || WallRunSystem.isActive()
                    || HandlebarSystem.isGrabbingCeiling()) {

                if (pose != Pose.STANDING) {
                    player.setPose(Pose.STANDING);
                    ci.cancel();
                }
            }
        }
    }

    @Inject(method = {"isCrouching", "m_6147_"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void neweracombat$cancelCrouchDuringParkour(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Player player) {

            // 🔥 КРИТИЧНЫЙ ФИКС
            if (neweracombat$shouldIgnore(player)) return;

            if (WallParkourSystem.isHanging()
                    || WallRunSystem.isActive()
                    || HandlebarSystem.isGrabbingCeiling()) {

                cir.setReturnValue(false);
            }
        }
    }

    @ModifyVariable(method = "travel", at = @At("STORE"), ordinal = 0, require = 0)
    private float neweracombat$applySlideFriction(float friction) {
        if ((Object) this instanceof Player player) {

            // 🔥 КРИТИЧНЫЙ ФИКС
            if (neweracombat$shouldIgnore(player)) {
                return friction;
            }

            if (WallParkourSystem.isHanging()
                    || WallRunSystem.isActive()
                    || HandlebarSystem.isGrabbingCeiling()) {

                return 1.0F;
            }
        }
        return friction;
    }

    @Inject(method = {"travel", "m_7023_"}, at = @At("HEAD"), require = 0)
    private void neweracombat$applySlideInertiaFix(Vec3 travelVector, CallbackInfo ci) {
        if ((Object) this instanceof Player player) {

            // 🔥 КРИТИЧНЫЙ ФИКС
            if (neweracombat$shouldIgnore(player)) return;

            if (PoseHandler.CRAWLING_PLAYERS.contains(player.getUUID())
                    && !WallParkourSystem.isHanging()) {

                if (player.onGround()) {
                    Vec3 delta = player.getDeltaMovement();
                    player.setDeltaMovement(
                            delta.x * 0.93,
                            delta.y,
                            delta.z * 0.93
                    );
                }
            }
        }
    }
}
