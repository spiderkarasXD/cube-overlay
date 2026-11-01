package dev.cubeoverlay.compat;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/** Minecraft 1.21.11: GLFW, экран хранится прямо в Minecraft. */
public final class Compat {

    private Compat() {
    }

    @Nullable
    public static Screen screen() {
        return Minecraft.getInstance().screen;
    }

    public static void setScreen(@Nullable Screen screen) {
        Minecraft.getInstance().setScreen(screen);
    }

    public static boolean isKeyDown(int key) {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), key);
    }

    public static void actionBar(Component message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(message, true);
        }
    }

    /** Событие ввода символа (конструктор отличается между версиями). */
    public static CharacterEvent charEvent(int codepoint) {
        return new CharacterEvent(codepoint, 0);
    }

    /** Аппаратный scancode клавиши для Windows (GLFW на Windows отдаёт аппаратный scancode (бит 0x100 — «расширенная» клавиша)). */
    public static int windowsScancode(KeyEvent event) {
        return event.scancode() & 0xFF;
    }

    /** Событие клавиши для самопроверки: key — код Minecraft (InputConstants.KEY_*), pcScancode — scancode PC. */
    public static KeyEvent keyEvent(int key, int pcScancode) {
        return new KeyEvent(key, pcScancode, 0);
    }
}
