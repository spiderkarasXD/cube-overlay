package dev.cubeoverlay;

import com.mojang.blaze3d.platform.InputConstants;
import dev.cubeoverlay.compat.Compat;
import net.dimaskama.mcef.api.MCEFApi;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Оверлей: верхняя панель, виджеты слева, окно браузера с вкладками, док с быстрыми ссылками. */
public class OverlayScreen extends CubeScreen {

    private static final int SIDEBAR_W = 112;
    private static final int TOP = 26;
    private static final int TAB_H = 15;
    private static final long FADE_MS = 180;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEE, d MMMM", Locale.forLanguageTag("ru"));
    private static final int[] LINK_COLORS = {0xFFFF4E45, 0xFF4C8BF5, 0xFF62C462, 0xFFA970FF, 0xFF1BD96A, 0xFFE6EDF3, 0xFFFFB443};

    private final Screen parent;
    private final BrowserTabs tabs = CubeOverlay.tabs();
    private final long openedAt = System.currentTimeMillis();
    private final List<Ui.Button> buttons = new ArrayList<>();
    private EditBox address;
    private String syncedUrl = "";
    private boolean browserFocused;
    private boolean browserDragging;
    private boolean autoOpened;
    private boolean sidebar;

    private int winX, winY, winW, winH;
    private int barY, addrX, addrW;
    private int viewX, viewY, viewW, viewH;
    private int dockY;

    public OverlayScreen(@Nullable Screen parent) {
        super(Component.literal("Cube Overlay"));
        this.parent = parent;
    }

    // ------------------------------------------------------------------ layout

    @Override
    protected void init() {
        layoutScale();
        OverlayConfig cfg = CubeOverlay.config();
        buttons.clear();
        sidebar = cfg.showSidebar && width >= 460;

        dockY = height - 22;
        winX = sidebar ? 8 + SIDEBAR_W + 6 : 8;
        winY = TOP;
        winW = width - 8 - winX;
        winH = dockY - 6 - winY;
        barY = winY + 4 + TAB_H;
        viewX = winX + 4;
        viewY = barY + 22;
        viewW = winW - 8;
        viewH = winY + winH - 4 - viewY;

        // верхняя панель: иконки справа
        buttons.add(new Ui.Button(width - 20, 3, 16, 15, "✕", Ui.Style.GHOST, this::onClose));
        buttons.add(new Ui.Button(width - 38, 3, 16, 15, "⚙", Ui.Style.GHOST,
                () -> Compat.setScreen(new SettingsScreen(this))));
        buttons.add(new Ui.Button(width - 56, 3, 16, 15, "☰", Ui.Style.GHOST, () -> {
            cfg.showSidebar = !cfg.showSidebar;
            cfg.save();
            rebuildWidgets();
        }).selected(() -> sidebar));

        // навигация
        int x = winX + 5;
        x = nav(x, "←", () -> withTab(t -> t.cef().goBack()), () -> tabs.active() != null && tabs.active().cef().canGoBack());
        x = nav(x, "→", () -> withTab(t -> t.cef().goForward()), () -> tabs.active() != null && tabs.active().cef().canGoForward());
        x = nav(x, "⟳", () -> withTab(t -> t.cef().reload()), () -> tabs.active() != null);
        x = nav(x, "⌂", () -> navigate(cfg.homePage), () -> true);
        addrX = x + 3;
        addrW = winX + winW - 6 - addrX;

        String old = address != null ? address.getValue() : "";
        address = new EditBox(font, addrX + 14, barY + 6, addrW - 18, 10, Component.literal("Адрес"));
        address.setBordered(false);
        address.setMaxLength(8192);
        address.setTextColor(Ui.TEXT);
        address.setHint(Component.literal("Поиск в Google или адрес сайта"));
        address.setValue(old);
        addRenderableWidget(address);

        // док: быстрые ссылки
        int lx = 10;
        int limit = width - 10 - hintWidth() - 10;
        int i = 0;
        for (OverlayConfig.QuickLink link : cfg.quickLinks) {
            int w = font.width(link.name()) + 20;
            if (lx + w > limit) break;
            int color = LINK_COLORS[i++ % LINK_COLORS.length];
            buttons.add(new LinkButton(lx, dockY + 2, w, 15, link.name(), color, () -> navigate(link.url())));
            lx += w + 4;
        }

        syncAddress(true);
    }

