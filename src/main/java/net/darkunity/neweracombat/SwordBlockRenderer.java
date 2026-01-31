package net.darkunity.neweracombat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.SwordItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;

@EventBusSubscriber(modid = "neweracombat", value = Dist.CLIENT)
public class SwordBlockRenderer {

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !SwordBlockSystem.isBlocking(mc.player)) return;
        if (!(event.getItemStack().getItem() instanceof SwordItem)) return;

        PoseStack poseStack = event.getPoseStack();
        HumanoidArm arm = (event.getHand() == InteractionHand.MAIN_HAND) ? 
                          mc.player.getMainArm() : mc.player.getMainArm().getOpposite();
        
        boolean isDualWielding = mc.player.getMainHandItem().getItem() instanceof SwordItem && 
                                 mc.player.getOffhandItem().getItem() instanceof SwordItem;

        // Если меча два — убираем ванильное опускание, будем рулить высотой сами
        if (!isDualWielding) {
            poseStack.translate(0, event.getEquipProgress() * 0.6F, 0);
        }

        if (isDualWielding) {
            DualWieldBlockRenderer.render(poseStack, arm);
        } else {
            // ОДИНОЧНЫЙ БЛОК (Твой идеал)
            renderSingleBlock(poseStack, arm == HumanoidArm.RIGHT);
        }
    }

    private static void renderSingleBlock(PoseStack poseStack, boolean isRightHand) {
        if (isRightHand) {
            poseStack.translate(-0.32F, 0.1F, 0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(70.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(50.0F));
        } else {
            poseStack.translate(0.32F, 0.1F, 0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(-70.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(-50.0F));
        }
    }
}