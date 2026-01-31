package net.darkunity.neweracombat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Заглушка.
 * Вся логика (сеть, пакеты и т.д.) вынесена в другие классы.
 * Этот класс нужен MCreator'у, трогать его больше не надо.
 */
public final class NewEraCore {

    private static final Logger LOGGER =
            LogManager.getLogger("NewEraCombat-Core");

    private NewEraCore() {
    }

    public static void init() {
        LOGGER.info("NewEraCore loaded (stub)");
    }
}
