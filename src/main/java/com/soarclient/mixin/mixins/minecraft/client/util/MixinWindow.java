package com.soarclient.mixin.mixins.minecraft.client.util;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.platform.Window;
import com.soarclient.skia.context.SkiaContext;

@Mixin(Window.class)
public class MixinWindow {

	@Inject(method = "onFramebufferSizeChanged", at = @At("RETURN"))
	private void onFramebufferSizeChanged(long window, int width, int height, CallbackInfo ci) {
		SkiaContext.invalidate();
	}
}
