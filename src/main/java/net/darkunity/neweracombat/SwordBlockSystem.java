package net.darkunity.neweracombat;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = NeweracombatMod.MODID)
public class SwordBlockSystem {
    private static final Map<UUID, Boolean> isBlockingMap = new HashMap<>();
    private static final Map<UUID, Integer> blockTimerMap = new HashMap<>();

    // МЕТОДЫ ДЛЯ КЛИЕНТА (чтобы не было ошибок компиляции)
    public static boolean isBlocking(Player player) {
        return isBlockingMap.getOrDefault(player.getUUID(), false);
    }

    public static void startBlocking(Player player) {
        setBlocking(player, true);
    }

    public static void stopBlocking(Player player) {
        setBlocking(player, false);
    }

    public static void setBlocking(Player player, boolean blocking) {
        UUID playerId = player.getUUID();
        boolean wasBlocking = isBlockingMap.getOrDefault(playerId, false);
        
        if (blocking != wasBlocking) {
            isBlockingMap.put(playerId, blocking);
            if (blocking) {
                blockTimerMap.put(playerId, 0);
                // Тут можно добавить пакет для синхронизации анимации
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (isBlocking(player)) {
                ItemStack mainHand = player.getMainHandItem();
                ItemStack offHand = player.getOffhandItem();
                
                if (mainHand.getItem() instanceof SwordItem || offHand.getItem() instanceof SwordItem) {
                    float originalDamage = event.getAmount();
                    int ticks = blockTimerMap.getOrDefault(player.getUUID(), 0);
                    
                    // Твоя логика идеального блока
                    if (ticks < 10) { 
                        event.setAmount(originalDamage * 0.2f);
                        playBlockEffects(player, true);
                        
                        // Твоя усталость
                        try {
                            if (originalDamage > 2.0f) {
                                FatigueSystem.addFatigue(player, originalDamage * 0.3f);
                            }
                        } catch (Exception ignored) {}
                    } else {
                        // Обычный блок
                        event.setAmount(originalDamage * 0.5f);
                        playBlockEffects(player, true);
                        try {
                            FatigueSystem.addFatigue(player, 1.0f);
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
    }

    private static void playBlockEffects(Player player, boolean successful) {
        if (successful) {
            if (player.level() instanceof ServerLevel serverLevel) {
                double px = player.getX() + player.getLookAngle().x * 0.7;
                double py = player.getY() + 1.2;
                double pz = player.getZ() + player.getLookAngle().z * 0.7;

                serverLevel.sendParticles(ParticleTypes.FIREWORK, px, py, pz, 10, 0.1, 0.1, 0.1, 0.05);
                serverLevel.sendParticles(ParticleTypes.CRIT, px, py, pz, 5, 0.2, 0.2, 0.2, 0.1);
            }
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), 
                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0f, 0.9f + player.getRandom().nextFloat() * 0.3f);
        }
    }
}