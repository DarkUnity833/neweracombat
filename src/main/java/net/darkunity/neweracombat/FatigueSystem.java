package net.darkunity.neweracombat;

import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

import java.util.HashMap;
import java.util.UUID;

@EventBusSubscriber(modid = "neweracombat")
public class FatigueSystem {
    private static float fatigue = 0;
    private static final float MAX_FATIGUE = 20.0f;
    private static boolean staminaLocked = false;
    private static int breathTimer = 0; 

    private static final ResourceLocation FATIGUE_SLOWDOWN_ID = ResourceLocation.fromNamespaceAndPath("neweracombat", "fatigue_slowdown");
    public static final HashMap<UUID, Integer> SLIDE_TICKS = new HashMap<>();
    
    public static boolean shouldIgnoreMovementSystems(Player player) {
        if (player == null) return true;
        return player.isSpectator() || player.isCreative();
    }

    public static float getLevel() { return fatigue; }
    public static float getLevel(Player player) { return fatigue; }
    
    public static void addFatigue(Player player, float amount) {
        if (shouldIgnoreMovementSystems(player)) {
            fatigue = 0;
            staminaLocked = false;
            return;
        }
        fatigue = Math.max(0, Math.min(MAX_FATIGUE, fatigue + amount));
    }

    public static boolean isLocked() { return staminaLocked; }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (shouldIgnoreMovementSystems(event.getEntity())) return;
        addFatigue(event.getEntity(), 2.86f);
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || shouldIgnoreMovementSystems(player)) return;
        if (player.isBlocking()) {
            if (player.getUseItem().getItem() instanceof ShieldItem) addFatigue(player, 4.0f); 
        } else if (SwordBlockSystem.isBlocking(player)) {
            addFatigue(player, 7.0f); 
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // ВЫХОД ДЛЯ СПЕКТАТОРА: Без этого он не может летать сквозь стены
        if (player.isSpectator()) {
            if (player.level().isClientSide) {
                fatigue = 0; staminaLocked = false;
                SLIDE_TICKS.remove(player.getUUID());
            }
            return; 
        }

        UUID uuid = player.getUUID();
        boolean isClient = player.level().isClientSide;

        if (isClient) {
            // Креатив просто не тратит стамину
            if (player.isCreative() && (fatigue > 0 || staminaLocked)) {
                fatigue = 0; staminaLocked = false;
            }

            if (staminaLocked) {
                breathTimer++;
                if (breathTimer >= 40) { 
                    player.level().playSound(player, player.getX(), player.getY(), player.getZ(), 
                        SoundEvents.PLAYER_BREATH, SoundSource.PLAYERS, 0.8F, 0.8F);
                    breathTimer = 0;
                }
            }

            if (staminaLocked && !player.isFallFlying() && !player.getAbilities().flying) {
                player.setSprinting(false);
                if (player.isBlocking()) player.stopUsingItem(); 
                if (SwordBlockSystem.isBlocking(player)) SwordBlockSystem.stopBlocking(player);
            }

            float recoveryBase = staminaLocked ? 0.25f : 0.5f;
            if (player.hasEffect(MobEffects.REGENERATION)) recoveryBase *= 1.5f;

            if (player.isSprinting() && !player.isFallFlying() && !player.getAbilities().flying) {
                fatigue = Math.min(MAX_FATIGUE, fatigue + 0.05f);
            } else {
                fatigue = Math.max(0, fatigue - recoveryBase); 
            }

            if (fatigue >= MAX_FATIGUE) staminaLocked = true;
            if (staminaLocked && fatigue <= 0.01f) staminaLocked = false; 

            var speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speedAttr != null) {
                if (staminaLocked) {
                    if (!speedAttr.hasModifier(FATIGUE_SLOWDOWN_ID)) {
                        speedAttr.addTransientModifier(new AttributeModifier(FATIGUE_SLOWDOWN_ID, -0.15, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                    }
                } else {
                    speedAttr.removeModifier(FATIGUE_SLOWDOWN_ID);
                }
            }
        }

        // КРИТИЧЕСКИЙ ФИКС ПОЗЫ: 
        if (PoseHandler.CRAWLING_PLAYERS.contains(uuid)) {
            // Если игрок в спектаторе или летит — НИКАКИХ ПОЗ
            if (!player.isSpectator() && !player.getAbilities().flying && !player.isFallFlying() 
                && !WallParkourSystem.isHanging() && !HandlebarSystem.isGrabbingCeiling()) {
                player.setPose(Pose.SWIMMING);
                player.setSwimming(true);
            }
            SLIDE_TICKS.put(uuid, 1);
        } else {
            SLIDE_TICKS.remove(uuid);
        }
    }
}