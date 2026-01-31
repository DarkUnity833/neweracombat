package net.darkunity.neweracombat.mixin;

import net.darkunity.neweracombat.FatigueSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Shadow(remap = false) @Nullable public LocalPlayer player;

    @Inject(method = {"startUseItem", "m_91320_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void onStartUseItem(CallbackInfo ci) {
        if (player != null && FatigueSystem.isLocked()) {
            ItemStack main = player.getMainHandItem();
            ItemStack off = player.getOffhandItem();

            if (!isAllowed(main) && !isAllowed(off)) {
                ci.cancel();
            }
        }
    }

    private boolean isAllowed(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.getItem() instanceof BlockItem || 
               stack.getItem() instanceof PotionItem || 
               stack.has(DataComponents.FOOD);
    }
}