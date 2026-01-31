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
public class SwordAnimationDebug {
    
    public static final boolean ENABLED = false; // Выключи (false) перед релизом

    // Начальные значения (те, что были последними удачными)
    public static float dX = -1.240F, dY = -0.070F, dZ = 0.700F; // Твои значения для Dual
public static float sX = -1.240F, sY = -0.070F, sZ = 0.700F; // Твои значения для Single
    private static int selectedParam = 0; // 0=X, 1=Y, 2=Z
    private static boolean isSingleMode = false;

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (!ENABLED) return;
        if (event.getAction() != GLFW.GLFW_PRESS && event.getAction() != GLFW.GLFW_REPEAT) return;

        // Определяем шаг: с шифтом быстрее
        long window = Minecraft.getInstance().getWindow().getWindow();
        boolean shifting = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS 
                        || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        
        float step = shifting ? 0.1F : 0.01F;

        switch (event.getKey()) {
            case GLFW.GLFW_KEY_UP -> changeValue(step);
            case GLFW.GLFW_KEY_DOWN -> changeValue(-step);
            case GLFW.GLFW_KEY_CAPS_LOCK -> isSingleMode = !isSingleMode;
            case GLFW.GLFW_KEY_TAB -> selectedParam = (selectedParam + 1) % 3;
            case GLFW.GLFW_KEY_P -> {
                System.out.println("\n=== SWORD DEBUG SETTINGS ===");
                System.out.println(String.format("Dual: x=%.3fF, y=%.3fF, z=%.3fF", dX, dY, dZ));
                System.out.println(String.format("Single: x=%.3fF, y=%.3fF, z=%.3fF", sX, sY, sZ));
            }
        }
    }

    private static void changeValue(float amount) {
        if (isSingleMode) {
            if (selectedParam == 0) sX += amount;
            else if (selectedParam == 1) sY += amount;
            else if (selectedParam == 2) sZ += amount;
        } else {
            if (selectedParam == 0) dX += amount;
            else if (selectedParam == 1) dY += amount;
            else if (selectedParam == 2) dZ += amount;
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!ENABLED) return;
        
        GuiGraphics g = event.getGuiGraphics();
        String mode = isSingleMode ? "[SINGLE BLOCK]" : "[DUAL BLOCK]";
        String param = (selectedParam == 0 ? "X (Высота)" : (selectedParam == 1 ? "Y (Стороны)" : "Z (Наклон)"));
        
        int color = 0x00FF00; // Зеленый для активного дебага
        g.drawString(Minecraft.getInstance().font, "DEBUG SWORD: " + mode, 10, 10, color);
        g.drawString(Minecraft.getInstance().font, "Параметр: " + param, 10, 20, 0xFFFFFF);
        
        String valText = isSingleMode 
            ? String.format("X: %.3f | Y: %.3f | Z: %.3f", sX, sY, sZ)
            : String.format("X: %.3f | Y: %.3f | Z: %.3f", dX, dY, dZ);
            
        g.drawString(Minecraft.getInstance().font, valText, 10, 30, 0xFFFF00);
        g.drawString(Minecraft.getInstance().font, "UP/DOWN: Изменить | Shift: Быстрее | Caps: Режим | Tab: Ось", 10, 45, 0xAAAAAA);
    }
}