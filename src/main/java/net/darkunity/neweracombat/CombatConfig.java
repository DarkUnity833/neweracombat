package net.darkunity.neweracombat;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.EventBusSubscriber;
import org.apache.commons.lang3.tuple.Pair;

@EventBusSubscriber(modid = "neweracombat")
public class CombatConfig {
    public static ModConfigSpec SPEC;
    public static Config CONFIG;
    
    private static volatile boolean configLoaded = false;
    
    static {
        final Pair<Config, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(Config::new);
        SPEC = specPair.getRight();
        CONFIG = specPair.getLeft();
    }
    
    public static void registerConfig(IEventBus modEventBus) {
        if (configLoaded) return;
        
        modEventBus.addListener(CombatConfig::onConfigLoad);
        modEventBus.addListener(CombatConfig::onConfigReload);
        
        configLoaded = true;
    }
    
    public static void register(ModContainer modContainer) {
        if (configLoaded) return;
        
        try {
            modContainer.registerConfig(ModConfig.Type.CLIENT, SPEC, "neweracombat-client.toml");
            configLoaded = true;
        } catch (Exception e) {
            // Тихий fail
        }
    }
    
    @SubscribeEvent
    public static void onConfigLoad(final ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) {
            configLoaded = true;
        }
    }
    
    @SubscribeEvent
    public static void onConfigReload(final ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SPEC) {
            configLoaded = true;
        }
    }
    
    public static boolean isConfigLoaded() {
        return configLoaded;
    }
    
    public static boolean enableCooldowns() {
        if (!configLoaded) return true;
        try {
            return CONFIG.enableCooldowns.get();
        } catch (Exception e) {
            return true;
        }
    }

    public static boolean showCooldownBars() {
        if (!configLoaded) return true;
        try {
            return CONFIG.showCooldownBars.get();
        } catch (Exception e) {
            return true;
        }
    }
    
    public static boolean alternateHandAttacks() {
        if (!configLoaded) return true;
        try {
            return CONFIG.alternateHandAttacks.get();
        } catch (Exception e) {
            return true;
        }
    }
    
    public static float getMainHandCooldown() {
        if (!configLoaded) return 0.6f;
        try {
            return CONFIG.mainHandCooldown.get().floatValue();
        } catch (Exception e) {
            return 0.6f;
        }
    }
    
    public static float getOffHandCooldown() {
        if (!configLoaded) return 0.8f;
        try {
            return CONFIG.offHandCooldown.get().floatValue();
        } catch (Exception e) {
            return 0.8f;
        }
    }
    
    public static float getShieldBashCooldown() {
        if (!configLoaded) return 1.0f;
        try {
            return CONFIG.shieldBashCooldown.get().floatValue();
        } catch (Exception e) {
            return 1.0f;
        }
    }
    
    public static int getBarOffset() {
        if (!configLoaded) return 18;
        try {
            return CONFIG.barOffset.get();
        } catch (Exception e) {
            return 18;
        }
    }
    
    public static int getBarWidth() {
        if (!configLoaded) return 4;
        try {
            return CONFIG.barWidth.get();
        } catch (Exception e) {
            return 4;
        }
    }
    
    public static int getBarHeight() {
        if (!configLoaded) return 20;
        try {
            return CONFIG.barHeight.get();
        } catch (Exception e) {
            return 20;
        }
    }
    
    public static boolean showCooldownText() {
        if (!configLoaded) return true;
        try {
            return CONFIG.showCooldownText.get();
        } catch (Exception e) {
            return true;
        }
    }
    
    public static void save() {
        if (configLoaded) {
            SPEC.save();
        }
    }
    
    public static class Config {
        public final ModConfigSpec.BooleanValue enableCooldowns;
        public final ModConfigSpec.BooleanValue showCooldownBars;
        public final ModConfigSpec.BooleanValue alternateHandAttacks;
        public final ModConfigSpec.DoubleValue mainHandCooldown;
        public final ModConfigSpec.DoubleValue offHandCooldown;
        public final ModConfigSpec.DoubleValue shieldBashCooldown;
        public final ModConfigSpec.IntValue barOffset;
        public final ModConfigSpec.IntValue barWidth;
        public final ModConfigSpec.IntValue barHeight;
        public final ModConfigSpec.BooleanValue showCooldownText;
        
        public Config(ModConfigSpec.Builder builder) {
            builder.comment("New Era Combat Configuration")
                   .push("general");
            
            enableCooldowns = builder
                .comment("Enable or disable weapon cooldowns")
                .translation("config.neweracombat.enableCooldowns")
                .define("enableCooldowns", true);
            
            showCooldownBars = builder
                .comment("Show cooldown bars next to crosshair")
                .translation("config.neweracombat.showCooldownBars")
                .define("showCooldownBars", true);
            
            alternateHandAttacks = builder
                .comment("Alternate attacks between left and right hand when both have weapons")
                .translation("config.neweracombat.alternateHandAttacks")
                .define("alternateHandAttacks", true);
            
            builder.pop();
            
            builder.push("cooldowns")
                .comment("Cooldown duration settings (in seconds)");
            
            mainHandCooldown = builder
                .comment("Right hand attack cooldown (seconds)")
                .translation("config.neweracombat.mainHandCooldown")
                .defineInRange("mainHandCooldown", 0.6, 0.1, 5.0);
            
            offHandCooldown = builder
                .comment("Left hand attack cooldown (seconds)")
                .translation("config.neweracombat.offHandCooldown")
                .defineInRange("offHandCooldown", 0.8, 0.1, 5.0);
            
            shieldBashCooldown = builder
                .comment("Shield bash cooldown (seconds)")
                .translation("config.neweracombat.shieldBashCooldown")
                .defineInRange("shieldBashCooldown", 1.0, 0.1, 5.0);
            
            builder.pop();
            
            builder.push("visual")
                .comment("Visual settings for cooldown bars");
            
            barOffset = builder
                .comment("Distance from crosshair to bars (pixels)")
                .translation("config.neweracombat.barOffset")
                .defineInRange("barOffset", 18, 10, 50);
            
            barWidth = builder
                .comment("Width of cooldown bars (pixels)")
                .translation("config.neweracombat.barWidth")
                .defineInRange("barWidth", 4, 2, 10);
            
            barHeight = builder
                .comment("Height of cooldown bars (pixels)")
                .translation("config.neweracombat.barHeight")
                .defineInRange("barHeight", 20, 10, 40);
            
            showCooldownText = builder
                .comment("Show cooldown text above bars")
                .translation("config.neweracombat.showCooldownText")
                .define("showCooldownText", true);
            
            builder.pop();
        }
    }
}