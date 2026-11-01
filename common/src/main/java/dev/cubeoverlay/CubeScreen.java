package dev.cubeoverlay;

import dev.cubeoverlay.compat.VersionedScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Экран со своим масштабом: оверлей не раздувается вместе с «Масштабом интерфейса» Minecraft.
 * width/height здесь — в собственных единицах (1 единица = pixelScale пикселей экрана),
 * а координаты мыши из событий надо пропускать через scaled()/vx().
 */
public abstract class CubeScreen extends VersionedScreen {

    /** Сколько пикселей экрана в одной единице интерфейса оверлея. */
    protected int pixelScale = 2;

    protected CubeScreen(Component title) {
        super(title);
    }

    /** Вызывать первой строкой в init(). */
    protected void layoutScale() {
        pixelScale = pixelScale(CubeOverlay.config().uiScale);
        width = Math.max(1, minecraft.getWindow().getWidth() / pixelScale);
        height = Math.max(1, minecraft.getWindow().getHeight() / pixelScale);
    }

    /** 0 = авто: ×1 до ~750p, ×2 для 1080p, ×3 для 1440p, ×4 для 4K. */
    public static int pixelScale(int setting) {
        if (setting > 0) return setting;
        int h = net.minecraft.client.Minecraft.getInstance().getWindow().getHeight();
        return Math.max(1, Math.round(h / 500f));
    }

    @Override
    protected float uiScale() {
        return pixelScale / (float) minecraft.getWindow().getGuiScale();
    }

    protected double vx(double guiX) {
        return guiX / uiScale();
    }

    protected MouseButtonEvent scaled(MouseButtonEvent event) {
        float s = uiScale();
        return new MouseButtonEvent(event.x() / s, event.y() / s, event.buttonInfo());
    }
}
