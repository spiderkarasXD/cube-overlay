package dev.cubeoverlay.compat;

import dev.cubeoverlay.Draw;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Мост между общим кодом экранов и рендером 1.21.11 (render/GuiGraphics). */
public abstract class VersionedScreen extends Screen {

    protected VersionedScreen(Component title) {
        super(title);
    }

    /** Затемнение поверх размытого мира. */
    protected abstract void drawBackdrop(Draw d);

    /** Панели под кнопками. */
    protected abstract void drawContents(Draw d, int mouseX, int mouseY, float partialTick);

    /** Поверх кнопок. */
    protected void drawForeground(Draw d, int mouseX, int mouseY, float partialTick) {
    }

    /** Во сколько раз рисовать крупнее/мельче GUI-масштаба Minecraft (свой масштаб оверлея). */
    protected float uiScale() {
        return 1f;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (minecraft.level == null) {
            super.renderBackground(g, mouseX, mouseY, partialTick);
        } else {
            renderBlurredBackground(g);
        }
        float s = uiScale();
        g.pose().pushMatrix();
        g.pose().scale(s, s);
        drawBackdrop(new DrawImpl(g, font));
        g.pose().popMatrix();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        float s = uiScale();
        int mx = (int) (mouseX / s);
        int my = (int) (mouseY / s);
        g.pose().pushMatrix();
        g.pose().scale(s, s);
        DrawImpl d = new DrawImpl(g, font);
        drawContents(d, mx, my, partialTick);
        super.render(g, mx, my, partialTick);
        drawForeground(d, mx, my, partialTick);
        g.pose().popMatrix();
    }
}