    private int nav(int x, String label, Runnable action, java.util.function.BooleanSupplier enabled) {
        buttons.add(new Ui.Button(x, barY + 2, 16, 16, label, Ui.Style.GHOST, action).enabled(enabled));
        return x + 17;
    }

    /** Кнопка-ссылка в доке с цветной точкой слева. */
    private static final class LinkButton extends Ui.Button {
        private final int dot;

        LinkButton(int x, int y, int w, int h, String label, int dot, Runnable action) {
            super(x, y, w, h, label, Ui.Style.SOLID, action);
            this.dot = dot;
        }

        @Override
        public void draw(Draw d, int mx, int my) {
            boolean hover = isOver(mx, my);
            Ui.round(d, x, y, w, h, hover ? 0xFF283444 : 0xC019212B);
            Ui.roundOutline(d, x, y, w, h, Ui.LINE);
            Ui.round(d, x + 6, y + 5, 6, 6, dot);
            d.text(label, x + 15, y + 4, hover ? Ui.TEXT : 0xFFB8C2CE);
        }
    }

    private int tabWidth() {
        int n = Math.max(1, tabs.tabs().size());
        int avail = winW - 10 - 18;
        return Math.max(40, Math.min(150, avail / n - 2));
    }

    private int tabX(int index) {
        return winX + 5 + index * (tabWidth() + 2);
    }

    private int tabsY() {
        return winY + 4;
    }

    private int plusX() {
        return tabX(tabs.tabs().size()) + 1;
    }

    private int tabAt(double mx, double my) {
        if (my < tabsY() || my >= tabsY() + TAB_H) return -1;
        for (int i = 0; i < tabs.tabs().size(); i++) {
            int tx = tabX(i);
            if (mx >= tx && mx < tx + tabWidth()) return i;
        }
        return -1;
    }

    private boolean onTabClose(int index, double mx) {
        return mx >= tabX(index) + tabWidth() - 13;
    }

    private boolean onPlus(double mx, double my) {
        return tabs.tabs().size() < BrowserTabs.MAX_TABS
                && my >= tabsY() && my < tabsY() + TAB_H && mx >= plusX() && mx < plusX() + TAB_H;
    }

    private boolean inView(double mx, double my) {
        return mx >= viewX && mx < viewX + viewW && my >= viewY && my < viewY + viewH;
    }

    private List<String> comboKeys() {
        return CubeOverlay.config().combo().keys().stream()
                .map(k -> Ui.shortKey(k.getDisplayName().getString())).toList();
    }

    private int hintWidth() {
        int w = font.width("закрыть") + 6;
        for (String k : comboKeys()) w += font.width(k) + 8 + 10;
        return w;
    }

    // ------------------------------------------------------------------ browser actions

    @Nullable
    private BrowserTabs.Tab liveTab() {
        // пока у страницы нет картинки, браузер ещё не создан — события мыши ему слать рано
        BrowserTabs.Tab tab = tabs.active();
        return tab != null && tab.browser().getTextureView() != null ? tab : null;
    }

    private void withTab(Consumer<BrowserTabs.Tab> action) {
        BrowserTabs.Tab tab = tabs.active();
        if (tab != null) action.accept(tab);
    }

    private void navigate(String input) {
        String url = BrowserTabs.localize(BrowserTabs.normalizeUrl(input));
        BrowserTabs.Tab tab = tabs.active();
        if (tab == null) {
            tabs.open(url);
        } else {
            tab.cef().loadURL(url);
        }
        address.setValue(url);
        syncedUrl = url;
        focusBrowser();
    }

