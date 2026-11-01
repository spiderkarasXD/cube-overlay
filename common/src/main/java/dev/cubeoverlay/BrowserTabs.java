package dev.cubeoverlay;

import net.dimaskama.mcef.api.MCEFApi;
import net.dimaskama.mcef.api.MCEFBrowser;
import org.cef.browser.CefBrowser;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Вкладки браузера. Живут, пока запущена игра: оверлей можно закрыть,
 * а музыка с YouTube продолжит играть — как в Steam.
 */
public final class BrowserTabs {

    public static final int MAX_TABS = 8;

    public static final class Tab {
        private final MCEFBrowser browser;
        private int width = -1;
        private int height = -1;

        private Tab(MCEFBrowser browser) {
            this.browser = browser;
        }

        public MCEFBrowser browser() {
            return browser;
        }

        public CefBrowser cef() {
            return browser.getCefBrowser();
        }

        public String url() {
            String url = cef().getURL();
            return url == null ? "" : url;
        }

        /** Подпись вкладки: домен без www. */
        public String label() {
            String url = url();
            try {
                String host = URI.create(url).getHost();
                if (host != null && !host.isEmpty()) {
                    return host.startsWith("www.") ? host.substring(4) : host;
                }
            } catch (IllegalArgumentException ignored) {
            }
            return url.isEmpty() ? "Новая вкладка" : url;
        }

        public void resize(int width, int height) {
            if (width != this.width || height != this.height) {
                this.width = width;
                this.height = height;
                browser.resize(width, height);
            }
        }
    }

    private final List<Tab> tabs = new ArrayList<>();
    private int active = -1;

    public static boolean isReady() {
        var future = MCEFApi.initialize().getFuture();
        return future.isDone() && !future.isCompletedExceptionally();
    }

    /** Chromium не смог запуститься (нет интернета при первом запуске, антивирус и т.п.). */
    public static boolean isFailed() {
        return MCEFApi.initialize().getFuture().isCompletedExceptionally();
    }

    public List<Tab> tabs() {
        return tabs;
    }

    public int activeIndex() {
        return active;
    }

    @Nullable
    public Tab active() {
        return active >= 0 && active < tabs.size() ? tabs.get(active) : null;
    }

    public void select(int index) {
        if (index >= 0 && index < tabs.size()) {
            Tab old = active();
            if (old != null) old.browser().setFocus(false);
            active = index;
        }
    }

    @Nullable
    public Tab open(String url) {
        if (!isReady() || tabs.size() >= MAX_TABS) {
            return null;
        }
        Tab tab = new Tab(MCEFApi.getInstance().createBrowser(normalizeUrl(url), false));
        tabs.add(tab);
        select(tabs.size() - 1);
        return tab;
    }

    public void close(int index) {
        if (index < 0 || index >= tabs.size()) {
            return;
        }
        tabs.remove(index).browser().close();
        if (tabs.isEmpty()) {
            active = -1;
        } else if (active >= tabs.size() || index < active) {
            active = Math.max(0, active - 1);
        }
    }

    public void closeAll() {
        for (Tab tab : tabs) {
            tab.browser().close();
        }
        tabs.clear();
        active = -1;
    }

    /**
     * Превращает текст из адресной строки в URL: "youtube.com" → https://youtube.com,
     * "как сделать портал" → поиск в Google.
     */
    public static String normalizeUrl(String input) {
        String text = input.trim();
        if (text.isEmpty()) {
            return "about:blank";
        }
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("://") || lower.matches("^(about|data|file|chrome|view-source):.*")) {
            return text; // уже есть схема
        }
        boolean looksLikeHost = !text.contains(" ")
                && (text.contains(".") || lower.startsWith("localhost"));
        if (looksLikeHost) {
            return "https://" + text;
        }
        return "https://www.google.com/search?hl=ru&q=" + URLEncoder.encode(text, StandardCharsets.UTF_8);
    }
}
