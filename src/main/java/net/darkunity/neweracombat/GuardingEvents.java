package net.darkunity.neweracombat;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MaceItem;
import net.minecraft.tags.ItemTags;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = "neweracombat")
public class GuardingEvents {

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Pre event) {
        // Исправление булавы для левой руки
        if (event.getSource().getDirectEntity() instanceof Player attacker) {
            ItemStack offHand = attacker.getOffhandItem();
            
            // Проверяем булаву в левой руке
            if (offHand.getItem() instanceof MaceItem mace && attacker.fallDistance > 1.5F) {
                // Вызываем метод через экземпляр предмета mace
                float fallBonus = mace.getAttackDamageBonus(attacker, event.getOriginalDamage(), event.getSource());
                if (fallBonus > 0) {
                    event.setNewDamage(event.getOriginalDamage() + fallBonus);
                    
                    // Визуальные эффекты удара булавой
                    if (attacker.level() instanceof ServerLevel sLevel) {
                        sLevel.sendParticles(ParticleTypes.EXPLOSION, 
                            event.getEntity().getX(), 
                            event.getEntity().getY(), 
                            event.getEntity().getZ(), 
                            1, 0, 0, 0, 0);
                    }
                    // Не обнуляем fallDistance здесь, чтобы не ломать ванильную логику приземления
                }
            }
        }

        // Блок мечом
        if (event.getEntity() instanceof Player player && player.isUsingItem()) {
            if (player.getUseItem().is(ItemTags.SWORDS)) {
                event.setNewDamage(event.getOriginalDamage() * 0.3f);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), 
                    SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0f, 1.0f);
            }
        }
    }
}