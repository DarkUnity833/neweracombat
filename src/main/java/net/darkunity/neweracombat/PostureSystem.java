package net.darkunity.neweracombat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = NeweracombatMod.MODID)
public class PostureSystem {
    // Используем ConcurrentHashMap для стабильности
    private static final Map<UUID, Float> postureData = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof Player player) {
            // Сокрушительный удар (Jump Attack)
            if (!player.onGround() && player.fallDistance > 0.2f) {
                // В NeoForge 1.21 лучше перемножать существующее значение
                event.setAmount(event.getAmount() * 1.5f);
                
                player.level().playSound(null, event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), 
                    SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.5f, 1.6f);
                
                if (player.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.ENCHANTED_HIT, 
                        event.getEntity().getX(), event.getEntity().getY() + 1, event.getEntity().getZ(), 
                        15, 0.3, 0.3, 0.3, 0.15);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        if (event.getSource().getEntity() instanceof LivingEntity) {
            LivingEntity victim = event.getEntity();
            if (!victim.isAlive()) return;
            
            UUID id = victim.getUUID();
            float currentPosture = postureData.getOrDefault(id, 0f);
            
            // Накопление постуры зависит от итогового нанесенного урона
            currentPosture += event.getNewDamage() * 2.0f;

            if (currentPosture >= 25f) {
                // СТАН (Оглушение)
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 9, false, false));
                victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 2, false, false)); // Добавим слабость при стане
                
                victim.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(), 
                    SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 1.0f, 0.7f);
                currentPosture = 0;
            }
            postureData.put(id, currentPosture);
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof LivingEntity living && living.isAlive()) {
            // Эффект визуализации стана
            if (living.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
                MobEffectInstance effect = living.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
                if (effect != null && effect.getAmplifier() >= 9) {
                    if (living.level() instanceof ServerLevel sl && living.tickCount % 5 == 0) {
                        sl.sendParticles(ParticleTypes.CRIT, 
                            living.getX(), living.getY() + living.getBbHeight() + 0.2, living.getZ(), 
                            3, 0.1, 0.05, 0.1, 0.05);
                    }
                }
            }
            
            // Реген постуры (0.1f за тик — чуть быстрее, чтобы был смысл в агрессии)
            UUID id = living.getUUID();
            postureData.computeIfPresent(id, (uuid, value) -> Math.max(0, value - 0.1f));
        }
    }

    // ОЧИСТКА ДАННЫХ (предотвращает утечку памяти)
    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        postureData.remove(event.getEntity().getUUID());
    }
}