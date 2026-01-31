package net.darkunity.neweracombat;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = NeweracombatMod.MODID)
public class CommonEvents {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        // Это работает на сервере для каждого игрока
        if (event.getEntity() != null) {
            CooldownManager.updateCooldowns(event.getEntity());
        }
    }
}