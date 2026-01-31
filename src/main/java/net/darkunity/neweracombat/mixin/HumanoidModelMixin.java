package net.darkunity.neweracombat.mixin;

import net.darkunity.neweracombat.SittableModel;
import net.darkunity.neweracombat.SwordBlockSystem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.SwordItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin<T extends LivingEntity> implements SittableModel {
    @Unique
    private boolean forcedSitting;

    @Override
    public void setForcedSitting(boolean sitting) {
        this.forcedSitting = sitting;
    }

    @Inject(method = {"setupAnim", "m_6973_"}, at = @At("TAIL"), remap = false)
    private void injectSittingPose(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        HumanoidModel<?> model = (HumanoidModel<?>)(Object)this;

        if (this.forcedSitting) {
            model.riding = true;
            model.body.y = 12.0F; 
            model.head.y = 12.0F; 
            model.leftLeg.y = 22.0F;  
            model.rightLeg.y = 22.0F;
            model.leftLeg.xRot = -1.5708F; 
            model.rightLeg.xRot = -1.5708F;
            model.leftLeg.yRot = -0.3F;
            model.rightLeg.yRot = 0.3F;
            
            float armY = 12.0F; 
            model.leftArm.y = armY;
            model.rightArm.y = armY;

            if (model.attackTime <= 0.0F) {
                model.leftArm.xRot = -1.1F; 
                model.rightArm.xRot = -1.1F;
                model.leftArm.yRot = 0.45F;
                model.rightArm.yRot = -0.45F;
            }
        }

        if (entity instanceof Player player && SwordBlockSystem.isBlocking(player)) {
            boolean hasMainHandSword = player.getMainHandItem().getItem() instanceof SwordItem;
            boolean hasOffHandSword = player.getOffhandItem().getItem() instanceof SwordItem;
            boolean isAttacking = model.attackTime > 0.0F;
            boolean isMainArmSwinging = player.swingingArm == net.minecraft.world.InteractionHand.MAIN_HAND;
            boolean isOffArmSwinging = player.swingingArm == net.minecraft.world.InteractionHand.OFF_HAND;

            if (hasMainHandSword && hasOffHandSword) {
                if (!(isAttacking && isMainArmSwinging)) {
                    model.rightArm.xRot = -1.240F;
                    model.rightArm.yRot = -0.070F;
                    model.rightArm.zRot = 0.700F;
                }
                if (!(isAttacking && isOffArmSwinging)) {
                    model.leftArm.xRot = -1.240F;
                    model.leftArm.yRot = 0.070F;
                    model.leftArm.zRot = -0.700F;
                }
            } else if (hasMainHandSword) {
                if (!isAttacking) {
                    model.rightArm.xRot = -1.240F;
                    model.rightArm.yRot = -0.070F;
                    model.rightArm.zRot = 0.700F;
                }
            } else if (hasOffHandSword) {
                if (!isAttacking) {
                    model.leftArm.xRot = -1.240F;
                    model.leftArm.yRot = 0.070F;
                    model.leftArm.zRot = -0.700F;
                }
            }
        }
    }
}