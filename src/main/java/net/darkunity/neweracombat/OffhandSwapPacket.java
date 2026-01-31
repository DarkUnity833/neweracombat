package net.darkunity.neweracombat;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.util.Mth;
import java.util.List;

public record OffhandSwapPacket(int targetId, boolean isOffhand) implements CustomPacketPayload {
    
    // Сменили ID на _fixed для чистоты регистрации
    public static final Type<OffhandSwapPacket> TYPE = 
        new Type<>(ResourceLocation.fromNamespaceAndPath(NeweracombatMod.MODID, "offhand_swap_fixed"));

    public static final StreamCodec<FriendlyByteBuf, OffhandSwapPacket> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, OffhandSwapPacket::targetId,
        ByteBufCodecs.BOOL, OffhandSwapPacket::isOffhand,
        OffhandSwapPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(final OffhandSwapPacket data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            ServerLevel world = player.serverLevel();
            Entity target = world.getEntity(data.targetId());
            if (!(target instanceof LivingEntity livingTarget) || !target.isAlive()) return;

            if (player.distanceToSqr(target) > 36.0D) return;

            InteractionHand hand = data.isOffhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            ItemStack stack = player.getItemInHand(hand);
            if (stack.isEmpty()) return;

            // ВСЯ ТВОЯ ЛОГИКА НИЖЕ СОХРАНЕНА БЕЗ ИЗМЕНЕНИЙ
            livingTarget.invulnerableTime = 0;
            livingTarget.hurtDuration = 0;

            if (stack.getItem() instanceof ShieldItem) {
                applyShieldHit(player, livingTarget, world, hand);
                return;
            }

            if (stack.getItem() instanceof AxeItem && livingTarget.isBlocking()) {
                if (livingTarget instanceof Player victimPlayer) {
                    victimPlayer.getCooldowns().addCooldown(Items.SHIELD, 100);
                    livingTarget.stopUsingItem();
                    world.broadcastEntityEvent(livingTarget, (byte)30);
                    world.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F);
                }
            }

            double baseDamage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
            double itemDamage = getStackDamage(stack);
            float totalDamage = (float) (baseDamage + itemDamage);

            boolean isCrit = player.fallDistance > 0.0F && !player.onGround() && !player.isInWater() && !player.onClimbable();
            if (isCrit) totalDamage *= 1.5F;

            DamageSource source = player.damageSources().playerAttack(player);
            totalDamage = EnchantmentHelper.modifyDamage(world, stack, livingTarget, source, totalDamage);

            if (stack.getItem() instanceof SwordItem && !isCrit && player.onGround()) {
                applySweep(player, livingTarget, world, totalDamage * 0.7f);
            }

            applyMainHit(player, livingTarget, world, stack, hand, source, totalDamage, isCrit);
            
            CooldownManager.setHandCooldown(player, hand);
            player.resetAttackStrengthTicker();
        });
    }

    private static void applyMainHit(ServerPlayer player, LivingEntity target, ServerLevel world, 
                               ItemStack stack, InteractionHand hand, DamageSource source, 
                               float damage, boolean isCrit) {
        
        ItemStack mainBackup = player.getMainHandItem();
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        
        try {
            if (target.hurt(source, damage)) {
                player.swing(hand, true); 
                EnchantmentHelper.doPostAttackEffects(world, target, source);
                
                if (isCrit) {
                    world.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.5), target.getZ(), 15, 0.2, 0.2, 0.2, 0.1);
                    world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 1.0F);
                }

                target.knockback(0.4F, Mth.sin(player.getYRot() * 0.017453292F), -Mth.cos(player.getYRot() * 0.017453292F));
                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            }
        } finally {
            player.setItemInHand(InteractionHand.MAIN_HAND, mainBackup);
        }
    }

    private static void applySweep(ServerPlayer player, LivingEntity target, ServerLevel world, float sweepDamage) {
        world.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY(0.5), target.getZ(), 1, 0, 0, 0, 0);
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.0F);

        List<LivingEntity> nearby = world.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(1.0D, 0.25D, 1.0D));
        for (LivingEntity entity : nearby) {
            if (entity != player && entity != target && !player.isAlliedTo(entity)) {
                entity.hurt(player.damageSources().playerAttack(player), sweepDamage);
                entity.knockback(0.4F, Mth.sin(player.getYRot() * 0.017453292F), -Mth.cos(player.getYRot() * 0.017453292F));
            }
        }
    }

    private static void applyShieldHit(ServerPlayer player, LivingEntity target, ServerLevel world, InteractionHand hand) {
        if (target.hurt(player.damageSources().playerAttack(player), 3.0F)) {
            player.swing(hand, true);
            target.knockback(0.5F, Mth.sin(player.getYRot() * 0.017453292F), -Mth.cos(player.getYRot() * 0.017453292F));
            world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 0.8F);
        }
    }

    private static double getStackDamage(ItemStack stack) {
        ItemAttributeModifiers modifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        double d = 0;
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            if (entry.attribute().is(Attributes.ATTACK_DAMAGE)) 
                d += entry.modifier().amount();
        }
        return d;
    }
}