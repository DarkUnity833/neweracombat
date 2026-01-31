package net.darkunity.neweracombat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.SwordItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = "neweracombat", value = Dist.CLIENT)
public class SwordBlockGui {

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        // Проверяем, что игрок в мире, GUI не скрыт и включен режим блока
        if (mc.player == null || mc.options.hideGui || !SwordBlockSystem.isBlocking(mc.player)) return;

        GuiGraphics guiGraphics = event.getGuiGraphics();
        
        // В 1.21.1 правильнее брать размеры через GuiGraphics или Window
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        
        int x = width / 2;
        int y = height / 2;

        boolean hasMain = mc.player.getMainHandItem().getItem() instanceof SwordItem;
        boolean hasOff = mc.player.getOffhandItem().getItem() instanceof SwordItem;

        String icon = "";
        int color = 0xFFFFFF; 

        if (hasMain && hasOff) {
            icon = "⚔"; 
            color = 0xFFAA00; // Золотистый/Оранжевый
        } else if (hasMain) {
            icon = "»"; // Более жирный символ для правой руки
            color = 0xAAAAAA; // Серый
        } else if (hasOff) {
            icon = "«"; // Для левой
            color = 0xAAAAAA;
        }

        if (!icon.isEmpty()) {
            // Отрисовка с небольшой тенью для читаемости на любом фоне
            // Сдвиг y + 12, чтобы не перекрывать точку прицела и индикатор атаки
            guiGraphics.drawCenteredString(mc.font, icon, x, y + 12, color);
        }
    }
}