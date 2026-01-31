package net.darkunity.neweracombat;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

@EventBusSubscriber(modid = "neweracombat", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
class ModKeyRegistration {
    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(ModKeyBindings.SIT_KEY);
        event.register(ModKeyBindings.CRAWL_KEY);
        event.register(ModKeyBindings.DASH_KEY);
    }
}

@EventBusSubscriber(modid = "neweracombat", value = Dist.CLIENT)
public class ModKeyBindings {
    public static final KeyMapping SIT_KEY = new KeyMapping("key.neweracombat.sit", GLFW.GLFW_KEY_C, "key.categories.neweracombat");
    public static final KeyMapping CRAWL_KEY = new KeyMapping("key.neweracombat.crawl", GLFW.GLFW_KEY_Z, "key.categories.neweracombat");
    public static final KeyMapping DASH_KEY = new KeyMapping("key.neweracombat.dash", GLFW.GLFW_KEY_R, "key.categories.neweracombat");
    
    private static boolean lastSitState = false;
    private static boolean lastCrawlState = false;

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        var player = mc.player;

        // КРИТИЧЕСКИЙ ФИКС: Спектатор и Креатив в полете игнорируют всё
        if (player.isSpectator() || player.getAbilities().flying) {
            lastSitState = false;
            lastCrawlState = false;
            return;
        }

        if (player.isFallFlying()) return;

        var input = event.getInput();
        if (lastSitState || lastCrawlState) {
            input.shiftKeyDown = false;
        }
        
        if (lastSitState) {
            input.forwardImpulse = 0;
            input.leftImpulse = 0;
            input.up = false;
            input.down = false;
            input.left = false;
            input.right = false;
            input.jumping = false;
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        UUID playerUUID = mc.player.getUUID();

        // Форсированный сброс поз при смене режима
        if (mc.player.isSpectator() || mc.player.getAbilities().flying) {
            if (lastSitState || lastCrawlState) {
                PoseHandler.SITTING_PLAYERS.remove(playerUUID);
                PoseHandler.CRAWLING_PLAYERS.remove(playerUUID);
                PacketDistributor.sendToServer(new ServerboundSitPacket(false));
                lastSitState = false;
                lastCrawlState = false;
            }
            return;
        }

        boolean sitPressed = SIT_KEY.isDown();
        boolean crawlPressed = CRAWL_KEY.isDown();

        boolean targetSit = (sitPressed && !lastCrawlState);
        boolean targetCrawl = (crawlPressed && !lastSitState);

        if (targetSit != lastSitState) {
            if (targetSit) PoseHandler.SITTING_PLAYERS.add(playerUUID);
            else PoseHandler.SITTING_PLAYERS.remove(playerUUID);
            mc.player.refreshDimensions(); 
            PacketDistributor.sendToServer(new ServerboundSitPacket(targetSit));
            lastSitState = targetSit;
        }

        if (targetCrawl != lastCrawlState) {
            if (targetCrawl) PoseHandler.CRAWLING_PLAYERS.add(playerUUID);
            else {
                PoseHandler.CRAWLING_PLAYERS.remove(playerUUID);
                FatigueSystem.SLIDE_TICKS.remove(playerUUID);
            }
            lastCrawlState = targetCrawl;
        }
    }
}