    private void newTab() {
        if (tabs.open(CubeOverlay.config().homePage) != null) {
            syncAddress(true);
            focusAddress();
        }
    }

    /** Фокус в адресную строку с выделением всего адреса — как в браузере: сразу печатаешь новый. */
    private void focusAddress() {
        unfocusBrowser();
        setFocused(address);
        address.setFocused(true);
        address.moveCursorToEnd(false);
        address.setHighlightPos(0);
    }

    private void focusBrowser() {
        browserFocused = true;
        if (address != null) address.setFocused(false);
        setFocused(null);
        withTab(t -> t.browser().setFocus(true));
    }

    private void unfocusBrowser() {
        browserFocused = false;
        withTab(t -> t.browser().setFocus(false));
    }

    private void syncAddress(boolean force) {
        BrowserTabs.Tab tab = tabs.active();
        if (address == null || (address.isFocused() && !force)) return;
        String url = tab == null ? "" : tab.url();
        if (force || !url.equals(syncedUrl)) {
            syncedUrl = url;
            address.setValue(url);
            address.moveCursorToStart(false);
        }
    }

    private MouseButtonEvent toBrowser(MouseButtonEvent event) {
        return new MouseButtonEvent(bx(event.x()), by(event.y()), event.buttonInfo());
    }

    private int bx(double mx) {
        return (int) ((mx - viewX) * guiScale());
    }

    private int by(double my) {
        return (int) ((my - viewY) * guiScale());
    }

    /** Пикселей экрана на единицу интерфейса — в таком разрешении рендерится страница. */
    private double guiScale() {
        return pixelScale;
    }

    // ------------------------------------------------------------------ screen lifecycle

    @Override
    public void tick() {
        if (!autoOpened && BrowserTabs.isReady()) {
            autoOpened = true;
            if (tabs.tabs().isEmpty()) {
                tabs.open(CubeOverlay.config().homePage);
                focusBrowser();
            }
        }
        syncAddress(false);
    }

    @Override
    public void onClose() {
        Compat.setScreen(parent);
    }

    @Override
    public void removed() {
        // вкладки не закрываем — видео/музыка продолжат играть в фоне
        unfocusBrowser();
        browserDragging = false;
    }

