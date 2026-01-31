package net.darkunity.neweracombat;

import net.neoforged.neoforge.common.ModConfigSpec;

public class NewEraCombatConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLE_DUAL_WIELD;
    public static final ModConfigSpec.BooleanValue ENABLE_SHIELD_ATTACK;
    public static final ModConfigSpec.BooleanValue SHOW_COOLDOWN;
    
    public static final ModConfigSpec.DoubleValue MAIN_HAND_COOLDOWN;
    public static final ModConfigSpec.DoubleValue OFF_HAND_COOLDOWN;
    public static final ModConfigSpec.DoubleValue SHIELD_BASH_COOLDOWN;

    static {
        BUILDER.push("General Settings");

        ENABLE_DUAL_WIELD = BUILDER
                .comment("Включить механику чередования рук при наличии оружия в обеих руках")
                .define("enableDualWield", true);

        ENABLE_SHIELD_ATTACK = BUILDER
                .comment("Включить возможность атаковать щитом на ПКМ")
                .define("enableShieldAttack", true);

        SHOW_COOLDOWN = BUILDER
                .comment("Показывать индикатор кулдауна левой руки под перекрестием")
                .define("showCooldown", true);

        BUILDER.pop(); // Закрываем General Settings

        BUILDER.push("Cooldown Settings");

        MAIN_HAND_COOLDOWN = BUILDER
                .comment("Кулдаун основной руки (в секундах)")
                .defineInRange("mainHandCooldown", 0.6, 0.0, 5.0);

        OFF_HAND_COOLDOWN = BUILDER
                .comment("Кулдаун левой руки (в секундах)")
                .defineInRange("offHandCooldown", 0.8, 0.0, 5.0);

        SHIELD_BASH_COOLDOWN = BUILDER
                .comment("Кулдаун удара щитом (в секундах)")
                .defineInRange("shieldBashCooldown", 1.0, 0.0, 10.0);

        BUILDER.pop(); // Закрываем Cooldown Settings

        SPEC = BUILDER.build();
    }
}