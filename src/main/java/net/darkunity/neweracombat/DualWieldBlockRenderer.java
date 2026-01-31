package net.darkunity.neweracombat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.world.entity.HumanoidArm;

public class DualWieldBlockRenderer {
    public static void render(PoseStack poseStack, HumanoidArm arm) {
        if (arm == HumanoidArm.RIGHT) {
            // Твой эталон для правой руки
            poseStack.translate(-0.32F, 0.1F, 0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(70.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(50.0F));
        } else {
            // ЛЕВАЯ РУКА (Твои финальные координаты)
            if (DualWieldDebug.DEBUG_MODE) {
                // Если дебаг включен — берем значения из оверлея
                poseStack.translate(DualWieldDebug.x, DualWieldDebug.y, DualWieldDebug.z);
                poseStack.mulPose(Axis.XP.rotationDegrees(DualWieldDebug.xp));
                poseStack.mulPose(Axis.YP.rotationDegrees(DualWieldDebug.yp));
                poseStack.mulPose(Axis.ZP.rotationDegrees(DualWieldDebug.zp));
            } else {
                // Если дебаг выключен — используем найденный идеал
                poseStack.translate(0.685F, -0.630F, -0.870F);
                poseStack.mulPose(Axis.XP.rotationDegrees(77.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(-13.5F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-56.0F));
            }
        }
    }
}