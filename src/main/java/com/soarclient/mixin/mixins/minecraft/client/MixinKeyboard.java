package com.soarclient.mixin.mixins.minecraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.soarclient.Soar;
import com.soarclient.management.mod.settings.impl.KeybindSetting;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class MixinKeyboard {

    @Inject(method = "keyPress", at = @At("TAIL"))
    private void handleKey(long window, int action, KeyEvent event, CallbackInfo ci) {
        InputConstants.Key key = InputConstants.getKey(event);
        boolean down = action != GLFW.GLFW_RELEASE;
        for (KeybindSetting setting : Soar.getInstance().getModManager().getKeybindSettings()) {
            if (setting.getKey().equals(key)) {
                if (action == GLFW.GLFW_PRESS) {
                    setting.setPressed();
                }
                setting.setKeyDown(down);
            }
        }
    }
}
