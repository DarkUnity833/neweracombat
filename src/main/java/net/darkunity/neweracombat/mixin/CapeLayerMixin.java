package net.darkunity.neweracombat.mixin;

import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.player.AbstractClientPlayer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CapeLayer.class)
public class CapeLayerMixin {
    
    /**
     * Полная блокировка рендеринга плаща.
     * Мы инжектимся в самое начало (HEAD) и отменяем метод.
     */
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void disableAllCapes(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        ci.cancel();
    }
}