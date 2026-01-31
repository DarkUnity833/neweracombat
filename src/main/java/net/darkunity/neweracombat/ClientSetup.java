package net.darkunity.neweracombat;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

@EventBusSubscriber(modid = "neweracombat", bus = EventBusSubscriber.Bus.MOD)
public final class ClientSetup {

   @SubscribeEvent
public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
    // .get() теперь не выдаст ошибку, так как мы вызвали ENTITIES.register(modEventBus)
    event.registerEntityRenderer(ModEntities.THROWN_AXE.get(), ThrownAxeRenderer::new);
}
}