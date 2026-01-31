package net.darkunity.neweracombat;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = "neweracombat")
public class ServerCombatEvents {
    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        CooldownManager.updateCooldowns(event.getEntity());
    }
}