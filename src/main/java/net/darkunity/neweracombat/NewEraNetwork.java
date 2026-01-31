package net.darkunity.neweracombat;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@EventBusSubscriber(modid = "neweracombat", bus = EventBusSubscriber.Bus.MOD)
public final class NewEraNetwork {

    private static final Logger LOGGER = LogManager.getLogger("NewEraCombat-Network");

    private NewEraNetwork() {}

    @SubscribeEvent
    public static void onRegisterPayloads(final RegisterPayloadHandlersEvent event) {
        LOGGER.info("Starting NewEraCombat packet registration...");

        final PayloadRegistrar registrar = event.registrar("neweracombat").versioned("1");

        // Поскольку файлы в той же папке (корне), обращаемся к ним напрямую
        registrar.playToServer(
                OffhandSwapPacket.TYPE,
                OffhandSwapPacket.CODEC,
                OffhandSwapPacket::handle
        );

        registrar.playToServer(
                ServerboundAxeThrowPacket.TYPE,
                ServerboundAxeThrowPacket.CODEC,
                ServerboundAxeThrowPacket::handle
        );

        registrar.playToServer(
                ServerboundPoseUpdatePacket.TYPE,
                ServerboundPoseUpdatePacket.CODEC,
                ServerboundPoseUpdatePacket::handle
        );

        registrar.playToServer(
                ServerboundSitPacket.TYPE,
                ServerboundSitPacket.CODEC,
                ServerboundSitPacket::handle
        );

        LOGGER.info("Network packets registered successfully.");
    }
}