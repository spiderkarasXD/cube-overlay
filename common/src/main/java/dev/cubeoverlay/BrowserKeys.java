package dev.cubeoverlay;

import dev.cubeoverlay.compat.Compat;
import net.minecraft.client.input.KeyEvent;
import org.cef.browser.CefBrowser;

import java.awt.event.InputEvent;
import java.lang.reflect.Field;

/**
 * Отправка клавиш в Chromium в обход MCEF.
 * <p>
 * jcef на Windows узнаёт, какая клавиша нажата, только по скрытому полю KeyEvent.scancode
 * (MapVirtualKey(scancode)). MCEF это поле не заполняет, поэтому буквы доходят (они идут отдельным
 * событием «символ»), а Backspace, Enter, стрелки, Delete, Ctrl+A/C/V — нет. Здесь поле заполняется
 * настоящим аппаратным scancode, а для клавиш, которые в Windows дают символ (Enter, Backspace, Tab),
 * дополнительно шлётся этот символ — ровно как WM_KEYDOWN + WM_CHAR.
 */
public final class BrowserKeys {

    private static final boolean WINDOWS = System.getProperty("os.name", "").startsWith("Windows");
    private static final sun.misc.Unsafe UNSAFE;
    private static final long SCANCODE_OFFSET;
    /** CefBrowser_N.sendKeyEvent — protected, зовём рефлексией (jcef не в модуле, это разрешено). */
    private static final java.lang.reflect.Method SEND_KEY;

    static {
        sun.misc.Unsafe unsafe = null;
        long offset = -1;
        java.lang.reflect.Method send = null;
        try {
            send = Class.forName("org.cef.browser.CefBrowser_N", false, CefBrowser.class.getClassLoader())
                    .getDeclaredMethod("sendKeyEvent", java.awt.event.KeyEvent.class);
            send.setAccessible(true);
            Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            unsafe = (sun.misc.Unsafe) f.get(null);
            offset = unsafe.objectFieldOffset(java.awt.event.KeyEvent.class.getDeclaredField("scancode"));
        } catch (Throwable t) {
            CubeOverlay.LOGGER.warn("Не получилось подготовить отправку клавиш, спецклавиши в браузере могут не работать", t);
        }
        UNSAFE = send != null ? unsafe : null;
        SCANCODE_OFFSET = offset;
        SEND_KEY = send;
    }

    private static void post(CefBrowser cef, java.awt.event.KeyEvent e) {
        try {
            SEND_KEY.invoke(cef, e);
        } catch (ReflectiveOperationException ex) {
            CubeOverlay.LOGGER.warn("sendKeyEvent failed", ex);
        }
    }

    /** PC scancode букв a..z. */
    private static final int[] LETTERS = {
            0x1E, 0x30, 0x2E, 0x20, 0x12, 0x21, 0x22, 0x23, 0x17, 0x24, 0x25, 0x26, 0x32,
            0x31, 0x18, 0x19, 0x10, 0x13, 0x1F, 0x14, 0x16, 0x2F, 0x11, 0x2D, 0x15, 0x2C
    };

    private BrowserKeys() {
    }

