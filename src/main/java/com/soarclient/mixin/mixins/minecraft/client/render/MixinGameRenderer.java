package com.soarclient.mixin.mixins.minecraft.client.render;

import com.soarclient.event.EventBus;
import com.soarclient.event.client.RenderSkiaEvent;
import com.soarclient.event.client.RenderSkiaPostEvent;
import com.soarclient.skia.Skia;
import com.soarclient.skia.context.SkiaContext;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {

	@Inject(method = "render(Lnet/minecraft/client/DeltaTracker;Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render()V", shift = At.Shift.BEFORE))
	private void renderSkiaBeforeGui(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {
		if (!SkiaContext.beginFrame()) {
			SkiaContext.discardPending();
			return;
		}

		try {
			Skia.save();
			try {
				Skia.scale((float) Minecraft.getInstance().getWindow().getGuiScale());
				EventBus.getInstance().post(new RenderSkiaEvent());
			} finally {
				Skia.restore();
			}
		} finally {
			SkiaContext.endFrame();
		}
	}

	@Inject(method = "render(Lnet/minecraft/client/DeltaTracker;Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render()V", shift = At.Shift.AFTER))
	private void renderSkiaAfterGui(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {
		if (!SkiaContext.beginFrame()) {
			SkiaContext.discardPending();
			return;
		}

		try {
			SkiaContext.drawPending();
			Skia.save();
			try {
				Skia.scale((float) Minecraft.getInstance().getWindow().getGuiScale());
				EventBus.getInstance().post(new RenderSkiaPostEvent());
			} finally {
				Skia.restore();
			}
		} finally {
			SkiaContext.endFrame();
		}
	}
}
