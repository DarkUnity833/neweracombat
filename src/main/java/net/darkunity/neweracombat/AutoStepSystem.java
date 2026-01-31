package net.darkunity.neweracombat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = "neweracombat")
public class AutoStepSystem {
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        var player = event.getEntity();
        if (player == null || !player.level().isClientSide) return;

        // 1. Логика AutoStep
        AttributeInstance stepHeight = player.getAttribute(Attributes.STEP_HEIGHT);
        if (stepHeight != null) {
            double currentSpeed = player.getDeltaMovement().horizontalDistance();
            
            // УСЛОВИЯ ОТКЛЮЧЕНИЯ: 
            // - Нажат Shift
            // - Или скорость слишком низкая (подошел вплотную и стоишь/еле идешь)
            if (player.isShiftKeyDown() || currentSpeed < 0.05) {
                if (stepHeight.getBaseValue() != 0.6) {
                    stepHeight.setBaseValue(0.6); // Стандартная высота (полблока)
                }
            } else {
                // В движении и без шифта - заходим на блоки
                if (stepHeight.getBaseValue() != 1.06) {
                    stepHeight.setBaseValue(1.06);
                }
            }
        }

        // 2. Силовое выключение ванильного индикатора
        var mc = Minecraft.getInstance();
        if (mc.options.attackIndicator().get() != AttackIndicatorStatus.OFF) {
            mc.options.attackIndicator().set(AttackIndicatorStatus.OFF);
        }
    }
}