    @Override
    public boolean isPauseScreen() {
        return CubeOverlay.config().pauseGame;
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        event = scaled(event);
        double mx = event.x(), my = event.y();
        BrowserTabs.Tab tab = liveTab();
        if (tab != null && inView(mx, my)) {
            focusBrowser();
            browserDragging = true;
            tab.browser().onMouseClicked(toBrowser(event), doubled);
            return true;
        }
        int hit = tabAt(mx, my);
        if (hit >= 0) {
            if (event.button() == 2 || onTabClose(hit, mx)) {
                tabs.close(hit);
            } else {
                tabs.select(hit);
                focusBrowser();
            }
            syncAddress(true);
            return true;
        }
        if (onPlus(mx, my)) {
            newTab();
            return true;
        }
        if (event.button() == 0) {
            for (Ui.Button b : buttons) {
                if (b.click(mx, my)) return true;
            }
        }
        unfocusBrowser();
        if (mx >= addrX && mx < addrX + addrW && my >= barY + 2 && my < barY + 18) {
            if (!address.isFocused()) {
                focusAddress(); // первый клик выделяет весь адрес
                return true;
            }
            // уже в фокусе — обычный клик ставит курсор
            address.visible = true;
            super.mouseClicked(event, doubled);
            setFocused(address);
            address.setFocused(true);
            return true;
        }
        if (address.isFocused()) {
            address.setFocused(false);
            setFocused(null);
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        event = scaled(event);
        BrowserTabs.Tab tab = liveTab();
        if (tab != null && (browserDragging || inView(event.x(), event.y()))) {
            browserDragging = false;
            tab.browser().onMouseReleased(toBrowser(event));
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        event = scaled(event);
        BrowserTabs.Tab tab = liveTab();
        if (tab != null && browserDragging) {
            tab.browser().onMouseMoved(bx(event.x()), by(event.y()));
            return true;
        }
        return super.mouseDragged(event, vx(dx), vx(dy));
    }

    @Override
    public void mouseMoved(double guiX, double guiY) {
        double mx = vx(guiX), my = vx(guiY);
        BrowserTabs.Tab tab = liveTab();
        if (tab != null) tab.browser().onMouseMoved(bx(mx), by(my));
        super.mouseMoved(mx, my);
    }

    @Override
    public boolean mouseScrolled(double guiX, double guiY, double horizontal, double vertical) {
        double mx = vx(guiX), my = vx(guiY);
        BrowserTabs.Tab tab = liveTab();
        if (tab != null && inView(mx, my)) {
            tab.browser().onMouseScrolled(bx(mx), by(my), vertical);
            return true;
        }
        return super.mouseScrolled(mx, my, horizontal, vertical);
    }

    private static boolean is(KeyEvent event, String keyName) {
        return InputConstants.getKey(event).getName().equals(keyName);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape()) {
            onClose();
            return true;
        }
        // горячие клавиши как в обычном браузере
        if (event.hasControlDown()) {
            if (is(event, "key.keyboard.t")) {
                newTab();
                return true;
            }
            if (is(event, "key.keyboard.w")) {
                tabs.close(tabs.activeIndex());
                syncAddress(true);
                return true;
            }
            if (is(event, "key.keyboard.l")) {
                focusAddress();
                return true;
            }
            if (is(event, "key.keyboard.tab") && !tabs.tabs().isEmpty()) {
                int n = tabs.tabs().size();
                tabs.select((tabs.activeIndex() + (event.hasShiftDown() ? n - 1 : 1)) % n);
                syncAddress(true);
                return true;
            }
        }
        if (is(event, "key.keyboard.f5")) {
            withTab(t -> t.cef().reload());
            return true;
        }
        BrowserTabs.Tab tab = liveTab();
        if (browserFocused && tab != null) {
            if (!BrowserKeys.send(tab.cef(), event, true)) tab.browser().onKeyPressed(event);
            return true;
        }
        if (address.isFocused() && event.isConfirmation()) {
            navigate(address.getValue());
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        BrowserTabs.Tab tab = liveTab();
        if (browserFocused && tab != null) {
            if (!BrowserKeys.send(tab.cef(), event, false)) tab.browser().onKeyReleased(event);
            return true;
        }
        return super.keyReleased(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        BrowserTabs.Tab tab = liveTab();
        if (browserFocused && tab != null) {
            tab.browser().onCharTyped(event);
            return true;
        }
        return super.charTyped(event);
    }

    // ------------------------------------------------------------------ drawing

    private float fade() {
        return Ui.easeOut((System.currentTimeMillis() - openedAt) / (float) FADE_MS);
    }

    @Override
    protected void drawBackdrop(Draw d) {
        Ui.faded(d, fade()).fill(0, 0, width, height, (CubeOverlay.config().backgroundAlpha << 24) | 0x080B10);
    }

    @Override
    protected void drawContents(Draw raw, int mouseX, int mouseY, float partialTick) {
        float t = fade();
        Draw d = Ui.faded(raw, t);
        LocalDateTime now = LocalDateTime.now();
        address.visible = t >= 1; // ванильное поле не умеет в анимацию — показываем после неё

        drawTopBar(d, now);

        // всё ниже верхней панели чуть «выезжает» при открытии
        d.pose().pushMatrix();
        d.pose().translate(0, (1 - t) * 10);
        if (sidebar) drawSidebar(d, now);
        drawWindow(d, mouseX, mouseY);
        drawDock(d, mouseX, mouseY);
        d.pose().popMatrix();
    }

    @Override
    protected void drawForeground(Draw d, int mouseX, int mouseY, float partialTick) {
        BrowserTabs.Tab tab = liveTab();
        if (tab != null && inView(mouseX, mouseY)) {
            d.browserCursor(tab.browser());
        }
    }

    private void drawTopBar(Draw d, LocalDateTime now) {
        d.fill(0, 0, width, 21, 0xE6090C11);
        d.fill(0, 21, width, 22, Ui.LINE);

        // логотип: маленький кубик
        Ui.round(d, 8, 5, 11, 11, Ui.ACCENT);
        d.fill(10, 7, 17, 10, 0xFF8FD0FF);
        d.fill(10, 10, 13, 14, 0xFF0E6FC0);
        d.fill(13, 10, 17, 14, 0xFF1484E0);
        d.text("Cube Overlay", 24, 7, Ui.TEXT, false);
        int ux = 24 + font.width("Cube Overlay") + 7;
        d.fill(ux, 6, ux + 1, 15, Ui.LINE);
        d.text(minecraft.getUser().getName(), ux + 7, 7, Ui.MUTED);

        String time = TIME.format(now);
        d.text(time, width / 2 - font.width(time) / 2, 7, Ui.TEXT, false);

        String fps = minecraft.getFps() + " FPS";
        d.rightAligned(fps, width - 64, 7, Ui.FAINT);

        for (Ui.Button b : buttons) {
            if (b.y < 20) b.draw(d, -1, -1);
        }
    }

    private void drawSidebar(Draw d, LocalDateTime now) {
        int x = 8;
        int y = TOP;
        int w = SIDEBAR_W;

        // часы
        Ui.glass(d, x, y, w, 46);
        Ui.bigText(d, TIME.format(now), x + 9, y + 8, Ui.TEXT, 2.5f);
        d.text(DATE.format(now), x + 9, y + 33, Ui.MUTED);
        y += 46 + 6;

        // игра
        List<String[]> game = new ArrayList<>();
        ServerData server = minecraft.getCurrentServer();
        if (minecraft.hasSingleplayerServer()) {
            game.add(new String[]{"Мир", "одиночный"});
        } else if (server != null) {
            game.add(new String[]{"Сервер", server.ip});
        } else {
            game.add(new String[]{"Где", "меню"});
        }
        game.add(new String[]{"Сессия", formatDuration(CubeOverlay.sessionMillis())});
        game.add(new String[]{"FPS", String.valueOf(minecraft.getFps())});
        y = card(d, x, y, w, "ИГРА", game);

        LocalPlayer player = minecraft.player;
        if (player != null && minecraft.level != null) {
            y = card(d, x, y, w, "ИГРОК", List.of(
                    new String[]{"X Y Z", player.getBlockX() + " " + player.getBlockY() + " " + player.getBlockZ()},
                    new String[]{"Мир", dimensionName()},
                    new String[]{"Здоровье", (int) Math.ceil(player.getHealth()) + "/" + (int) player.getMaxHealth()}
            ));
        }

        List<String[]> browser = new ArrayList<>();
        browser.add(new String[]{"Вкладки", tabs.tabs().size() + " / " + BrowserTabs.MAX_TABS});
        card(d, x, y, w, "БРАУЗЕР", browser);
    }

    private int card(Draw d, int x, int y, int w, String title, List<String[]> rows) {
        int h = 18 + rows.size() * 11 + 3;
        if (y + h > dockY - 6) return y;
        Ui.glass(d, x, y, w, h);
        d.text(title, x + 8, y + 6, Ui.FAINT);
        int ry = y + 19;
        for (String[] r : rows) {
            Ui.row(d, r[0], r[1], x + 8, ry, w - 16, Ui.TEXT);
            ry += 11;
        }
        return y + h + 6;
    }

    private void drawWindow(Draw d, int mouseX, int mouseY) {
        Ui.glass(d, winX, winY, winW, winH);

        // панель адреса — того же цвета, что активная вкладка (как в Chrome)
        d.fill(winX + 1, barY, winX + winW - 1, barY + 20, Ui.SURFACE);

        // вкладки
        int ty = tabsY();
        int tw = tabWidth();
        List<BrowserTabs.Tab> list = tabs.tabs();
        for (int i = 0; i < list.size(); i++) {
            int tx = tabX(i);
            boolean hover = tabAt(mouseX, mouseY) == i;
            boolean active = i == tabs.activeIndex();
            if (active) {
                Ui.round(d, tx, ty, tw, TAB_H + 3, Ui.SURFACE);
            } else if (hover) {
                Ui.round(d, tx, ty + 1, tw, TAB_H - 2, Ui.HOVER);
            } else if (i + 1 != tabs.activeIndex() && i + 1 < list.size()) {
                d.fill(tx + tw, ty + 4, tx + tw + 1, ty + TAB_H - 4, Ui.LINE); // разделитель
            }
            Ui.round(d, tx + 5, ty + 4, 6, 6, active ? Ui.ACCENT : Ui.FAINT);
            String label = font.plainSubstrByWidth(list.get(i).label(), tw - 28);
            d.text(label, tx + 14, ty + 4, active ? Ui.TEXT : Ui.MUTED);
            if (active || hover) {
                boolean overClose = hover && onTabClose(i, mouseX);
                if (overClose) Ui.round(d, tx + tw - 12, ty + 2, 10, 10, Ui.HOVER);
                d.text("×", tx + tw - 9, ty + 3, overClose ? Ui.TEXT : Ui.MUTED);
            }
        }
        if (list.size() < BrowserTabs.MAX_TABS) {
            int px = plusX();
            boolean hover = onPlus(mouseX, mouseY);
            if (hover) Ui.round(d, px, ty + 1, TAB_H, TAB_H - 2, Ui.HOVER);
            d.text("+", px + 5, ty + 3, hover ? Ui.TEXT : Ui.MUTED);
        }

        // кнопки навигации и адресная строка
        for (Ui.Button b : buttons) {
            if (b.y == barY + 2) b.draw(d, mouseX, mouseY);
        }
        boolean addrFocus = address.isFocused();
        Ui.round(d, addrX, barY + 2, addrW, 16, Ui.FIELD);
        Ui.roundOutline(d, addrX, barY + 2, addrW, 16, addrFocus ? Ui.ACCENT : Ui.LINE);
        BrowserTabs.Tab active = tabs.active();
        boolean secure = active != null && active.url().startsWith("https://");
        Ui.round(d, addrX + 5, barY + 7, 6, 6, secure ? Ui.GOOD : Ui.FAINT);

        // страница
        d.fill(viewX, viewY, viewX + viewW, viewY + viewH, 0xFF0B0F14);
        if (BrowserTabs.isFailed()) {
            centered(d, "Не удалось запустить Chromium", -6, Ui.DANGER);
            centered(d, "Подробности в logs/latest.log", 6, Ui.MUTED);
        } else if (!BrowserTabs.isReady()) {
            drawLoading(d);
        } else if (active == null) {
            centered(d, "Нет открытых вкладок", -6, Ui.TEXT);
            centered(d, "Нажми + или Ctrl+T", 6, Ui.MUTED);
        } else {
            double scale = guiScale();
            active.resize((int) (viewW * scale), (int) (viewH * scale));
            d.browser(active.browser(), viewX, viewY, viewW, viewH);
        }
    }

    private void centered(Draw d, String text, int dy, int color) {
        d.centered(text, viewX + viewW / 2, viewY + viewH / 2 + dy - 4, color);
    }

    private void drawLoading(Draw d) {
        MCEFApi.Initialization init = MCEFApi.initialize();
        String stage = switch (init.getStage()) {
            case NOT_STARTED -> "Ищу Chromium";
            case DOWNLOADING -> "Скачиваю Chromium";
            case EXTRACTING -> "Распаковываю";
            case INSTALL -> "Устанавливаю";
            case INITIALIZING, DONE -> "Запускаю браузер";
        };
        int cx = viewX + viewW / 2;
        int cy = viewY + viewH / 2;
        d.centered(stage, cx, cy - 18, Ui.TEXT);
        d.centered("только при первом запуске, ~150 МБ", cx, cy - 6, Ui.FAINT);
        float pct = init.getPercentage();
        int barW = Math.min(200, viewW - 40);
        int bx = cx - barW / 2;
        Ui.round(d, bx, cy + 8, barW, 4, 0xFF1C2530);
        if (pct >= 0) {
            int fillW = Math.max(4, (int) (barW * Math.min(100, pct) / 100f));
            Ui.round(d, bx, cy + 8, fillW, 4, Ui.ACCENT);
            d.centered((int) pct + "%", cx, cy + 18, Ui.MUTED);
        } else {
            // бегущая полоска, пока процент неизвестен
            int seg = barW / 4;
            int pos = (int) ((System.currentTimeMillis() / 5) % (barW + seg)) - seg;
            int a = Math.max(0, pos), b = Math.min(barW, pos + seg);
            if (b - a >= 4) Ui.round(d, bx + a, cy + 8, b - a, 4, Ui.ACCENT);
        }
    }

    private void drawDock(Draw d, int mouseX, int mouseY) {
        for (Ui.Button b : buttons) {
            if (b.y >= dockY) b.draw(d, mouseX, mouseY);
        }
        int right = width - 10;
        d.rightAligned("закрыть", right, dockY + 6, Ui.MUTED);
        Ui.keycaps(d, comboKeys(), right - font.width("закрыть") - 6, dockY + 3);
    }

    private String dimensionName() {
        // ResourceKey[minecraft:dimension / minecraft:overworld] → overworld
        String key = minecraft.level.dimension().toString();
        int slash = key.lastIndexOf('/');
        String id = slash >= 0 ? key.substring(slash + 1).replace("]", "").trim() : key;
        return switch (id) {
            case "minecraft:overworld" -> "Верхний";
            case "minecraft:the_nether" -> "Незер";
            case "minecraft:the_end" -> "Энд";
            default -> id.replace("minecraft:", "");
        };
    }

    private static String formatDuration(long millis) {
        long s = millis / 1000;
        return String.format(Locale.ROOT, "%d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60);
    }

    // ------------------------------------------------------------------ для самопроверки (-Dcubeoverlay.selftest)

    /** Точка элемента в координатах GUI Minecraft (как приходят настоящие клики). */
    double[] testPoint(String what) {
        double vx, vy;
        switch (what) {
            case "address" -> { vx = addrX + addrW / 2.0; vy = barY + 10; }
            case "home" -> { vx = winX + 5 + 17 * 3 + 8; vy = barY + 10; }
            case "back" -> { vx = winX + 5 + 8; vy = barY + 10; }
            case "plus" -> { vx = plusX() + TAB_H / 2.0; vy = tabsY() + TAB_H / 2.0; }
            case "close-last-tab" -> { vx = tabX(tabs.tabs().size() - 1) + tabWidth() - 7; vy = tabsY() + 7; }
            case "view-top" -> { vx = viewX + viewW / 2.0; vy = viewY + viewH * 0.25; }
            case "view-bottom" -> { vx = viewX + viewW / 2.0; vy = viewY + viewH * 0.75; }
            case "gear" -> { vx = width - 30; vy = 10; }
            case "dock-first" -> {
                Ui.Button b = buttons.stream().filter(x -> x.y >= dockY).findFirst().orElseThrow();
                vx = b.x + b.w / 2.0; vy = b.y + b.h / 2.0;
            }
            default -> throw new IllegalArgumentException(what);
        }
        float s = uiScale();
        return new double[]{vx * s, vy * s};
    }

    boolean testAddressFocused() {
        return address.isFocused() && getFocused() == address;
    }

    String testAddressValue() {
        return address.getValue();
    }

    boolean testBrowserFocused() {
        return browserFocused;
    }
}
