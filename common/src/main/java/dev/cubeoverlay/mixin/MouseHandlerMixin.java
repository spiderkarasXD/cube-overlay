package dev.cubeoverlay.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import dev.cubeoverlay.ComboTracker;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void cubeoverlay$onButton(long window, MouseButtonInfo info, int action, CallbackInfo ci) {
        if (ComboTracker.onInput(InputConstants.Type.MOUSE.getOrCreate(info.button()), action)) {
            ci.cancel();
        }
    }
}
