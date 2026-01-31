package net.darkunity.neweracombat;

import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = "neweracombat")
public class CombatTickEvents {
    private static final ResourceLocation SLOW_ID = ResourceLocation.fromNamespaceAndPath("neweracombat", "shield_slow");
    private static final AttributeModifier SHIELD_SLOW = new AttributeModifier(SLOW_ID, -0.2D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Замедление при блокировании щитом
            AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) {
                if (player.isUsingItem() && player.getUseItem().getItem() instanceof net.minecraft.world.item.ShieldItem) {
                    if (!speed.hasModifier(SLOW_ID)) speed.addTransientModifier(SHIELD_SLOW);
                } else {
                    if (speed.hasModifier(SLOW_ID)) speed.removeModifier(SLOW_ID);
                }
            }
        }
    }
}