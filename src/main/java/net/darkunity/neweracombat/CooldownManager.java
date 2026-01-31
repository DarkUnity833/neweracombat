package net.darkunity.neweracombat;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.InteractionHand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = NeweracombatMod.MODID)
public class CooldownManager {
    private static final Map<UUID, Float> offHandCooldown = new ConcurrentHashMap<>();
    private static final Map<UUID, Float> mainHandCooldown = new ConcurrentHashMap<>();
    private static final Map<UUID, Float> shieldCooldown = new ConcurrentHashMap<>();
    private static final Map<UUID, Float> fatigueLevel = new ConcurrentHashMap<>();
    
    private static final float BASE_WEAPON_COOLDOWN = 1.0f;
    private static final float BASE_SHIELD_COOLDOWN = 0.8f;
    private static final float MAX_FATIGUE = 30f;
    private static final float FATIGUE_DECAY_RATE = 0.5f; 
    private static final float FATIGUE_DECAY_TICK = FATIGUE_DECAY_RATE / 20f;

    public static void updateCooldowns(Player player) {
        if (player == null) return;
        UUID id = player.getUUID();
        
        updateFatigue(player);
        
        float attackSpeed = (float) player.getAttributeValue(Attributes.ATTACK_SPEED);
        float fatigue = getFatigueLevel(player);
        float fatiguePenalty = calculateFatiguePenalty(fatigue);
        
        // Балансировка прироста: учитываем скорость атаки и штраф усталости
        float weaponIncrement = (attackSpeed / 20.0f) * fatiguePenalty;
        float shieldIncrement = (attackSpeed / 25.0f) * fatiguePenalty; 
        
        // Обновляем значения, не выходя за пределы 1.0
        offHandCooldown.compute(id, (k, v) -> Math.min(BASE_WEAPON_COOLDOWN, (v == null ? 1.0f : v) + weaponIncrement));
        mainHandCooldown.compute(id, (k, v) -> Math.min(BASE_WEAPON_COOLDOWN, (v == null ? 1.0f : v) + weaponIncrement));
        
        if (isShieldInHands(player)) {
            shieldCooldown.compute(id, (k, v) -> Math.min(BASE_SHIELD_COOLDOWN, (v == null ? 0.8f : v) + shieldIncrement));
        } else {
            shieldCooldown.compute(id, (k, v) -> Math.min(BASE_SHIELD_COOLDOWN, (v == null ? 0.8f : v) + shieldIncrement * 2f));
        }
    }
    
    private static void updateFatigue(Player player) {
        UUID id = player.getUUID();
        fatigueLevel.computeIfPresent(id, (k, v) -> Math.max(0f, v - FATIGUE_DECAY_TICK));
    }
    
    private static float calculateFatiguePenalty(float fatigue) {
        if (fatigue <= 0) return 1.0f;
        if (fatigue <= 10f) return 0.8f;
        if (fatigue <= 20f) return 0.5f;
        return 0.2f;
    }
    
    private static boolean isShieldInHands(Player player) {
        return player.getMainHandItem().getItem() instanceof ShieldItem || 
               player.getOffhandItem().getItem() instanceof ShieldItem;
    }
    
    public static boolean isHandReady(Player player, InteractionHand hand) {
        // Синхронизируем порог с ClientCombatEvents (0.8f вместо 0.9f для отзывчивости)
        return getHandProgress(player, hand) >= 0.8f;
    }
    
    public static float getHandProgress(Player player, InteractionHand hand) {
        if (player == null) return BASE_WEAPON_COOLDOWN;
        UUID id = player.getUUID();
        return (hand == InteractionHand.MAIN_HAND) 
            ? mainHandCooldown.getOrDefault(id, BASE_WEAPON_COOLDOWN) 
            : offHandCooldown.getOrDefault(id, BASE_WEAPON_COOLDOWN);
    }
    
    public static float getOffHandProgress(Player player) { return getHandProgress(player, InteractionHand.OFF_HAND); }
    public static float getMainHandProgress(Player player) { return getHandProgress(player, InteractionHand.MAIN_HAND); }
    public static float getShieldProgress(Player player) { return shieldCooldown.getOrDefault(player.getUUID(), BASE_SHIELD_COOLDOWN); }
    
    public static void setHandCooldown(Player player, InteractionHand hand) {
        if (player == null) return;
        UUID id = player.getUUID();
        if (hand == InteractionHand.MAIN_HAND) mainHandCooldown.put(id, 0.0f);
        else offHandCooldown.put(id, 0.0f);
        
        addFatigue(player, 2.5f);
        
        // Удар мечом немного замедляет кулдаун щита
        shieldCooldown.computeIfPresent(id, (k, v) -> Math.max(0f, v - 0.2f));
    }
    
    public static void setShieldCooldown(Player player) {
        if (player == null) return;
        shieldCooldown.put(player.getUUID(), 0.0f);
        addFatigue(player, 3.0f);
    }
    
    public static void setOffHandCooldown(Player player) { setHandCooldown(player, InteractionHand.OFF_HAND); }
    public static void setMainHandCooldown(Player player) { setHandCooldown(player, InteractionHand.MAIN_HAND); }
    
    public static void addFatigue(Player player, float amount) {
        UUID id = player.getUUID();
        fatigueLevel.merge(id, amount, (oldV, newV) -> Math.min(MAX_FATIGUE, oldV + newV));
    }
    
    public static float getFatigueLevel(Player player) { return fatigueLevel.getOrDefault(player.getUUID(), 0f); }
    public static boolean isFatigued(Player player) { return getFatigueLevel(player) > 10f; }
    public static boolean isHeavilyFatigued(Player player) { return getFatigueLevel(player) > 20f; }
    
    public static void resetFatigue(Player player) { fatigueLevel.put(player.getUUID(), 0f); }
    
    public static void resetAllCooldowns(Player player) {
        UUID id = player.getUUID();
        offHandCooldown.remove(id);
        mainHandCooldown.remove(id);
        shieldCooldown.remove(id);
        resetFatigue(player);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        offHandCooldown.remove(id);
        mainHandCooldown.remove(id);
        shieldCooldown.remove(id);
        fatigueLevel.remove(id);
    }

    public static float getCooldownForItem(ItemStack stack) {
        if (stack.isEmpty()) return 0.5f;
        if (stack.getItem() instanceof SwordItem) return 1.0f;
        if (stack.getItem() instanceof AxeItem) return 1.2f;
        if (stack.getItem() instanceof ShieldItem) return 0.8f;
        return 0.6f;
    }
    
    public static float getAttackSpeedMultiplier(Player player, InteractionHand hand) {
        float progress = getHandProgress(player, hand);
        // Используем 0.8f как порог "полной силы"
        float cooldownMultiplier = progress >= 0.8f ? 1.0f : progress / 0.8f;
        return cooldownMultiplier * calculateFatiguePenalty(getFatigueLevel(player));
    }
}