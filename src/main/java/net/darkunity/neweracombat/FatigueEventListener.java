package net.darkunity.neweracombat;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;

@EventBusSubscriber(modid = "neweracombat")
public class FatigueEventListener {
    @SubscribeEvent
    public static void onPlayerJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.player.Player player) {
            FatigueSystem.addFatigue(player, 1.5f);
        }
    }
}