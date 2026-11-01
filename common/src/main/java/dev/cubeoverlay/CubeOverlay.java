package dev.cubeoverlay;

import dev.cubeoverlay.compat.Compat;
import net.dimaskama.mcef.api.MCEFApi;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CubeOverlay implements ClientModInitializer {

    public static final String MOD_ID = "cubeoverlay";
    public static final Logger LOGGER = LoggerFactory.getLogger("Cube Overlay");

    private static OverlayConfig config;
    private static final BrowserTabs TABS = new BrowserTabs();
    private static final long STARTED_AT = System.currentTimeMillis();
    private static long joinedAt = -1;
    private static int hintDelay = -1;
    private static boolean hintShown;

    public static OverlayConfig config() {
        return config;
    }

    public static BrowserTabs tabs() {
        return TABS;
    }

    /** Сколько миллисекунд игрок в текущем мире (или с запуска игры, если не в мире). */
    public static long sessionMillis() {
        return System.currentTimeMillis() - (joinedAt > 0 ? joinedAt : STARTED_AT);
    }

    @Override
    public void onInitializeClient() {
        config = OverlayConfig.load();
        // Chromium (~150 МБ) при первом запуске скачивается в фоне, игра не ждёт.
        MCEFApi.initialize();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            joinedAt = System.currentTimeMillis();
            hintDelay = 60; // игрока ещё нет в мире в момент JOIN — ждём 3 секунды
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> joinedAt = -1);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (hintDelay > 0 && --hintDelay == 0 && !hintShown && client.player != null) {
                hintShown = true;
                Compat.actionBar(Component.literal("§b◆ Cube Overlay§r — нажми §e"
                        + config.combo().displayName() + "§r, чтобы открыть оверлей"));
            }
            if (SELFTEST) {
                selfTestTick();
            }
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> TABS.closeAll());

        // displayName() тут нельзя: в 1.21.11 он лезет в GLFW, а окно ещё не создано
        LOGGER.info("Cube Overlay загружен, бинд: {}", String.join(" + ", config.combo().toNames()));
    }

    // Самопроверка для dev-клиента (gradlew runClient -Pselftest). В обычной игре выключена.
    private static final boolean SELFTEST = Boolean.getBoolean("cubeoverlay.selftest");
    private static SelfTest selfTest;

    private static void selfTestTick() {
        if (selfTest == null) selfTest = new SelfTest();
        selfTest.tick();
    }
}
