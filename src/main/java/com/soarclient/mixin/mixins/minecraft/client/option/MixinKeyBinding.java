package com.soarclient.mixin.mixins.minecraft.client.option;

import com.mojang.blaze3d.platform.InputConstants;
import com.soarclient.management.mod.impl.player.SnapTapMod;
import com.soarclient.mixin.interfaces.IMixinKeyBinding;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyMapping.class)
public class MixinKeyBinding implements IMixinKeyBinding {

    @Shadow @Final private InputConstants.Key defaultKey;
    @Shadow private boolean isDown;

    @Inject(method = "isDown", at = @At("HEAD"), cancellable = true)
    private void applySnapTap(CallbackInfoReturnable<Boolean> cir) {
        SnapTapMod mod = SnapTapMod.getInstance();
        if (mod == null || !mod.isEnabled() || !isDown) {
            return;
        }

        int key = defaultKey.getValue();
        if (key == InputConstants.KEY_A) {
            cir.setReturnValue(mod.getRightPressTime() == 0 || mod.getRightPressTime() <= mod.getLeftPressTime());
        } else if (key == InputConstants.KEY_D) {
            cir.setReturnValue(mod.getLeftPressTime() == 0 || mod.getLeftPressTime() <= mod.getRightPressTime());
        } else if (key == InputConstants.KEY_W) {
            cir.setReturnValue(mod.getBackPressTime() == 0 || mod.getBackPressTime() <= mod.getForwardPressTime());
        } else if (key == InputConstants.KEY_S) {
            cir.setReturnValue(mod.getForwardPressTime() == 0 || mod.getForwardPressTime() <= mod.getBackPressTime());
        }
    }

    @Inject(method = "setDown", at = @At("HEAD"))
    private void recordPressTime(boolean down, CallbackInfo ci) {
        SnapTapMod mod = SnapTapMod.getInstance();
        if (mod == null || !mod.isEnabled()) {
            return;
        }

        long time = down ? System.currentTimeMillis() : 0;
        int key = defaultKey.getValue();
        if (key == InputConstants.KEY_A) {
            mod.setLeftPressTime(time);
        } else if (key == InputConstants.KEY_D) {
            mod.setRightPressTime(time);
        } else if (key == InputConstants.KEY_W) {
            mod.setForwardPressTime(time);
        } else if (key == InputConstants.KEY_S) {
            mod.setBackPressTime(time);
        }
    }

    @Override
    public boolean getRealIsPressed() {
        return isDown;
    }
}
