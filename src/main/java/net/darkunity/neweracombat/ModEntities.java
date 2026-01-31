package net.darkunity.neweracombat;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = 
            DeferredRegister.create(Registries.ENTITY_TYPE, "neweracombat");

    public static final DeferredHolder<EntityType<?>, EntityType<ThrownAxeEntity>> THROWN_AXE = 
            ENTITIES.register("thrown_axe", () -> EntityType.Builder.<ThrownAxeEntity>of(ThrownAxeEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(64)
                    .updateInterval(20)
                    .build("thrown_axe"));
}