    /** @return false — отправить не удалось, пусть обработает MCEF. */
    static boolean send(CefBrowser cef, KeyEvent event, boolean pressed) {
        // на Linux/macOS jcef смотрит на AWT-коды, с ними MCEF справляется сам
        int scan = WINDOWS ? Compat.windowsScancode(event) : 0;
        if (UNSAFE == null || scan <= 0 || !SEND_KEY.getDeclaringClass().isInstance(cef)) {
            return false;
        }
        int mods = 0;
        if (event.hasShiftDown()) mods |= InputEvent.SHIFT_DOWN_MASK;
        if (event.hasControlDown()) mods |= InputEvent.CTRL_DOWN_MASK;
        if (event.hasAltDown()) mods |= InputEvent.ALT_DOWN_MASK;

        int vk = awtKeyCode(scan);
        long now = System.currentTimeMillis();
        java.awt.event.KeyEvent awt = new java.awt.event.KeyEvent(cef.getUIComponent(),
                pressed ? java.awt.event.KeyEvent.KEY_PRESSED : java.awt.event.KeyEvent.KEY_RELEASED,
                now, mods, vk, java.awt.event.KeyEvent.CHAR_UNDEFINED);
        UNSAFE.putLong(awt, SCANCODE_OFFSET, scan);
        post(cef, awt);

        // клавиши, у которых в Windows есть свой символ: без него Enter не отправляет формы
        char typed = switch (scan) {
            case 0x1C -> '\r';
            case 0x0E -> '\b';
            case 0x0F -> '\t';
            default -> 0;
        };
        if (pressed && typed != 0 && (mods & (InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK)) == 0) {
            post(cef, new java.awt.event.KeyEvent(cef.getUIComponent(), java.awt.event.KeyEvent.KEY_TYPED,
                    now, mods, java.awt.event.KeyEvent.VK_UNDEFINED, typed));
        }
        return true;
    }

    /** USB HID (SDL scancode, 26.3+) → PC scancode set 1, который понимает Windows. */
    public static int hidToSet1(int hid) {
        if (hid >= 4 && hid <= 29) return LETTERS[hid - 4]; // a..z
        if (hid >= 30 && hid <= 39) return 0x02 + (hid - 30); // 1..9, 0
        if (hid >= 58 && hid <= 67) return 0x3B + (hid - 58); // F1..F10
        return switch (hid) {
            case 40 -> 0x1C; // Enter
            case 41 -> 0x01; // Esc
            case 42 -> 0x0E; // Backspace
            case 43 -> 0x0F; // Tab
            case 44 -> 0x39; // Space
            case 45 -> 0x0C; case 46 -> 0x0D; case 47 -> 0x1A; case 48 -> 0x1B; case 49 -> 0x2B;
            case 51 -> 0x27; case 52 -> 0x28; case 53 -> 0x29; case 54 -> 0x33; case 55 -> 0x34; case 56 -> 0x35;
            case 68 -> 0x57; case 69 -> 0x58; // F11, F12
            case 73 -> 0x52; // Insert
            case 74 -> 0x47; // Home
            case 75 -> 0x49; // PageUp
            case 76 -> 0x53; // Delete
            case 77 -> 0x4F; // End
            case 78 -> 0x51; // PageDown
            case 79 -> 0x4D; // →
            case 80 -> 0x4B; // ←
            case 81 -> 0x50; // ↓
            case 82 -> 0x48; // ↑
            case 224, 228 -> 0x1D; // Ctrl
            case 225 -> 0x2A; // LShift
            case 229 -> 0x36; // RShift
            case 226, 230 -> 0x38; // Alt
            default -> 0;
        };
    }

    /** AWT-код клавиши (нужен jcef на Linux/macOS; на Windows хватает scancode). */
    private static int awtKeyCode(int scan) {
        return switch (scan) {
            case 0x1C -> java.awt.event.KeyEvent.VK_ENTER;
            case 0x0E -> java.awt.event.KeyEvent.VK_BACK_SPACE;
            case 0x0F -> java.awt.event.KeyEvent.VK_TAB;
            case 0x01 -> java.awt.event.KeyEvent.VK_ESCAPE;
            case 0x53 -> java.awt.event.KeyEvent.VK_DELETE;
            case 0x4B -> java.awt.event.KeyEvent.VK_LEFT;
            case 0x4D -> java.awt.event.KeyEvent.VK_RIGHT;
            case 0x48 -> java.awt.event.KeyEvent.VK_UP;
            case 0x50 -> java.awt.event.KeyEvent.VK_DOWN;
            case 0x47 -> java.awt.event.KeyEvent.VK_HOME;
            case 0x4F -> java.awt.event.KeyEvent.VK_END;
            case 0x49 -> java.awt.event.KeyEvent.VK_PAGE_UP;
            case 0x51 -> java.awt.event.KeyEvent.VK_PAGE_DOWN;
            default -> java.awt.event.KeyEvent.VK_UNDEFINED;
        };
    }
}
