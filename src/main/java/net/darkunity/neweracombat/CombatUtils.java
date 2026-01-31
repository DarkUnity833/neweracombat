package net.darkunity.neweracombat;

import net.minecraft.world.entity.LivingEntity;
import java.lang.reflect.Field;

public class CombatUtils {
    private static Field lastHurtByPlayerTimeField;

    static {
        try {
            // Ищем поле lastHurtByPlayerTime. В среде разработки оно называется так, 
            // но в билде может быть обфусцировано (NeoForge обычно маппит их автоматически)
            lastHurtByPlayerTimeField = LivingEntity.class.getDeclaredField("lastHurtByPlayerTime");
            lastHurtByPlayerTimeField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            // Резервный вариант для некоторых версий маппингов
            try {
                lastHurtByPlayerTimeField = LivingEntity.class.getDeclaredField("f_20914_");
                lastHurtByPlayerTimeField.setAccessible(true);
            } catch (Exception ignored) {}
        }
    }

    public static void forceResetHurtResistance(LivingEntity entity) {
        // 1. Стандартные доступные поля
        entity.invulnerableTime = 0;
        entity.hurtDuration = 0;
        entity.hurtTime = 0;
        
        // 2. Рефлексия для защищенного поля
        if (lastHurtByPlayerTimeField != null) {
            try {
                lastHurtByPlayerTimeField.set(entity, 0);
            } catch (IllegalAccessException ignored) {}
        }
    }
}