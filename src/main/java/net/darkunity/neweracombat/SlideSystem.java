package net.darkunity.neweracombat;

import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.HashMap;
import java.util.UUID;

@EventBusSubscriber(modid = "neweracombat", value = Dist.CLIENT)
public class SlideSystem {
    private static final HashMap<UUID, Integer> SLIDE_TICKS = new HashMap<>();
    private static final HashMap<UUID, Vec3> SLIDE_DIR = new HashMap<>();
    
    private static final double SLIDE_RESISTANCE = 0.92; 
    private static final double EXIT_FRICTION = 0.35;
    private static final double INITIAL_BOOST = 0.38;
    private static final double MIN_SLIDE_THRESHOLD = 0.15;

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof net.minecraft.world.entity.player.Player player)) return;
        if (!player.level().isClientSide) return;

        UUID uuid = player.getUUID();

        if (player.getPose() == Pose.SWIMMING && !player.isInWater()) {
            Vec3 currentMov = player.getDeltaMovement();
            double horizontalSpeed = currentMov.horizontalDistance();

            if (!SLIDE_DIR.containsKey(uuid)) {
                if (player.isSprinting() || horizontalSpeed > MIN_SLIDE_THRESHOLD) {
                    SLIDE_TICKS.put(uuid, 22);
                    Vec3 look = player.getLookAngle();
                    Vec3 boostVec = new Vec3(look.x, 0, look.z).normalize().scale(horizontalSpeed + INITIAL_BOOST);
                    SLIDE_DIR.put(uuid, boostVec);
                    
                    player.level().playSound(player, player.getX(), player.getY(), player.getZ(), 
                        SoundEvents.PLAYER_SMALL_FALL, SoundSource.PLAYERS, 0.7F, 1.1F);
                }
            }
            
            if (SLIDE_DIR.containsKey(uuid)) {
                double slideSpeed = SLIDE_DIR.get(uuid).horizontalDistance();
                if (horizontalSpeed > slideSpeed + 0.1) {
                    SLIDE_DIR.put(uuid, new Vec3(currentMov.x, 0, currentMov.z).normalize().scale(horizontalSpeed));
                    SLIDE_TICKS.put(uuid, 18); 
                }
            }

            int ticks = SLIDE_TICKS.getOrDefault(uuid, 0);
            Vec3 slideVec = SLIDE_DIR.get(uuid);

            if (ticks > 0 && slideVec != null) {
                player.setDeltaMovement(slideVec.x, player.getDeltaMovement().y, slideVec.z);
                SLIDE_DIR.put(uuid, slideVec.scale(SLIDE_RESISTANCE));
                player.hasImpulse = true;

                // Дым во время движения (облачка)
                if (ticks % 2 == 0) {
                    spawnSmoke(player, 1, 0.02);
                }
                
                SLIDE_TICKS.put(uuid, ticks - 1);
            } else if (SLIDE_DIR.containsKey(uuid)) {
                applyBrake(player, uuid);
            }
        } else {
            if (SLIDE_DIR.containsKey(uuid)) {
                applyBrake(player, uuid);
            }
        }
    }

    private static void applyBrake(net.minecraft.world.entity.player.Player player, UUID uuid) {
        // Густое облако дыма при торможении
        spawnSmoke(player, 8, 0.1); 
        
        Vec3 lastVel = player.getDeltaMovement();
        player.setDeltaMovement(lastVel.x * EXIT_FRICTION, lastVel.y, lastVel.z * EXIT_FRICTION);
        
        SLIDE_TICKS.remove(uuid);
        SLIDE_DIR.remove(uuid);
        
        player.level().playSound(player, player.getX(), player.getY(), player.getZ(), 
            SoundEvents.CANDLE_EXTINGUISH, SoundSource.PLAYERS, 0.8F, 0.5F);
    }

    private static void spawnSmoke(net.minecraft.world.entity.player.Player player, int count, double speed) {
        for (int i = 0; i < count; i++) {
            // Используем CLOUD или SMOKE — их видно всегда
            player.level().addParticle(ParticleTypes.CLOUD, 
                player.getX() + (player.getRandom().nextDouble() - 0.5) * 0.4, 
                player.getY() + 0.1, // Чуть выше пола
                player.getZ() + (player.getRandom().nextDouble() - 0.5) * 0.4, 
                (player.getRandom().nextDouble() - 0.5) * speed, 
                0.01, 
                (player.getRandom().nextDouble() - 0.5) * speed);
        }
    }
}