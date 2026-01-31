package net.darkunity.neweracombat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class DebugBomb {
    public static final Logger LOG = LogManager.getLogger("NEC-DEBUG");

    public static void spawnProbe(ServerLevel level, double x, double y, double z) {
        try {
            // Обязательно .get()
            EntityType<?> type = ModEntities.THROWN_AXE.get();
            Entity e = type.create(level);
            if (e != null) {
                e.setPos(x, y, z);
                level.addFreshEntity(e);
            }
        } catch (Exception e) {
            LOG.error("Failed to spawn probe", e);
        }
    }
}