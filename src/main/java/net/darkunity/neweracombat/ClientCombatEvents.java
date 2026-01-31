package net.darkunity.neweracombat;

import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.LeadItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Глобальный клиентский контроллер боевой системы NewEraCombat.
 * Содержит полную иерархию приоритетов: Взаимодействие -> Строительство -> Бой.
 * Исправлены ошибки строительства на Shift, использования яиц спавна и кулдауна урона.
 * РЕШЕНО: Попеременная атака в блоке (без отскоков) и блок с предметами в руках.
 * ОБНОВЛЕНО: Приоритет стрелкового оружия и фикс "дерганья" анимаций при усталости.
 */
@EventBusSubscriber(modid = NeweracombatMod.MODID, value = Dist.CLIENT)
public class ClientCombatEvents {
    private static final Logger LOGGER = LogManager.getLogger("NewEraCombat-Client");
    
    private static boolean isMainHandNext = true;
    private static long lastRightClickTime = 0;
    private static final long RIGHT_CLICK_COOLDOWN = 300;
    private static int attackDelay = 0;

    @SubscribeEvent
    public static void onClientTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player == null || !player.level().isClientSide) return;
        
        if (player == Minecraft.getInstance().player) {
            if (attackDelay > 0) attackDelay--;

            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            // Логика SwordBlockSystem: прекращаем блок, если кнопка не зажата
            if (SwordBlockSystem.isBlocking(mc.player) && !mc.options.keyUse.isDown()) {
                SwordBlockSystem.stopBlocking(mc.player);
            }
            
            CooldownManager.updateCooldowns(player);
        }
    }

    /**
     * Обработчик мыши (ЛКМ и ПКМ).
     * ФИКС: Блокируем ПКМ на уровне ввода, чтобы не было замаха.
     */
    @SubscribeEvent
    public static void onMouseClick(InputEvent.MouseButton.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || mc.player.isSpectator()) return;

        // ЛКМ (Атака)
        if (event.getButton() == 0 && event.getAction() == 1) { 
            if (isBreakingBlock(mc)) return;

            if (attackDelay <= 0) {
                InteractionHand attackHand = getAttackHand(mc);
                if (performAttack(mc, attackHand)) {
                    event.setCanceled(true); 
                    updateNextHand(mc);
                    attackDelay = 4;
                }
            }
        }

        // ПКМ (Использование / Блок)
        if (event.getButton() == 1 && event.getAction() == 1) {
            if (FatigueSystem.isLocked()) {
                ItemStack main = mc.player.getMainHandItem();
                ItemStack off = mc.player.getOffhandItem();
                
                // Если в руках НЕ утилитарный предмет (еда/блоки) — отменяем клик СОВСЕМ
                if (!isAllowedUtility(main) && !isAllowedUtility(off)) {
                    event.setCanceled(true);
                }
            }
        }
    }

    /**
     * Обработчик ПКМ (Блок, Метание, Строительство).
     */
    @SubscribeEvent
    public static void onRightClickTrigger(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.hitResult == null) return;

        // ПРОВЕРКА УСТАЛОСТИ: Фикс "дерганья" - отменяем триггер полностью
        if (FatigueSystem.isLocked()) {
            ItemStack main = mc.player.getMainHandItem();
            ItemStack off = mc.player.getOffhandItem();
            if (!isAllowedUtility(main) && !isAllowedUtility(off)) {
                event.setCanceled(true);
                return;
            }
        }

        Player player = mc.player;
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();

        // НОВОЕ: ПРИОРИТЕТ СТРЕЛЬБЫ. Если в руках лук, арбалет или трезубец, блок мечом игнорируется.
        if (isRangedWeapon(main) || isRangedWeapon(off)) {
            return; 
        }

       // 1. СИСТЕМА МЕТАНИЯ ТОПОРА (Shift + ПКМ)
        if (player.isShiftKeyDown() && hasAxe(player) && !isTryingToBuild(mc, main, off)) {
            if (isLookingAtInteractiveBlock(mc)) return;

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastRightClickTime >= RIGHT_CLICK_COOLDOWN) {
                InteractionHand axeHand = findAxeHand(player);
                
                if (axeHand != null) {
                    ItemStack axeStack = player.getItemInHand(axeHand);

                    // КРИТИЧЕСКИЙ ФИКС: Не шлем пакет, если топор на перезарядке
                    if (!axeStack.isEmpty() && !player.getCooldowns().isOnCooldown(axeStack.getItem())) {
                        
                        mc.player.swing(axeHand);
                        PacketDistributor.sendToServer(
                            new ServerboundAxeThrowPacket(axeHand == InteractionHand.OFF_HAND)
                        );

                        // Уменьшаем стак на клиенте только если пакет реально ушел
                        if (!player.getAbilities().instabuild) {
                            axeStack.shrink(1);
                        }
                        
                        lastRightClickTime = currentTime;
                    }
                }
            }
        }
        // 2. ЛОГИКА БЛОКИРОВКИ МЕЧОМ (SWORD BLOCKING)
        if (canBlockWithSword(mc, main, off)) {
            if (isLookingAtInteractiveBlock(mc)) return;
            if (isPerformingEntityInteraction(mc, player)) return;
            
            if (isTryingToBuild(mc, main, off)) return;
            
            if (isSpecialItem(main) || isSpecialItem(off)) {
                if (mc.hitResult.getType() == HitResult.Type.BLOCK) return;
            }

            event.setCanceled(true);
            event.setSwingHand(false);
            if (!SwordBlockSystem.isBlocking(player)) {
                SwordBlockSystem.startBlocking(player);
            }
        }
    }

    /**
     * Логика выбора руки: исправлено чередование при блокировке двумя мечами.
     */
    private static InteractionHand getAttackHand(Minecraft mc) {
        Player player = mc.player;
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();

        if (isWeapon(main) && isWeapon(off)) {
            return isMainHandNext ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        }

        if (isWeapon(main)) return InteractionHand.MAIN_HAND;
        if (isWeapon(off)) return InteractionHand.OFF_HAND;

        return isMainHandNext ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }

    /**
     * Выполняет атаку. Решена проблема "отскока" правой руки при зажатом ПКМ.
     */
    private static boolean performAttack(Minecraft mc, InteractionHand hand) {
        if (mc.player == null) return false;
        
        float progress = (hand == InteractionHand.MAIN_HAND) 
            ? CooldownManager.getMainHandProgress(mc.player) 
            : CooldownManager.getOffHandProgress(mc.player);
            
        if (progress < 0.9F) return false;

        if (mc.hitResult instanceof EntityHitResult eHit) {
            Entity target = eHit.getEntity();
            
            if (hand == InteractionHand.OFF_HAND) {
                PacketDistributor.sendToServer(new OffhandSwapPacket(target.getId(), true));
            } else {
                if (SwordBlockSystem.isBlocking(mc.player) || mc.player.isBlocking()) {
                    PacketDistributor.sendToServer(new OffhandSwapPacket(target.getId(), false));
                } else {
                    mc.gameMode.attack(mc.player, target);
                }
            }
            
            mc.player.swing(hand);
            resetCooldown(mc.player, hand);
            return true;
        }
        
        if (mc.hitResult == null || mc.hitResult.getType() == HitResult.Type.MISS) {
            mc.player.swing(hand);
            resetCooldown(mc.player, hand);
            return true;
        }
        
        return false;
    }

    // --- ПРОВЕРКИ ПРИОРИТЕТОВ ---

    /**
     * Разрешает использование еды, блоков и зелий при усталости.
     */
    private static boolean isAllowedUtility(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        return item instanceof BlockItem || item instanceof PotionItem || stack.has(DataComponents.FOOD);
    }

    private static boolean isRangedWeapon(ItemStack stack) {
        Item i = stack.getItem();
        return i instanceof BowItem || i instanceof CrossbowItem || i instanceof TridentItem;
    }

    private static boolean isTryingToBuild(Minecraft mc, ItemStack main, ItemStack off) {
        if (!(mc.hitResult instanceof BlockHitResult)) return false;
        return main.getItem() instanceof BlockItem || off.getItem() instanceof BlockItem;
    }

    /**
     * Проверяет, является ли предмет "специальным".
     */
    private static boolean isSpecialItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();

        if (stack.has(DataComponents.FOOD)) return true;

        if (item instanceof SpawnEggItem || 
            item instanceof BucketItem || 
            item instanceof PotionItem || 
            item instanceof FishingRodItem || 
            item instanceof LeadItem ||
            item instanceof ProjectileWeaponItem) {
            return true;
        }

        return stack.getMaxStackSize() < 64 && !(item instanceof TieredItem);
    }

    private static boolean isPerformingEntityInteraction(Minecraft mc, Player player) {
        if (!(mc.hitResult instanceof EntityHitResult eHit)) return false;
        Entity target = eHit.getEntity();
        if (player.isShiftKeyDown()) return false;

        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();

        if (target instanceof TamableAnimal animal) {
            if ((main.has(DataComponents.FOOD) && animal.isFood(main)) || 
                (off.has(DataComponents.FOOD) && animal.isFood(off))) return true;
            if (animal.isOwnedBy(player)) return true;
        }
        return target instanceof AbstractHorse || target instanceof Villager;
    }

    private static boolean isLookingAtInteractiveBlock(Minecraft mc) {
        if (mc.hitResult instanceof BlockHitResult blockHit) {
            BlockPos pos = blockHit.getBlockPos();
            BlockState state = mc.level.getBlockState(pos);
            if (state.getMenuProvider(mc.level, pos) != null) return true;
            return state.is(BlockTags.BUTTONS) || state.is(BlockTags.DOORS) ||
                   state.is(BlockTags.FENCE_GATES) || state.is(BlockTags.TRAPDOORS);
        }
        return false;
    }

    private static boolean isBreakingBlock(Minecraft mc) {
        if (mc.hitResult instanceof BlockHitResult bHit) {
            return mc.player.isCreative() && !mc.level.getBlockState(bHit.getBlockPos()).isAir();
        }
        return false;
    }

    // --- УТИЛИТЫ ---

    private static boolean isWeapon(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item i = stack.getItem();
        return i instanceof SwordItem || i instanceof AxeItem || i instanceof TridentItem || i instanceof MaceItem;
    }

    private static void resetCooldown(Player player, InteractionHand hand) {
        if (hand == InteractionHand.OFF_HAND) CooldownManager.setOffHandCooldown(player);
        else CooldownManager.setMainHandCooldown(player);
    }

    private static void updateNextHand(Minecraft mc) {
        ItemStack main = mc.player.getMainHandItem();
        ItemStack off = mc.player.getOffhandItem();
        if (isWeapon(main) == isWeapon(off)) {
            isMainHandNext = !isMainHandNext;
        } else {
            isMainHandNext = true;
        }
    }

    private static boolean canBlockWithSword(Minecraft mc, ItemStack main, ItemStack off) {
        boolean hasSword = main.getItem() instanceof SwordItem || off.getItem() instanceof SwordItem;
        boolean hasShield = main.getItem() instanceof ShieldItem || off.getItem() instanceof ShieldItem;
        
        if (!hasSword || hasShield) return false;

        if (off.getItem() instanceof BlockItem || isSpecialItem(off) || isSpecialItem(main)) {
            return mc.hitResult == null || 
                   mc.hitResult.getType() == HitResult.Type.MISS || 
                   mc.hitResult.getType() == HitResult.Type.ENTITY;
        }

        return true;
    }

    private static InteractionHand findAxeHand(Player player) {
        if (player.getMainHandItem().getItem() instanceof AxeItem) return InteractionHand.MAIN_HAND;
        if (player.getOffhandItem().getItem() instanceof AxeItem) return InteractionHand.OFF_HAND;
        return null;
    }

    private static boolean hasAxe(Player player) {
        return findAxeHand(player) != null;
    }
}