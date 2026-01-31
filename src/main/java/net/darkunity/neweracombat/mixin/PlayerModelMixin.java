package net.darkunity.neweracombat.mixin;

import net.darkunity.neweracombat.HandlebarSystem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin<T extends LivingEntity> extends HumanoidModel<T> {

    @Shadow @Final public ModelPart leftSleeve;
    @Shadow @Final public ModelPart rightSleeve;

    public PlayerModelMixin(ModelPart part) {
        super(part);
    }

    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void neweracombat$handlebarAnimations(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (HandlebarSystem.isGrabbingCeiling()) {
            // 1. БАЗОВАЯ ПОЗА (Руки вверх)
            float baseArmAngle = -2.9F; // Почти вертикально
            
            // 2. ПОКАЧИВАНИЕ (Зависит от времени жизни сущности)
            // Плавный синус для создания эффекта висения на весу
            float swingX = Mth.sin(ageInTicks * 0.1F) * 0.05F;
            float swingZ = Mth.cos(ageInTicks * 0.1F) * 0.03F;

            // Применяем к основным частям (руки)
            this.rightArm.xRot = baseArmAngle + swingX;
            this.rightArm.zRot = 0.1F + swingZ;
            
            this.leftArm.xRot = baseArmAngle - swingX;
            this.leftArm.zRot = -0.1F - swingZ;

            // 3. ИСПРАВЛЕНИЕ СЛОЯ ОДЕЖДЫ (Sleeves)
            // Копируем углы из основных рук в "рукава"
            this.rightSleeve.copyFrom(this.rightArm);
            this.leftSleeve.copyFrom(this.leftArm);

            // 4. ПОКАЧИВАНИЕ ТЕЛА (Torso)
            // Чтобы всё тело немного "дышало" при висении
            this.body.xRot = Mth.sin(ageInTicks * 0.1F) * 0.02F;
            this.body.zRot = Mth.cos(ageInTicks * 0.1F) * 0.02F;
        }
    }
}