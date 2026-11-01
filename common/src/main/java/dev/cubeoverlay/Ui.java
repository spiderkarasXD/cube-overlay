package dev.cubeoverlay;

import net.dimaskama.mcef.api.MCEFBrowser;
import net.minecraft.client.gui.Font;
import org.joml.Matrix3x2fStack;

import java.util.List;
import java.util.function.BooleanSupplier;

/** Свой мини-набор виджетов: тёмное стекло, плоские кнопки, клавиши-«кейкапы». Без ванильных текстур. */
public final class Ui {

    // палитра
    public static final int GLASS = 0xE0121820;
    public static final int GLASS_LIGHT = 0xF01A222D;
    public static final int SURFACE = 0xFF212B38;
    public static final int FIELD = 0xFF0D1218;
    public static final int LINE = 0x1FFFFFFF;
    public static final int HOVER = 0x1AFFFFFF;
    public static final int PRESS = 0x2EFFFFFF;
    public static final int TEXT = 0xFFE8EDF2;
    public static final int MUTED = 0xFF8A96A5;
    public static final int FAINT = 0xFF5B6675;
    public static final int ACCENT = 0xFF1A9FFF;
    public static final int ACCENT_SOFT = 0x401A9FFF;
    public static final int GOOD = 0xFF4CD07D;
    public static final int DANGER = 0xFFFF5D5D;

    private Ui() {
    }

