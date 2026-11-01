package dev.cubeoverlay;

import com.mojang.blaze3d.platform.InputConstants;
import dev.cubeoverlay.compat.Compat;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Автотест интерфейса для dev-клиента (gradlew runClient -Pselftest).
 * Жмёт по кнопкам и полям теми же событиями, что приходят от мыши/клавиатуры, и пишет PASS/FAIL в лог.
 * В обычной игре не запускается.
 */
final class SelfTest {

    /**
     * Тестовая страница: верх — кнопка, низ — поле в форме. Результат она сообщает переходом
     * на about:blank#... (на data: Chromium странице переходить не даёт, а смену #hash getURL не видит).
     */
    private static final String PAGE = "data:text/html," + URLEncoder.encode(
            "<body style='margin:0'>"
                    + "<button style='width:100%;height:50vh;font-size:40px' "
                    + "onclick=\"location.href='about:blank#clicked'\">CLICK</button>"
                    + "<form onsubmit=\"location.replace('about:blank#sent='+i.value);return false\">"
                    + "<input id='i' style='width:100%;height:50vh;font-size:40px'></form></body>",
            StandardCharsets.UTF_8).replace("+", "%20");

    private record Step(int delay, String name, Runnable action, BooleanSupplier check) {
    }

    private final List<Step> steps = new ArrayList<>();
    private int index = -1;
    private int waited;
    private int delayLeft;
    private int ticks;
    private int passed, failed;

    SelfTest() {
        // стектрейсы из потока AWT пишутся в System.err, а обёртка Minecraft их теряет — пишем stderr в файл
        try {
            System.setErr(new java.io.PrintStream(new java.io.FileOutputStream("selftest-stderr.txt"), true, StandardCharsets.UTF_8));
        } catch (java.io.IOException e) {
            CubeOverlay.LOGGER.error("[selftest] stderr redirect failed", e);
        }

        add(0, "overlay opens by combo", () -> {
            KeyCombo combo = CubeOverlay.config().combo();
            for (var key : combo.keys()) ComboTracker.onInput(key, ComboTracker.PRESS, true);
            for (var key : combo.keys()) ComboTracker.onInput(key, ComboTracker.RELEASE, true);
        }, () -> overlay() != null);
        add(1, "browser ready", () -> { }, () -> CubeOverlay.tabs().active() != null
                && CubeOverlay.tabs().active().browser().getTextureView() != null);
        add(10, "click address focuses it", () -> click("address"), () -> overlay().testAddressFocused());
        add(4, "typing replaces the address", () -> type(PAGE), () -> overlay().testAddressValue().equals(PAGE));
        add(2, "enter opens the page", () -> key(InputConstants.KEY_RETURN, 0x1C), () -> url().startsWith("data:text/html"));
        add(10, "click on page button", () -> click("view-top"), () -> url().equals("about:blank#clicked"));
        add(10, "back button returns", () -> click("back"), () -> url().startsWith("data:text/html"));
        add(20, "click into page input", () -> click("view-bottom"), () -> overlay().testBrowserFocused());
        add(4, "typing + backspace + enter in page", () -> {
            type("hello");
            key(InputConstants.KEY_BACKSPACE, 0x0E);
            key(InputConstants.KEY_RETURN, 0x1C);
        }, () -> url().equals("about:blank#sent=hell"));
        add(10, "dock link opens site", () -> click("dock-first"), () -> !url().startsWith("data:"));
        add(30, "plus opens a tab", () -> click("plus"), () -> CubeOverlay.tabs().tabs().size() == 2);
        add(20, "tab close button", () -> click("close-last-tab"), () -> CubeOverlay.tabs().tabs().size() == 1);
        add(10, "home button", () -> click("home"), () -> url().contains("google."));
        add(30, "gear opens settings", () -> click("gear"), () -> Compat.screen() instanceof SettingsScreen);
        add(10, "settings: pause toggle", () -> {
            boolean before = CubeOverlay.config().pauseGame;
            clickSettings("pause");
            pauseFlipped = CubeOverlay.config().pauseGame != before;
            clickSettings("pause"); // вернуть как было
        }, () -> pauseFlipped);
        add(4, "settings: home field focus", () -> clickSettings("home"), () -> settings().testHomeFocused());
        add(4, "settings: done", () -> clickSettings("done"), () -> overlay() != null);
    }

    private boolean pauseFlipped;

    private void add(int delay, String name, Runnable action, BooleanSupplier check) {
        steps.add(new Step(delay, name, action, check));
    }

    void tick() {
        ticks++;
        if (ticks < 100 || index >= steps.size()) return;
        if (index < 0) {
            start(0);
            return;
        }
        if (delayLeft > 0) {
            if (--delayLeft == 0) runCurrent();
            return;
        }
        Step step = steps.get(index);
        boolean ok;
        try {
            ok = step.check().getAsBoolean();
        } catch (RuntimeException e) {
            ok = false;
        }
        if (!ok && ++waited < 200) return; // ждём результат до 10 секунд
        if (ok) passed++; else failed++;
        CubeOverlay.LOGGER.info("[selftest] {} {}  (url={})", ok ? "PASS" : "FAIL", step.name(), safeUrl());
        if (index + 1 >= steps.size()) {
            index++;
            CubeOverlay.LOGGER.info("[selftest] DONE passed={} failed={}", passed, failed);
        } else {
            start(index + 1);
        }
    }

    private void start(int i) {
        index = i;
        waited = 0;
        delayLeft = steps.get(i).delay();
        if (delayLeft == 0) runCurrent();
    }

    private void runCurrent() {
        Step step = steps.get(index);
        try {
            step.action().run();
        } catch (RuntimeException e) {
            CubeOverlay.LOGGER.error("[selftest] step '{}' threw", step.name(), e);
        }
    }

    private static OverlayScreen overlay() {
        return Compat.screen() instanceof OverlayScreen o ? o : null;
    }

    private static SettingsScreen settings() {
        return Compat.screen() instanceof SettingsScreen s ? s : null;
    }

    private static String url() {
        BrowserTabs.Tab tab = CubeOverlay.tabs().active();
        return tab == null ? "" : tab.url();
    }

    private static String safeUrl() {
        String u = url();
        return u.length() > 60 ? u.substring(0, 60) + "…" : u;
    }

    private static void click(String what) {
        OverlayScreen o = overlay();
        double[] p = o.testPoint(what);
        o.mouseMoved(p[0], p[1]);
        MouseButtonEvent e = new MouseButtonEvent(p[0], p[1], new MouseButtonInfo(0, 0));
        o.mouseClicked(e, false);
        o.mouseReleased(e);
    }

    private static void clickSettings(String what) {
        SettingsScreen s = settings();
        double[] p = s.testPoint(what);
        MouseButtonEvent e = new MouseButtonEvent(p[0], p[1], new MouseButtonInfo(0, 0));
        s.mouseClicked(e, false);
        s.mouseReleased(e);
    }

    private static void type(String text) {
        var screen = Compat.screen();
        text.codePoints().forEach(cp -> screen.charTyped(Compat.charEvent(cp)));
    }

    private static void key(int key, int pcScancode) {
        var screen = Compat.screen();
        KeyEvent e = Compat.keyEvent(key, pcScancode);
        screen.keyPressed(e);
        screen.keyReleased(e);
    }
}
