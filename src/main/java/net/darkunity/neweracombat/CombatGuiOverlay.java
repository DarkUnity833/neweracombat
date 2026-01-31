package net.darkunity.neweracombat;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;

@EventBusSubscriber(modid = "neweracombat", value = Dist.CLIENT)
public class CombatGuiOverlay {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderGui(RenderGuiLayerEvent.Pre event) {
        String path = event.getName().getPath();
        // Убираем ваниль, чтобы не мешала нашему оверлею
        if (path.contains("crosshair") || path.contains("attack_indicator")) {
            event.setCanceled(true);
            if (path.equals("crosshair")) {
                renderEverything(event.getGuiGraphics());
            }
        }
    }

    private static void renderEverything(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return;

        int centerX = mc.getWindow().getGuiScaledWidth() / 2;
        int centerY = mc.getWindow().getGuiScaledHeight() / 2;

        RenderSystem.enableBlend();

        // 1. ПРИЦЕЛ
        float mainProgress = CooldownManager.getMainHandProgress(mc.player);
        int crossColor = (mainProgress < 0.95f) ? 0x88FFFFFF : 0xFFFFFFFF;
        g.fill(centerX - 4, centerY, centerX + 5, centerY + 1, crossColor);
        g.fill(centerX, centerY - 4, centerX + 1, centerY + 5, crossColor);

        // 2. БОКОВЫЕ КУЛДАУНЫ (только если не смотрим на блок)
        boolean lookingAtBlock = mc.hitResult != null && mc.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK;
        if (!lookingAtBlock) {
            float offProgress = CooldownManager.getOffHandProgress(mc.player);
            if (offProgress < 0.98f) drawSideBar(g, centerX - 12, centerY - 5, offProgress, 0xFFFFFFFF);
            if (mainProgress < 0.98f) drawSideBar(g, centerX + 10, centerY - 5, mainProgress, 0xFFFFFFFF);
        }

        // 3. УСТАЛОСТЬ (ИНДИКАТОР ПОД ПРИЦЕЛОМ)
        float fatigue = FatigueSystem.getLevel();
        if (fatigue > 0.1f) {
            int width = 30;
            int x = centerX - (width / 2);
            int y = centerY + 12;

            // Задний фон полоски
            g.fill(x, y, x + width, y + 3, 0x66000000); 

            // Определение цвета: если стамина заблокирована - красный, иначе белый/серый
            int fColor;
            if (FatigueSystem.isLocked()) {
                fColor = 0xFFFF4444; // Ярко-красный (Блокировка)
            } else if (fatigue > 15.0f) {
                fColor = 0xFFFFCC00; // Желтый (Предупреждение, скоро лимит)
            } else {
                fColor = 0xFFFFFFFF; // Белый (Норма)
            }

            // Рисуем заполнение (инвертируем, так как усталость растет)
            // Показываем, сколько "станины" осталось (от 1.0 до 0.0)
            float fillFactor = fatigue / 20f;
            int fillWidth = (int)(fillFactor * width);
            
            // Рисуем полоску усталости (растет слева направо)
            g.fill(x, y + 1, x + fillWidth, y + 2, fColor);
        }
        
        RenderSystem.disableBlend();
    }

    private static void drawSideBar(GuiGraphics g, int x, int y, float progress, int color) {
        g.fill(x, y, x + 2, y + 10, 0x44000000);
        int barHeight = (int)(progress * 10);
        g.fill(x, y + (10 - barHeight), x + 2, y + 10, color);
    }
}