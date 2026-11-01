package dev.cubeoverlay;

import com.mojang.blaze3d.platform.InputConstants;
import dev.cubeoverlay.compat.Compat;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Настройки. Главное — запись бинда: жмёшь на клавиши, зажимаешь нужные кнопки
 * (до трёх, можно вперемешку клавиатуру и мышь), отпускаешь — комбинация записана.
 */
public class SettingsScreen extends CubeScreen implements ComboTracker.CaptureListener {

    private static final int ROW = 24;

    private final Screen parent;
    private final OverlayConfig cfg = CubeOverlay.config();
    private KeyCombo combo = cfg.combo();

    private boolean capturing;
    private final List<InputConstants.Key> captured = new ArrayList<>();
    private final Set<InputConstants.Key> held = new LinkedHashSet<>();

    private final List<Ui.Button> buttons = new ArrayList<>();
    private EditBox homePage;
    private int px, py, pw, ph;
    private int comboX, comboY, comboW;

    public SettingsScreen(Screen parent) {
        super(Component.literal(Tr.t("settings.title")));
        this.parent = parent;
    }

    @Override
    protected void init() {
        layoutScale();
        buttons.clear();
        pw = Math.min(340, width - 20);
        ph = 30 + ROW * 7 + 34;
        px = (width - pw) / 2;
        py = Math.max(6, (height - ph) / 2);
        int right = px + pw - 12;
        int y = py + 30;

        buttons.add(new Ui.Button(px + pw - 20, py + 4, 16, 15, "✕", Ui.Style.GHOST, this::onClose));

        // строка бинда: [кейкапы] [сброс]
        comboW = 120;
        comboX = right - comboW - 44;
        comboY = y + 2;
        buttons.add(new Ui.Button(comboX, comboY, comboW, 16, "", Ui.Style.SOLID, this::startCapture)
                .selected(() -> capturing));
        buttons.add(new Ui.Button(right - 40, comboY, 40, 16, Tr.t("settings.reset"), Ui.Style.GHOST, () -> {
            combo = KeyCombo.defaultCombo();
            capturing = false;
        }));
        y += ROW;

        // домашняя страница
        String oldHome = homePage != null ? homePage.getValue() : cfg.homePage;
        homePage = addRenderableWidget(new EditBox(font, comboX + 20, y + 6, right - comboX - 24, 10,
                Component.literal(Tr.t("settings.home"))));
        homePage.setBordered(false);
        homePage.setMaxLength(2048);
        homePage.setTextColor(Ui.TEXT);
        homePage.setValue(oldHome);
        homePage.moveCursorToStart(false);
        y += ROW;

        // тумблеры — кликабельна вся строка
        buttons.add(new Ui.Button(px + 6, y, pw - 12, ROW - 2, "", Ui.Style.GHOST,
                () -> cfg.pauseGame = !cfg.pauseGame));
        y += ROW;
        buttons.add(new Ui.Button(px + 6, y, pw - 12, ROW - 2, "", Ui.Style.GHOST,
                () -> cfg.showSidebar = !cfg.showSidebar));
        y += ROW;

        // размер интерфейса: − Авто +
        buttons.add(new Ui.Button(right - 84, y + 3, 16, 16, "−", Ui.Style.SOLID, () -> {
            cfg.uiScale = cfg.uiScale == 0 ? Math.max(1, pixelScale - 1) : Math.max(0, cfg.uiScale - 1);
            rebuildWidgets();
        }));
        buttons.add(new Ui.Button(right - 16, y + 3, 16, 16, "+", Ui.Style.SOLID, () -> {
            cfg.uiScale = cfg.uiScale == 0 ? Math.min(4, pixelScale + 1) : (cfg.uiScale >= 4 ? 0 : cfg.uiScale + 1);
            rebuildWidgets();
        }));
        y += ROW;

        // затемнение: − 60% +
        buttons.add(new Ui.Button(right - 54, y + 3, 16, 16, "−", Ui.Style.SOLID,
                () -> cfg.backgroundAlpha = Math.max(0, cfg.backgroundAlpha - 26)));
        buttons.add(new Ui.Button(right - 16, y + 3, 16, 16, "+", Ui.Style.SOLID,
                () -> cfg.backgroundAlpha = Math.min(255, cfg.backgroundAlpha + 26)));
        y += ROW;

        buttons.add(new Ui.Button(right - 110, y + 3, 110, 16, Tr.t("settings.close_all"), Ui.Style.DANGER,
                () -> CubeOverlay.tabs().closeAll()));

        buttons.add(new Ui.Button(px + 12, py + ph - 26, pw - 24, 18, Tr.t("settings.done"), Ui.Style.ACCENT, this::onClose));
    }

    private void startCapture() {
        capturing = true;
        captured.clear();
        held.clear();
    }

    @Override
    public boolean isCapturing() {
        return capturing;
    }

