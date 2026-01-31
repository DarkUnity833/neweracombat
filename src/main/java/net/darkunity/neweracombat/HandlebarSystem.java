package net.darkunity.neweracombat;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Pose;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Random;

@EventBusSubscriber(modid = "neweracombat", value = Dist.CLIENT)
public class HandlebarSystem {
    private static boolean isGrabbing = false;
    private static int swingTimer = 0;
    private static InteractionHand lastHand = InteractionHand.MAIN_HAND;
    private static final Random RANDOM = new Random();
    private static int boostCooldown = 0;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.getEntity() != mc.player || mc.player == null) return;
        var player = mc.player;

        if (boostCooldown > 0) boostCooldown--;

        // ИСПРАВЛЕНИЕ: Защита креатива и спектатора
        if (player.getAbilities().flying || player.isSpectator()) {
            isGrabbing = false;
            return;
        }

        if (isGrabbing) {
            player.input.shiftKeyDown = false;
            player.setShiftKeyDown(false);
            player.setPose(Pose.STANDING);
            player.fallDistance = 0;

            if (mc.options.keyJump.isDown() && boostCooldown == 0) {
                Vec3 look = player.getLookAngle();
                player.setDeltaMovement(look.x * 0.6, 0.35, look.z * 0.6);
                
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), 
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8F, 1.2F);
                
                boostCooldown = 15;
                isGrabbing = false;
                return; 
            }

            double targetSpeed = 0.08; 
            float forward = player.input.forwardImpulse;
            float strafe = player.input.leftImpulse;
            
            if (forward != 0 || strafe != 0) {
                float angle = player.getYRot();
                double radians = Math.toRadians(angle);
                Vec3 moveVec = new Vec3(strafe, 0, forward).normalize().scale(targetSpeed);
                
                double finalX = moveVec.x * Math.cos(radians) - moveVec.z * Math.sin(radians);
                double finalZ = moveVec.z * Math.cos(radians) + moveVec.x * Math.sin(radians);

                player.setDeltaMovement(finalX, 0.07, finalZ);

                if (player.level().getGameTime() % 8 == 0) {
                    player.level().playSound(null, player.getX(), player.getY() + 2, player.getZ(), 
                        SoundEvents.GRASS_STEP, SoundSource.PLAYERS, 0.4F, 0.7F);
                }

                if (player.level().getGameTime() % 2 == 0) {
                    spawnCeilingParticles(player);
                }

                swingTimer++;
                if (swingTimer % 8 == 0) {
                    player.swing(lastHand);
                    lastHand = (lastHand == InteractionHand.MAIN_HAND) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
                }
            } else {
                player.setDeltaMovement(0, 0.05, 0);
                swingTimer = 0;
            }
        }
    }

    private static void spawnCeilingParticles(net.minecraft.client.player.LocalPlayer player) {
        BlockPos ceilingPos = BlockPos.containing(player.getX(), player.getY() + 2.2, player.getZ());
        BlockState state = player.level().getBlockState(ceilingPos);
        if (!state.isAir()) {
            for (int i = 0; i < 2; i++) {
                double px = player.getX() + (RANDOM.nextDouble() - 0.5) * 0.6;
                double py = player.getY() + 2.1;
                double pz = player.getZ() + (RANDOM.nextDouble() - 0.5) * 0.6;
                player.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state), px, py, pz, 0, -0.1, 0);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderTick(RenderFrameEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.isPaused()) return;
        var player = mc.player;

        // ИСПРАВЛЕНИЕ: Защита креатива и спектатора в рендере
        if (player.getAbilities().flying || player.isSpectator()) {
            isGrabbing = false;
            return;
        }

        if (mc.options.keyShift.isDown() && !player.onGround() && checkCeiling(player) && boostCooldown < 10) {
            isGrabbing = true;
            player.input.shiftKeyDown = false; 
            player.setPose(Pose.STANDING);
        } else {
            isGrabbing = false;
        }
    }

    private static boolean checkCeiling(net.minecraft.client.player.LocalPlayer player) {
        BlockPos pos = BlockPos.containing(player.getX(), player.getY() + 2.2, player.getZ());
        return !player.level().getBlockState(pos).isAir();
    }

    public static boolean isGrabbingCeiling() { 
        return isGrabbing && !Minecraft.getInstance().player.getAbilities().flying; 
    }
}