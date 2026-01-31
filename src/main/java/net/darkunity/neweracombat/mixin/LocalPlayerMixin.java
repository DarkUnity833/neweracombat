package net.darkunity.neweracombat.mixin;

import net.darkunity.neweracombat.FatigueSystem;
import net.darkunity.neweracombat.WallRunSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.PotionItem;
import net.minecraft.core.component.DataComponents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    @Inject(method = "isShiftKeyDown", at = @At("HEAD"), cancellable = true)
    private void neweracombat$fakeShiftForCamera(CallbackInfoReturnable<Boolean> cir) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (player.isSpectator() || player.getAbilities().flying) return;

        if (WallRunSystem.isActive()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = {"tick", "m_8119_"}, at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        // ПОЛНЫЙ ИГНОР СПЕКТАТОРА И КРЕАТИВА
        if (player.isSpectator() || player.isCreative()) return;

        Minecraft mc = Minecraft.getInstance();
        if (player.isUsingItem() && FatigueSystem.isLocked()) {
            ItemStack usingItem = player.getUseItem();
            if (usingItem.getItem() instanceof TridentItem || usingItem.getItem() instanceof BowItem) {
                player.stopUsingItem();
                mc.options.keyUse.setDown(false);
            } else {
                if (!usingItem.has(DataComponents.FOOD) && !(usingItem.getItem() instanceof PotionItem)) {
                    player.stopUsingItem();
                    mc.options.keyUse.setDown(false);
                }
            }
        }
    }
}