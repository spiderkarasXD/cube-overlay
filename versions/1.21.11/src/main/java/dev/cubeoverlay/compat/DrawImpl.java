package dev.cubeoverlay.compat;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.cubeoverlay.Draw;
import net.dimaskama.mcef.api.MCEFBrowser;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.render.state.BlitRenderState;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;

public final class DrawImpl implements Draw {

    private final GuiGraphics g;
    private final Font font;

    public DrawImpl(GuiGraphics g, Font font) {
        this.g = g;
        this.font = font;
    }

    @Override
    public Font font() {
        return font;
    }

    @Override
    public Matrix3x2fStack pose() {
        return g.pose();
    }

    @Override
    public void fill(int x1, int y1, int x2, int y2, int color) {
        g.fill(x1, y1, x2, y2, color);
    }

    @Override
    public void outline(int x, int y, int width, int height, int color) {
        g.renderOutline(x, y, width, height, color);
    }

    @Override
    public void text(String text, int x, int y, int color, boolean shadow) {
        g.drawString(font, text, x, y, color, shadow);
    }

    @Override
    public void browser(MCEFBrowser browser, int x, int y, int width, int height) {
        GpuTextureView view = browser.getTextureView();
        if (view == null) {
            return;
        }
        g.guiRenderState.submitGuiElement(new BlitRenderState(
                RenderPipelines.GUI_TEXTURED,
                TextureSetup.singleTexture(view, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)),
                new Matrix3x2f(g.pose()),
                x, y, x + width, y + height,
                0.0F, 1.0F, 0.0F, 1.0F,
                0xFFFFFFFF,
                g.scissorStack.peek()
        ));
    }

    @Override
    public void browserCursor(MCEFBrowser browser) {
        g.requestCursor(browser.getCursorType());
    }
}
