package net.darkunity.neweracombat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = "neweracombat", value = Dist.CLIENT)
public class DualWieldDebug {
    
    public static final boolean DEBUG_MODE = false;// ПОСТАВЬ true, ЧТОБЫ ВКЛЮЧИТЬ РЕДАКТИРОВАНИЕ В ИГРЕ для доработки и корректировки

    public static float x = 0.685F, y = -0.630F, z = -0.870F;
    public static float xp = 77.0F, yp = -13.5F, zp = -56.0F;
    
    private static int selectedParam = 0; 

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (!DEBUG_MODE) return; // Игнорируем ввод, если дебаг выключен
        
        if (event.getAction() != GLFW.GLFW_PRESS && event.getAction() != GLFW.GLFW_REPEAT) return;

        float step = 0.005F;
        float angleStep = 0.5F;

        switch (event.getKey()) {
            case GLFW.GLFW_KEY_UP -> changeValue(1, (selectedParam > 2) ? angleStep : step);
            case GLFW.GLFW_KEY_DOWN -> changeValue(-1, (selectedParam > 2) ? -angleStep : -step);
            case GLFW.GLFW_KEY_TAB -> selectedParam = (selectedParam + 1) % 6;
            case GLFW.GLFW_KEY_P -> System.out.println(String.format("pos(%.3fF, %.3fF, %.3fF) rot(%.1fF, %.1fF, %.1fF)", x, y, z, xp, yp, zp));
        }
    }

    private static void changeValue(int side, float amount) {
        if (selectedParam == 0) x += amount;
        else if (selectedParam == 1) y += amount;
        else if (selectedParam == 2) z += amount;
        else if (selectedParam == 3) xp += amount;
        else if (selectedParam == 4) yp += amount;
        else if (selectedParam == 5) zp += amount;
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!DEBUG_MODE) return; // Не рисуем оверлей, если дебаг выключен
        
        GuiGraphics g = event.getGuiGraphics();
        String[] names = {"X", "Y", "Z", "XP", "YP", "ZP"};
        g.drawString(Minecraft.getInstance().font, "DEBUG ACTIVE: " + names[selectedParam], 10, 10, 0xFF5555);
        g.drawString(Minecraft.getInstance().font, String.format("%.3f %.3f %.3f | %.1f %.1f %.1f", x, y, z, xp, yp, zp), 10, 20, 0xFFFFFF);
    }
}