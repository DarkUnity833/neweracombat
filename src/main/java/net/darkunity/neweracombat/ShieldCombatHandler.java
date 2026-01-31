package net.darkunity.neweracombat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ShieldItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = "neweracombat", value = Dist.CLIENT)
public class ShieldCombatHandler {
    
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        // Клиентская логика для отображения блокировки
        if (event.getEntity().level().isClientSide && event.getEntity() == Minecraft.getInstance().player) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            
            if (mc.player.isUsingItem() && mc.player.getUseItem().getItem() instanceof ShieldItem) {
                // В 1.21.1 getUseDuration требует LivingEntity параметр
                if (mc.player.getUseItemRemainingTicks() < mc.player.getUseItem().getUseDuration(mc.player) - 5) {
                    // Ваша логика анимации блокировки
                }
            }
        }
    }
}