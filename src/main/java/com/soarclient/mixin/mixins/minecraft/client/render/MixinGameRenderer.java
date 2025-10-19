package com.soarclient.mixin.mixins.minecraft.client.render;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.soarclient.event.EventBus;
import com.soarclient.event.client.RenderSkiaEvent;
import com.soarclient.event.client.RenderSkiaPostEvent;
import com.soarclient.management.mod.impl.player.ZoomMod;
import com.soarclient.management.mod.impl.settings.HUDModSettings;
import com.soarclient.management.mod.impl.settings.ModMenuSettings;
import com.soarclient.shader.impl.KawaseBlur;
import com.soarclient.skia.Skia;
import com.soarclient.skia.context.SkiaContext;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", shift = At.Shift.BEFORE))
	public void render(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {

		if (HUDModSettings.getInstance().getBlurSetting().isEnabled()) {
			KawaseBlur.INGAME_BLUR.draw((int) HUDModSettings.getInstance().getBlurIntensitySetting().getValue());
		}

		SkiaContext.draw((context) -> {
			Skia.save();
			Skia.scale((float) MinecraftClient.getInstance().getWindow().getScaleFactor());
			EventBus.getInstance().post(new RenderSkiaEvent());
			Skia.restore();
		});
	}
	
	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", shift = At.Shift.AFTER))
	public void renderGuiBlur(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {

		if (HUDModSettings.getInstance().getBlurSetting().isEnabled()) {
			KawaseBlur.GUI_BLUR.draw((int) ModMenuSettings.getInstance().getBlurIntensitySetting().getValue());
		}

		// 在 HUD 渲染并应用 GUI blur 后执行一个 Skia 绘制事件，用于那些需要置顶显示的 HUD 元素
		SkiaContext.draw((context) -> {
			Skia.save();
			Skia.scale((float) MinecraftClient.getInstance().getWindow().getScaleFactor());
			EventBus.getInstance().post(new RenderSkiaPostEvent());
			Skia.restore();
		});
	}

	@Inject(method = "getFov", at = @At(value = "RETURN", ordinal = 1), cancellable = true)
	private void getFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> cir) {
		if (ZoomMod.getInstance().isEnabled()) {
			float value = cir.getReturnValue();
			value = ZoomMod.getInstance().getFov(value);
			cir.setReturnValue(value);
		}
	}
}
