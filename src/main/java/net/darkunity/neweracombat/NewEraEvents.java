package net.darkunity.neweracombat;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NewEraEvents {

    private static final Logger LOGGER = LogManager.getLogger("NewEraCombat-Events");

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
}
}
