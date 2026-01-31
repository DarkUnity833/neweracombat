package net.darkunity.neweracombat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;

@EventBusSubscriber(modid = "neweracombat", value = Dist.CLIENT)
public class DashSystem {
    private static long lastDashTime = 0;
    private static final long DASH_COOLDOWN = 1000;

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        // Проверяем нажатие кнопки Dash из настроек
        if (ModKeyBindings.DASH_KEY.isDown()) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastDashTime < DASH_COOLDOWN) return;

            // Проверка стамины: если стамина меньше 18 (почти максимум), разрешаем дэш
            // Убрал shouldIgnoreMovementSystems, чтобы точно работало везде
            if (FatigueSystem.getLevel(mc.player) <= 18.0f) {
                performDash(mc);
                FatigueSystem.addFatigue(mc.player, 4.0f); // Твой расход
                lastDashTime = currentTime;
            }
        }
    }

    private static void performDash(Minecraft mc) {
        var player = mc.player;
        Vec3 look = player.getLookAngle();
        Vec3 side = look.cross(new Vec3(0, 1, 0)).normalize();
        double speed = 0.8; 
        
        ParkourRoll.activateRollWindow();
        
        player.level().playSound(player, player.getX(), player.getY(), player.getZ(), 
            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.5F);

        // ТВОЯ ОРИГИНАЛЬНАЯ ЛОГИКА НАПРАВЛЕНИЙ
        if (mc.options.keyLeft.isDown()) {
            player.setDeltaMovement(player.getDeltaMovement().add(side.scale(-speed)));
        } else if (mc.options.keyRight.isDown()) {
            player.setDeltaMovement(player.getDeltaMovement().add(side.scale(speed)));
        } else if (mc.options.keyDown.isDown()) {
            player.setDeltaMovement(player.getDeltaMovement().add(look.scale(-speed)));
        } else {
            // Вперед: убираем Y, чтобы не рыть носом землю
            player.setDeltaMovement(player.getDeltaMovement().add(look.x * speed, 0, look.z * speed));
        }
        
        player.hasImpulse = true;
        player.hurtMarked = true; // Важно для плавности на клиенте
    }
}