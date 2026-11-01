package dev.cubeoverlay.compat;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/** Minecraft 26.3: экраны переехали в Minecraft.gui, ввод — на SDL. */
public final class Compat {

    private Compat() {
    }

    @Nullable
    public static Screen screen() {
        return Minecraft.getInstance().gui.screen();
    }

    public static void setScreen(@Nullable Screen screen) {
        Minecraft.getInstance().gui.setScreen(screen);
    }

    public static boolean isKeyDown(int key) {
        return InputConstants.isKeyDown(key);
    }

    public static void actionBar(Component message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.sendOverlayMessage(message);
        }
    }

    /** Событие ввода символа (конструктор отличается между версиями). */
    public static CharacterEvent charEvent(int codepoint) {
        return new CharacterEvent(codepoint);
    }

    /** Аппаратный scancode клавиши для Windows (SDL отдаёт USB HID-коды — переводим в PC scancode). */
    public static int windowsScancode(KeyEvent event) {
        return dev.cubeoverlay.BrowserKeys.hidToSet1(event.key());
    }

    /** Событие клавиши для самопроверки: key — код Minecraft (InputConstants.KEY_*), pcScancode — scancode PC. */
    public static KeyEvent keyEvent(int key, int pcScancode) {
        return new KeyEvent(key, 0, 0);
    }
}
