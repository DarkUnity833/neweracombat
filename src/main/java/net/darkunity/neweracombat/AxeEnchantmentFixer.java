package net.darkunity.neweracombat;

import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.core.RegistryAccess;

import java.util.Map;

@EventBusSubscriber(modid = "neweracombat")
public class AxeEnchantmentFixer {

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();

        if (left.getItem() instanceof AxeItem && right.is(Items.ENCHANTED_BOOK)) {
            if (event.getPlayer() == null) return;
            
            RegistryAccess reg = event.getPlayer().level().registryAccess();
            var enchantLookup = reg.lookupOrThrow(Registries.ENCHANTMENT);
            
            // Список зачарований для переноса
            Holder<Enchantment> loyalty = enchantLookup.getOrThrow(Enchantments.LOYALTY);
            Holder<Enchantment> channeling = enchantLookup.getOrThrow(Enchantments.CHANNELING);
            Holder<Enchantment> riptide = enchantLookup.getOrThrow(Enchantments.RIPTIDE);

            int loyaltyLvl = EnchantmentHelper.getItemEnchantmentLevel(loyalty, right);
            int channelingLvl = EnchantmentHelper.getItemEnchantmentLevel(channeling, right);
            int riptideLvl = EnchantmentHelper.getItemEnchantmentLevel(riptide, right);

            if (loyaltyLvl > 0 || channelingLvl > 0 || riptideLvl > 0) {
                ItemStack result = left.copy();
                int totalCost = 0;

                if (loyaltyLvl > 0) {
                    result.enchant(loyalty, loyaltyLvl);
                    totalCost += 5 * loyaltyLvl;
                }
                if (channelingLvl > 0) {
                    result.enchant(channeling, channelingLvl);
                    totalCost += 10;
                }
                if (riptideLvl > 0) {
                    // Тягун и Верность/Громовержец несовместимы в ванилле
                    if (loyaltyLvl == 0 && channelingLvl == 0) {
                        result.enchant(riptide, riptideLvl);
                        totalCost += 6 * riptideLvl;
                    }
                }

                if (totalCost > 0) {
                    event.setOutput(result);
                    event.setCost(totalCost);
                    event.setMaterialCost(1);
                }
            }
        }
    }
}