    /** Прямоугольник со скруглёнными (2px) углами. Слои не перекрываются, так что полупрозрачные цвета ровные. */
    public static void round(Draw d, int x, int y, int w, int h, int color) {
        if (w < 4 || h < 4) {
            d.fill(x, y, x + w, y + h, color);
            return;
        }
        d.fill(x + 2, y, x + w - 2, y + 1, color);
        d.fill(x + 1, y + 1, x + w - 1, y + 2, color);
        d.fill(x, y + 2, x + w, y + h - 2, color);
        d.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, color);
        d.fill(x + 2, y + h - 1, x + w - 2, y + h, color);
    }

    /** Скруглённая рамка в 1px. */
    public static void roundOutline(Draw d, int x, int y, int w, int h, int color) {
        d.fill(x + 2, y, x + w - 2, y + 1, color);
        d.fill(x + 2, y + h - 1, x + w - 2, y + h, color);
        d.fill(x, y + 2, x + 1, y + h - 2, color);
        d.fill(x + w - 1, y + 2, x + w, y + h - 2, color);
        d.fill(x + 1, y + 1, x + 2, y + 2, color);
        d.fill(x + w - 2, y + 1, x + w - 1, y + 2, color);
        d.fill(x + 1, y + h - 2, x + 2, y + h - 1, color);
        d.fill(x + w - 2, y + h - 2, x + w - 1, y + h - 1, color);
    }

    /** Стеклянная панель: мягкая тень, фон, тонкая светлая рамка. */
    public static void glass(Draw d, int x, int y, int w, int h) {
        round(d, x + 1, y + 2, w, h, 0x50000000);
        round(d, x, y, w, h, GLASS);
        roundOutline(d, x, y, w, h, LINE);
    }

    public static void bigText(Draw d, String text, int x, int y, int color, float scale) {
        Matrix3x2fStack pose = d.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(scale, scale);
        d.text(text, 0, 0, color, false);
        pose.popMatrix();
    }

    /** Подпись слева, значение справа (обрезается, если не влезает). */
    public static void row(Draw d, String label, String value, int x, int y, int w, int valueColor) {
        d.text(label, x, y, MUTED);
        int lw = d.font().width(label) + 6;
        String v = d.font().plainSubstrByWidth(value, w - lw);
        d.rightAligned(v, x + w, y, valueColor);
    }

    /** Клавиши как на клавиатуре: [Shift] + [Tab]. Рисует справа налево от right, возвращает левый край. */
    public static int keycaps(Draw d, List<String> keys, int right, int y) {
        Font font = d.font();
        int x = right;
        for (int i = keys.size() - 1; i >= 0; i--) {
            String k = keys.get(i);
            int w = font.width(k) + 8;
            x -= w;
            round(d, x, y + 1, w, 13, 0xFF0A0E13);
            round(d, x, y, w, 12, SURFACE);
            roundOutline(d, x, y, w, 12, 0x30FFFFFF);
            d.text(k, x + 4, y + 2, TEXT);
            if (i > 0) {
                x -= 10;
                d.text("+", x + 2, y + 2, FAINT);
            }
        }
        return x;
    }

    /** "Left Shift" → "Shift", "Левый Ctrl" → "Ctrl": на кейкапах короче — красивее. */
    public static String shortKey(String name) {
        return name.replaceFirst("^(Left|Right|Левый|Левая|Правый|Правая) ", "");
    }

    // ------------------------------------------------------------------ кнопки

    public enum Style { GHOST, SOLID, ACCENT, DANGER, TAB }

    /** Плоская кнопка: рисуется и кликается вручную из экрана. */
    public static class Button {
        public int x, y, w, h;
        public String label;
        public final Style style;
        public final Runnable action;
        private BooleanSupplier selected = () -> false;
        private BooleanSupplier enabled = () -> true;
        private int labelColor = 0;

        public Button(int x, int y, int w, int h, String label, Style style, Runnable action) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.label = label;
            this.style = style;
            this.action = action;
        }

        public Button selected(BooleanSupplier selected) {
            this.selected = selected;
            return this;
        }

        public Button enabled(BooleanSupplier enabled) {
            this.enabled = enabled;
            return this;
        }

        public Button labelColor(int color) {
            this.labelColor = color;
            return this;
        }

        public boolean isOver(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }

        public boolean click(double mx, double my) {
            if (isOver(mx, my) && enabled.getAsBoolean()) {
                action.run();
                return true;
            }
            return false;
        }

        public void draw(Draw d, int mx, int my) {
            boolean on = enabled.getAsBoolean();
            boolean hover = on && isOver(mx, my);
            boolean sel = selected.getAsBoolean();
            int bg = switch (style) {
                case GHOST -> sel ? PRESS : hover ? HOVER : 0;
                case SOLID, TAB -> sel ? SURFACE : hover ? 0xFF283444 : 0xFF19212B;
                case ACCENT -> hover ? 0xFF47B2FF : ACCENT;
                case DANGER -> hover ? 0x60FF5D5D : 0x30FF5D5D;
            };
            if (bg != 0) round(d, x, y, w, h, bg);
            if (style == Style.SOLID && !sel) roundOutline(d, x, y, w, h, LINE);
            int color = labelColor != 0 ? labelColor
                    : !on ? FAINT
                    : style == Style.ACCENT ? 0xFFFFFFFF
                    : style == Style.DANGER ? DANGER
                    : hover || sel ? TEXT : MUTED;
            String text = d.font().plainSubstrByWidth(label, w - 6);
            d.text(text, x + (w - d.font().width(text)) / 2 + 1, y + (h - 8) / 2, color);
        }
    }

    /** Переключатель-«таблетка». */
    public static void toggle(Draw d, int x, int y, boolean on) {
        round(d, x, y, 20, 10, on ? ACCENT : 0xFF2A3442);
        int kx = on ? x + 11 : x + 1;
        round(d, kx, y + 1, 8, 8, 0xFFFFFFFF);
    }

    // ------------------------------------------------------------------ плавное появление

    /** Draw, у которого все цвета умножены на alpha — для fade-in при открытии. */
    public static Draw faded(Draw d, float alpha) {
        if (alpha >= 0.999f) return d;
        return new Draw() {
            private int a(int color) {
                int ca = (color >>> 24) & 0xFF;
                return ((int) (ca * alpha) << 24) | (color & 0xFFFFFF);
            }

            @Override
            public Font font() {
                return d.font();
            }

            @Override
            public Matrix3x2fStack pose() {
                return d.pose();
            }

            @Override
            public void fill(int x1, int y1, int x2, int y2, int color) {
                d.fill(x1, y1, x2, y2, a(color));
            }

            @Override
            public void outline(int x, int y, int width, int height, int color) {
                d.outline(x, y, width, height, a(color));
            }

            @Override
            public void text(String text, int x, int y, int color, boolean shadow) {
                // почти прозрачный текст Minecraft рисует непрозрачным — просто не рисуем его
                if (alpha > 0.1f) d.text(text, x, y, a(color), shadow);
            }

            @Override
            public void browser(MCEFBrowser browser, int x, int y, int width, int height) {
                d.browser(browser, x, y, width, height);
            }

            @Override
            public void browserCursor(MCEFBrowser browser) {
                d.browserCursor(browser);
            }
        };
    }

    public static float easeOut(float t) {
        t = Math.clamp(t, 0f, 1f);
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }
}
