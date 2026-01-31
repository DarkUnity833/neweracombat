package net.darkunity.neweracombat;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.core.registries.Registries;

/**
 * Пакет обработки броска топора на сервере.
 * Учитывает все зачарования: Острота (через атрибуты), Тягун, Громовержец, Верность.
 * ФИКС: Тягун теперь корректно отрывает от земли и синхронизирует движение.
 * ДОБАВЛЕНО: Защита от фантомных бросков при кулдауне.
 */
public record ServerboundAxeThrowPacket(boolean offhand) implements CustomPacketPayload {
    public static final Type<ServerboundAxeThrowPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("neweracombat", "axe_throw"));
    public static final StreamCodec<FriendlyByteBuf, ServerboundAxeThrowPacket> CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, ServerboundAxeThrowPacket::offhand, ServerboundAxeThrowPacket::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(ServerboundAxeThrowPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            
            InteractionHand hand = msg.offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            ItemStack stack = player.getItemInHand(hand);
            
            // --- КРИТИЧЕСКИЙ ФИКС ФАНТОМНОСТИ ---
            // Проверяем: не пуста ли рука, топор ли это, и не на откате ли он.
            if (stack.isEmpty() || !(stack.getItem() instanceof AxeItem) || player.getCooldowns().isOnCooldown(stack.getItem())) {
                return; 
            }
            
            // 1. ПОЛУЧЕНИЕ УРОВНЕЙ ЗАЧАРОВАНИЙ (NeoForge 1.21.1 Registry Way)
            var enchantmentRegistry = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            
            int riptideLevel = EnchantmentHelper.getItemEnchantmentLevel(enchantmentRegistry.getOrThrow(Enchantments.RIPTIDE), stack);
            int loyaltyLevel = EnchantmentHelper.getItemEnchantmentLevel(enchantmentRegistry.getOrThrow(Enchantments.LOYALTY), stack);
            int channelingLevel = EnchantmentHelper.getItemEnchantmentLevel(enchantmentRegistry.getOrThrow(Enchantments.CHANNELING), stack);

            // 2. ЛОГИКА ТЯГУНА (RIPTIDE)
            if (riptideLevel > 0 && player.isInWaterOrRain()) {
                float yaw = player.getYRot();
                float pitch = player.getXRot();
                
                // Математика вектора направления
                float f2 = -Mth.sin(yaw * ((float)Math.PI / 180F)) * Mth.cos(pitch * ((float)Math.PI / 180F));
                float f3 = -Mth.sin(pitch * ((float)Math.PI / 180F));
                float f4 = Mth.cos(yaw * ((float)Math.PI / 180F)) * Mth.cos(pitch * ((float)Math.PI / 180F));
                float f5 = Mth.sqrt(f2 * f2 + f3 * f3 + f4 * f4);
                float forceMultiplier = 3.0F * ((1.0F + (float)riptideLevel) / 4.0F);
                
                // Установка вектора движения
                double mX = (double)f2 * (double)forceMultiplier / (double)f5;
                double mY = (double)f3 * (double)forceMultiplier / (double)f5;
                double mZ = (double)f4 * (double)forceMultiplier / (double)f5;

                // ФИКС ПРИЖАТИЯ: Если игрок на земле, принудительно подбрасываем вверх (минимум 0.5)
                if (player.onGround()) {
                    mY = Math.max(mY, 0.5D);
                }

                // Принудительная синхронизация движения сервер -> клиент
                player.setDeltaMovement(mX, mY, mZ);
                player.hurtMarked = true; 
                
                // Запуск анимации кручения (NeoForge/Vanilla 1.21.1)
                player.startAutoSpinAttack(20, 10.0F, stack); 
                
                // Звуковой эффект Тягуна
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), 
                    SoundEvents.TRIDENT_RIPTIDE_3.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
                
                // Прочность
                if (!player.getAbilities().instabuild) {
                    stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
                }
            } 
            // 3. ЛОГИКА ОБЫЧНОГО БРОСКА
            else {
                // Учитываем Остроту и базовый урон через атрибуты игрока
                float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
                
                ThrownAxeEntity axe = new ThrownAxeEntity(player.level(), player, stack, damage);
                axe.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
                
                // Скорость 1.6F (баллистика 16 блоков)
                axe.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.6F, 1.0F);
                
                // Устанавливаем кулдаун (60 тиков = 3 сек)
                player.getCooldowns().addCooldown(stack.getItem(), 60);
                
                player.level().addFreshEntity(axe);
                
                // Звук броска
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), 
                    SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
                
                // Если нет Верности - предмет тратится (уменьшаем стак)
                if (!player.getAbilities().instabuild && loyaltyLevel <= 0) {
                    stack.shrink(1);
                }
            }
        });
    }
}