package net.darkunity.neweracombat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashSet;
import java.util.UUID;

@EventBusSubscriber(modid = "neweracombat")
public class PoseHandler {
    public static final HashSet<UUID> CRAWLING_PLAYERS = new HashSet<>();
    public static final HashSet<UUID> SITTING_PLAYERS = new HashSet<>();
    
    private static final long COOLDOWN_TIME = 1500;
    private static final java.util.HashMap<UUID, Long> SLIDE_COOLDOWN = new java.util.HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        UUID uuid = player.getUUID();

        // ФИКС: Единая проверка для всех специальных режимов + принудительный сброс позы
        if (player.getAbilities().flying || player.isSpectator() || player.isFallFlying()) {
            CRAWLING_PLAYERS.remove(uuid);
            SITTING_PLAYERS.remove(uuid);
            FatigueSystem.SLIDE_TICKS.remove(uuid);
            
            // ОБЯЗАТЕЛЬНО возвращаем игрока в вертикальное состояние
            if ((player.getPose() == Pose.SWIMMING || player.isSwimming()) && !player.isInWater()) {
                player.setPose(Pose.STANDING);
                player.setSwimming(false);
            }
            return;
        }

        if (CRAWLING_PLAYERS.contains(uuid)) {
            // Если игрок висит на стене, не даем ему "плыть"
            if (!WallParkourSystem.isHanging()) {
                player.setPose(Pose.SWIMMING);
                player.setSwimming(true);
            }

            if (player.isInWater() || player.isInLava()) {
                FatigueSystem.SLIDE_TICKS.remove(uuid);
                return; 
            }

            if (!FatigueSystem.SLIDE_TICKS.containsKey(uuid)) {
                long currentTime = System.currentTimeMillis();
                long lastSlide = SLIDE_COOLDOWN.getOrDefault(uuid, 0L);
                double speedSqr = player.getDeltaMovement().horizontalDistanceSqr();
                
                if (!FatigueSystem.isLocked() && (player.isSprinting() || speedSqr > 0.002) 
                    && (currentTime - lastSlide > COOLDOWN_TIME)) { 
                    
                    Vec3 look = player.getLookAngle();
                    Vec3 kick = new Vec3(look.x, 0, look.z).normalize().scale(0.55);
                    player.setDeltaMovement(player.getDeltaMovement().add(kick));
                    player.hasImpulse = true;
                    player.hurtMarked = true;

                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(), 
                        SoundEvents.PLAYER_SMALL_FALL, SoundSource.PLAYERS, 1.0F, 0.7F);

                    if (player.level().isClientSide) FatigueSystem.addFatigue(player, 4.0f);

                    SLIDE_COOLDOWN.put(uuid, currentTime);
                    FatigueSystem.SLIDE_TICKS.put(uuid, 1);
                } else {
                    FatigueSystem.SLIDE_TICKS.put(uuid, 1); 
                }
            }

            int ticks = FatigueSystem.SLIDE_TICKS.getOrDefault(uuid, 0);
            if (ticks > 0 && ticks < 45) {
                double speedSqr = player.getDeltaMovement().horizontalDistanceSqr();
                if (speedSqr > 0.01) {
                    player.hasImpulse = true;
                    if (player.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(), 1, 0.1, 0.05, 0.1, 0.01);
                        BlockPos posBelow = player.blockPosition().below();
                        BlockState state = serverLevel.getBlockState(posBelow);
                        if (!state.isAir()) {
                            serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), player.getX(), player.getY(), player.getZ(), 2, 0.2, 0.05, 0.2, 0.1);
                        }
                    }
                    FatigueSystem.SLIDE_TICKS.put(uuid, ticks + 1);
                } else {
                    FatigueSystem.SLIDE_TICKS.put(uuid, 46);
                }
            }
        } else {
            FatigueSystem.SLIDE_TICKS.remove(uuid);
        }

        if (SITTING_PLAYERS.contains(uuid)) {
            if (player.getDeltaMovement().horizontalDistanceSqr() > 0.005) {
                SITTING_PLAYERS.remove(uuid);
                player.refreshDimensions();
            }
        }
    }

    @EventBusSubscriber(modid = "neweracombat", value = Dist.CLIENT)
    public static class ClientPoseEvents {
        @SubscribeEvent
        public static void onEntitySize(EntityEvent.Size event) {
            if (event.getEntity() instanceof Player player) {
                if (SITTING_PLAYERS.contains(player.getUUID())) {
                    event.setNewSize(EntityDimensions.scalable(0.6F, 0.4F).withEyeHeight(0.7F)); 
                }
            }
        }
        @SubscribeEvent
        public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
            boolean isSitting = SITTING_PLAYERS.contains(event.getEntity().getUUID());
            if (event.getRenderer().getModel() instanceof SittableModel sittable) {
                sittable.setForcedSitting(isSitting);
            }
        }
        @SubscribeEvent
        public static void onComputeFov(ComputeFovModifierEvent event) {
            Player player = event.getPlayer();
            if (player == null) return;
            
            float finalMod = event.getFovModifier();
            double horizontalSpeed = player.getDeltaMovement().horizontalDistance();
            float speedMod = 1.0f + (float) Math.min(0.25, horizontalSpeed * 0.35);
            finalMod *= speedMod;

            if (FatigueSystem.SLIDE_TICKS.containsKey(player.getUUID())) {
                int ticks = FatigueSystem.SLIDE_TICKS.get(player.getUUID());
                if (ticks > 0 && ticks < 40) {
                    float slideMod = 1.0f + (float) Math.min(0.12, ticks * 0.01);
                    finalMod *= slideMod;
                }
            }
            event.setNewFovModifier(finalMod);
        }
    }
}