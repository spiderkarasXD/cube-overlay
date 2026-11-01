package dev.cubeoverlay;

import net.dimaskama.mcef.api.MCEFBrowser;
import net.minecraft.client.gui.Font;
import org.joml.Matrix3x2fStack;

/**
 * Рисование без привязки к версии: в 1.21.11 это GuiGraphics, в 26.x — GuiGraphicsExtractor.
 * Реализация лежит в versions/&lt;mc&gt;/.../compat/DrawImpl.java. Цвета — ARGB (0xFFrrggbb).
 */
public interface Draw {

    Font font();

    /** Матрица трансформаций GUI (сдвиг/масштаб для анимаций и крупного текста). */
    Matrix3x2fStack pose();

    void fill(int x1, int y1, int x2, int y2, int color);

    void outline(int x, int y, int width, int height, int color);

    void text(String text, int x, int y, int color, boolean shadow);

    /** Картинка страницы браузера, растянутая на прямоугольник. */
    void browser(MCEFBrowser browser, int x, int y, int width, int height);

    /** Курсор как в браузере: рука над ссылкой, I-образный над текстом. */
    void browserCursor(MCEFBrowser browser);

    default void text(String text, int x, int y, int color) {
        text(text, x, y, color, false);
    }

    default void centered(String text, int centerX, int y, int color) {
        text(text, centerX - font().width(text) / 2, y, color, false);
    }

    default void rightAligned(String text, int right, int y, int color) {
        text(text, right - font().width(text), y, color, false);
    }

    /** Панель с рамкой. */
    default void panel(int x, int y, int width, int height, int background, int border) {
        fill(x, y, x + width, y + height, background);
        outline(x, y, width, height, border);
    }
}
