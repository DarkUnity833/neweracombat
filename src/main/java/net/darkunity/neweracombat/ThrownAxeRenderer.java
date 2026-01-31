package net.darkunity.neweracombat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;

public class ThrownAxeRenderer extends EntityRenderer<ThrownAxeEntity> {
    private final ItemRenderer itemRenderer;

    public ThrownAxeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(ThrownAxeEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        
        float fYaw = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
        float fPitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
        
        if (entity.isStuckInBlock()) {
            // 1. Стабилизация: Убираем наклон Pitch, чтобы лезвие не ложилось плашмя
            fPitch = 0.0F; 
            
            // 2. Если его косит влево/вправо при прямом броске, 
            // значит Yaw нужно использовать тот, который был в момент ПОПАДАНИЯ.
            // Но мы оставим текущий, просто уберем из него лишние вращения.
        }

        // Поворот корпуса по направлению полета
        poseStack.mulPose(Axis.YP.rotationDegrees(fYaw - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(fPitch));

        if (!entity.isStuckInBlock()) {
            // Вращение в полете
            float rotation = (entity.tickCount + partialTicks) * 25.0F;
            poseStack.mulPose(Axis.ZP.rotationDegrees(rotation));
            poseStack.translate(0.0, -0.1, 0.0);
        } else {
            // --- НОВАЯ ЛОГИКА ПОЗИЦИОНИРОВАНИЯ ---
            
            // 1. Сдвигаем модель НАЗАД (от центра сущности к рукоятке).
            // Если он косит вбок — уменьши 0.1 до 0.0.
            poseStack.translate(-0.3, 0.0, 0.0); 

            // 2. Втыкаем лезвие (поворот палки вверх). 
            // -90 это строго перпендикулярно.
            poseStack.mulPose(Axis.ZP.rotationDegrees(-100.0F));

            // 3. ЦЕНТРИРОВАНИЕ ЛЕЗВИЯ
            // Чтобы лезвие не уходило влево/вправо, translate по Z должен быть 0.
            // Сдвигаем только по X и Y, чтобы выставить острие в точку удара.
            poseStack.translate(0.15, 0.1, 0.0); 
        }

        this.itemRenderer.renderStatic(
                entity.getItem(),
                ItemDisplayContext.GROUND,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                entity.level(),
                entity.getId()
        );
        
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ThrownAxeEntity entity) { return null; }
}