package net.darkunity.neweracombat.firewall;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public final class CreativeFirewall {

    private static int graceTicks = 0;
    private static boolean wasCreative = false;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        if (!(event.getEntity() instanceof LocalPlayer player)) return;
        if (Minecraft.getInstance().player != player) return;

        boolean creative = player.isCreative();

        // ВХОД В КРЕАТИВ
        if (creative && !wasCreative) {
            graceTicks = 5; // 🔑 5 тиков ваниллы
        }

        wasCreative = creative;

        if (!creative) return;

        // === GRACE PERIOD ===
        if (graceTicks > 0) {
            graceTicks--;

            // НИЧЕГО не трогаем, но гарантируем полёт
            player.getAbilities().mayfly = true;

            // НЕ ставим flying = true — это решает ванилла
            return;
        }
    }

    /**
     * Используется другими системами
     * чтобы понять: можно ли трогать движение
     */
    public static boolean allowCustomMovement() {
        return graceTicks <= 0;
    }
}
