package dev.cubeoverlay.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import dev.cubeoverlay.ComboTracker;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void cubeoverlay$onKey(long window, int action, KeyEvent event, CallbackInfo ci) {
        if (ComboTracker.onInput(InputConstants.getKey(event), action)) {
            ci.cancel();
        }
    }
}