    @Override
    public void onCaptureInput(InputConstants.Key key, boolean pressed) {
        if (pressed) {
            if (key.getName().equals("key.keyboard.escape") && captured.isEmpty()) {
                capturing = false; // отмена
            } else if (captured.size() < KeyCombo.MAX_KEYS && !captured.contains(key)) {
                captured.add(key);
                held.add(key);
            }
        } else {
            held.remove(key);
            // все кнопки отпущены — комбинация записана
            if (held.isEmpty() && !captured.isEmpty()) {
                combo = new KeyCombo(captured);
                capturing = false;
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        event = scaled(event);
        if (event.button() == 0) {
            for (Ui.Button b : buttons) {
                if (b.click(event.x(), event.y())) {
                    setFocused(null);
                    homePage.setFocused(false);
                    return true;
                }
            }
        }
        // поле рисуется «пилюлей» выше самого EditBox — клик по любой её части ставит фокус
        int right = px + pw - 12;
        int fy = py + 30 + ROW + 2;
        if (event.x() >= comboX + 16 && event.x() < right && event.y() >= fy && event.y() < fy + 16) {
            super.mouseClicked(event, doubled);
            setFocused(homePage);
            homePage.setFocused(true);
            return true;
        }
        if (homePage.isFocused()) {
            homePage.setFocused(false);
            setFocused(null);
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return super.mouseReleased(scaled(event));
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        return super.mouseDragged(scaled(event), vx(dx), vx(dy));
    }

    @Override
    public void mouseMoved(double x, double y) {
        super.mouseMoved(vx(x), vx(y));
    }

    @Override
    public void onClose() {
        cfg.setCombo(combo);
        String home = homePage.getValue().trim();
        cfg.homePage = home.isEmpty() ? "https://www.google.com" : BrowserTabs.normalizeUrl(home);
        cfg.save();
        Compat.setScreen(parent);
    }

    @Override
    protected void drawBackdrop(Draw d) {
        d.fill(0, 0, width, height, 0xB0080B10);
    }

    @Override
    protected void drawContents(Draw d, int mouseX, int mouseY, float partialTick) {
        Ui.glass(d, px, py, pw, ph);
        d.text(Tr.t("settings.header"), px + 12, py + 9, Ui.TEXT);
        d.fill(px + 1, py + 23, px + pw - 1, py + 24, Ui.LINE);

        for (Ui.Button b : buttons) b.draw(d, mouseX, mouseY);

        int lx = px + 12;
        int right = px + pw - 12;
        int y = py + 30;

        // бинд
        label(d, Tr.t("settings.bind"), capturing ? Tr.t("settings.bind.recording") : Tr.t("settings.bind.hint"), y);
        List<InputConstants.Key> keys = capturing ? captured : combo.keys();
        if (capturing && keys.isEmpty()) {
            boolean blink = (System.currentTimeMillis() / 400) % 2 == 0;
            d.centered(blink ? Tr.t("settings.bind.waiting") : "", comboX + comboW / 2, comboY + 4, 0xFFFFD866);
        } else {
            List<String> names = keys.stream().map(k -> Ui.shortKey(k.getDisplayName().getString())).toList();
            int capsW = 0;
            for (String n : names) capsW += d.font().width(n) + 8 + 10;
            capsW -= 10;
            Ui.keycaps(d, names, comboX + (comboW + capsW) / 2, comboY + 2);
        }
        if (capturing) Ui.roundOutline(d, comboX, comboY, comboW, 16, Ui.ACCENT);
        y += ROW;

        label(d, Tr.t("settings.home"), null, y);
        Ui.round(d, comboX + 16, y + 2, right - comboX - 16, 16, Ui.FIELD);
        Ui.roundOutline(d, comboX + 16, y + 2, right - comboX - 16, 16, homePage.isFocused() ? Ui.ACCENT : Ui.LINE);
        y += ROW;

        label(d, Tr.t("settings.pause"), Tr.t("settings.pause.hint"), y);
        Ui.toggle(d, right - 20, y + 6, cfg.pauseGame);
        y += ROW;

        label(d, Tr.t("settings.sidebar"), Tr.t("settings.sidebar.hint"), y);
        Ui.toggle(d, right - 20, y + 6, cfg.showSidebar);
        y += ROW;

        label(d, Tr.t("settings.scale"), Tr.t("settings.scale.hint"), y);
        String size = cfg.uiScale == 0 ? Tr.t("settings.scale.auto", pixelScale) : cfg.uiScale + "×";
        d.centered(size, right - 42, y + 7, Ui.TEXT);
        y += ROW;

        label(d, Tr.t("settings.dim"), null, y);
        d.centered(Math.round(cfg.backgroundAlpha / 2.55f) + "%", right - 27, y + 7, Ui.TEXT);
        y += ROW;

        label(d, Tr.t("settings.tabs"), Tr.t("settings.tabs.open", CubeOverlay.tabs().tabs().size()), y);
    }

    private void label(Draw d, String title, String hint, int y) {
        int x = px + 12;
        if (hint == null) {
            d.text(title, x, y + 7, Ui.TEXT);
        } else {
            d.text(title, x, y + 2, Ui.TEXT);
            d.text(hint, x, y + 12, Ui.FAINT);
        }
    }

    // ------------------------------------------------------------------ для самопроверки (-Dcubeoverlay.selftest)

    double[] testPoint(String what) {
        int right = px + pw - 12;
        int y0 = py + 30;
        double vx, vy;
        switch (what) {
            case "home" -> { vx = (comboX + 16 + right) / 2.0; vy = y0 + ROW + 10; }
            case "pause" -> { vx = px + pw / 2.0; vy = y0 + ROW * 2 + 10; }
            case "done" -> { vx = px + pw / 2.0; vy = py + ph - 17; }
            default -> throw new IllegalArgumentException(what);
        }
        float s = uiScale();
        return new double[]{vx * s, vy * s};
    }

    boolean testHomeFocused() {
        return homePage.isFocused() && getFocused() == homePage;
    }

    String testHomeValue() {
        return homePage.getValue();
    }
}
