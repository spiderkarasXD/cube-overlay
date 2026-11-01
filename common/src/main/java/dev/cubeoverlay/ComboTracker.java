package dev.cubeoverlay;

import com.mojang.blaze3d.platform.InputConstants;
import dev.cubeoverlay.compat.Compat;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Следит, какие кнопки клавиатуры и мыши сейчас зажаты (по событиям из миксинов),
 * и открывает/закрывает оверлей, когда зажата вся комбинация.
 */
public final class ComboTracker {

    public static final int RELEASE = 0;
    public static final int PRESS = 1;

    /** Экран, которому нужны «сырые» нажатия (запись нового бинда). */
    public interface CaptureListener {
        /** Пока true, все нажатия идут сюда, а не игре. */
        boolean isCapturing();

        void onCaptureInput(InputConstants.Key key, boolean pressed);
    }

    private static final Set<InputConstants.Key> PRESSED = new LinkedHashSet<>();
    private static boolean comboFired;

    private ComboTracker() {
    }

    /** @return true, если событие нужно съесть и не отдавать игре. */
    public static boolean onInput(InputConstants.Key key, int action) {
        return onInput(key, action, false);
    }

    /** simulated — нажатие из самопроверки: физически кнопка не зажата, «залипание» не проверяем. */
    static boolean onInput(InputConstants.Key key, int action, boolean simulated) {
        if (action != PRESS && action != RELEASE) {
            return false; // автоповтор
        }
        boolean pressed = action == PRESS;
        if (pressed) {
            PRESSED.add(key);
        } else {
            PRESSED.remove(key);
        }
        // клавиши могли «залипнуть», если отпустили их вне окна игры
        if (!simulated) PRESSED.removeIf(k -> k.getType() != InputConstants.Type.MOUSE && !k.equals(key) && !Compat.isKeyDown(k.getValue()));

        if (Compat.screen() instanceof CaptureListener listener && listener.isCapturing()) {
            listener.onCaptureInput(key, pressed);
            return true;
        }

        KeyCombo combo = CubeOverlay.config().combo();
        if (!pressed) {
            if (combo.contains(key)) {
                comboFired = false;
            }
            return false;
        }
        if (!comboFired && combo.contains(key) && combo.isHeld(PRESSED)) {
            comboFired = true;
            return toggleOverlay();
        }
        return false;
    }

    private static boolean toggleOverlay() {
        Screen current = Compat.screen();
        if (current instanceof OverlayScreen overlay) {
            overlay.onClose();
            return true;
        }
        if (current instanceof ChatScreen || (current != null && current.getFocused() instanceof EditBox)) {
            return false; // человек печатает — не мешаем
        }
        Compat.setScreen(new OverlayScreen(current));
        return true;
    }
}
