package net.darkunity.neweracombat;

import net.minecraft.world.entity.player.Player;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = "neweracombat")
public class ParkourRoll {

    private static int rollWindow = 0;

    public static void activateRollWindow() {
        rollWindow = 15; // Окно ~0.75 сек
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (rollWindow > 0) {
            rollWindow--;
        }
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player) {
            // ИСПРАВЛЕНИЕ: Убрана проверка высоты падения, оставлена только проверка окна переката
            if (rollWindow > 0) {
                
                // 1. Отмена урона
                event.setDistance(0);
                event.setDamageMultiplier(0);
                
                // 2. Механика "Второго дыхания"
                // Восстанавливаем 3 единицы усталости за техничное приземление
                float currentFatigue = FatigueSystem.getLevel(player);
                FatigueSystem.addFatigue(player, -3.0f); 

                // 3. Эффекты
                if (!player.level().isClientSide) {
                    ServerLevel sl = (ServerLevel) player.level();
                    
                    // Белый дым (эффект переката)
                    sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, 
                        player.getX(), player.getY(), player.getZ(), 
                        8, 0.2, 0.1, 0.2, 0.02);
                    
                    // Бирюзовые искры (визуал восстановления сил)
                    sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, 
                        player.getX(), player.getY() + 1, player.getZ(), 
                        10, 0.5, 0.5, 0.5, 0.1);
                        
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(), 
                        SoundEvents.PLAYER_SMALL_FALL, SoundSource.PLAYERS, 1.0f, 1.3f);
                    
                    // Звук "вздоха" или восстановления
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(), 
                        SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.3f, 1.5f);
                }
                
                rollWindow = 0; // Закрываем окно
            }
        }
    }
}