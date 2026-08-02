package com.soarclient.mixin.mixins.minecraft.client.gui;

import java.util.Optional;
import java.util.function.Consumer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LoadingOverlay.class)
public abstract class MixinSplashScreen {

	@Shadow @Final private Minecraft minecraft;
	@Shadow @Final private boolean fadeIn;
	@Shadow @Final private Consumer<Optional<Throwable>> onFinish;
	@Unique private long soar_animationStartTime = -1L;
	@Unique private long soar_reloadStartTime = -1L;
	@Unique private static final long MAX_RELOAD_TIME = 15_000L;
	@Unique private static final Identifier CUSTOM_LOGO = Identifier.fromNamespaceAndPath("soar", "logo.png");
	@Unique private static final int LOGO_ACTUAL_SIZE = 1080;
	@Unique private static final float LOGO_SCALE = 0.15f;
	@Unique private static final long ANIMATION_TOTAL_TIME = 4500L;
	@Unique private static final long FADE_DURATION = 500L;
	@Unique private static final int PROGRESS_BAR_HEIGHT = 2;
	@Unique private static final int PROGRESS_BAR_BASE_COLOR = 0xFFFFFF;
	@Unique private static final int PROGRESS_BAR_BG_BASE_COLOR = 0x303030;
	@Unique private int lastWindowWidth = -1;
	@Unique private int lastWindowHeight = -1;
	@Unique private boolean skipNextFrame;

	@Unique
	private void ensureLogoTexture() {
		var textureManager = this.minecraft.getTextureManager();
		if (textureManager.getTexture(CUSTOM_LOGO) == null) {
			textureManager.registerAndLoad(CUSTOM_LOGO, new SimpleTexture(CUSTOM_LOGO));
		}
	}

	@Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
	private void soar_takeOverAndRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		int width = graphics.guiWidth();
		int height = graphics.guiHeight();
		if (this.lastWindowWidth != -1 && this.lastWindowHeight != -1
				&& (width != this.lastWindowWidth || height != this.lastWindowHeight)) {
			this.skipNextFrame = true;
		}
		this.lastWindowWidth = width;
		this.lastWindowHeight = height;
		if (this.skipNextFrame || width <= 0 || height <= 0) {
			this.skipNextFrame = false;
			return;
		}

		ci.cancel();
		this.ensureLogoTexture();
		if (this.fadeIn) {
			this.renderReloading(graphics, width, height);
		} else {
			this.renderInitial(graphics, width, height);
		}
	}

	@Unique
	private void renderReloading(GuiGraphicsExtractor graphics, int width, int height) {
		if (this.soar_reloadStartTime == -1L) {
			this.soar_reloadStartTime = Util.getMillis();
		}
		this.soar_animationStartTime = -1L;
		long elapsed = Util.getMillis() - this.soar_reloadStartTime;
		if (elapsed > MAX_RELOAD_TIME) {
			this.minecraft.gui.setOverlay(null);
			this.onFinish.accept(Optional.empty());
			this.soar_reloadStartTime = -1L;
			return;
		}

		graphics.fill(0, 0, width, height, 0xFF000000);
		this.blitLogo(graphics, width, height, 1.0F);
		long cycle = 1500L;
		float progress = (float)(Util.getMillis() % cycle) / cycle;
		int barWidth = Math.max(1, width / 3);
		int start = (int)((width + barWidth) * progress) - barWidth;
		int end = start + barWidth;
		int barY = height - PROGRESS_BAR_HEIGHT;
		graphics.fill(0, barY, width, height, 0xFF303030);
		graphics.fill(Math.max(0, start), barY, Math.min(width, end), height, 0xFFFFFFFF);
	}

	@Unique
	private void renderInitial(GuiGraphicsExtractor graphics, int width, int height) {
		this.soar_reloadStartTime = -1L;
		if (this.soar_animationStartTime == -1L) {
			this.soar_animationStartTime = Util.getMillis();
		}
		long elapsed = Util.getMillis() - this.soar_animationStartTime;
		if (elapsed >= ANIMATION_TOTAL_TIME) {
			this.minecraft.gui.setOverlay(null);
			this.onFinish.accept(Optional.empty());
			this.soar_animationStartTime = -1L;
			return;
		}

		float alpha = 1.0F;
		long fadeStart = ANIMATION_TOTAL_TIME - FADE_DURATION;
		if (elapsed > fadeStart) {
			alpha = 1.0F - (float)(elapsed - fadeStart) / FADE_DURATION;
		}
		alpha = Mth.clamp(alpha, 0.0F, 1.0F);
		graphics.fill(0, 0, width, height, 0xFF000000);
		this.blitLogo(graphics, width, height, alpha);

		int barY = height - PROGRESS_BAR_HEIGHT;
		int progressWidth = (int)(width * Math.min(1.0F, (float)elapsed / ANIMATION_TOTAL_TIME));
		graphics.fill(0, barY, width, height, 0xFF303030);
		graphics.fill(0, barY, progressWidth, height, ((int)(alpha * 255.0F) << 24) | PROGRESS_BAR_BASE_COLOR);
	}

	@Unique
	private void blitLogo(GuiGraphicsExtractor graphics, int width, int height, float alpha) {
		int scaledSize = (int)(LOGO_ACTUAL_SIZE * LOGO_SCALE);
		int logoX = (width - scaledSize) / 2;
		int logoY = (height - scaledSize) / 2;
		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		try {
			pose.translate(logoX + scaledSize / 2.0F, logoY + scaledSize / 2.0F);
			pose.scale(LOGO_SCALE);
			pose.translate(-LOGO_ACTUAL_SIZE / 2.0F, -LOGO_ACTUAL_SIZE / 2.0F);
			graphics.blit(
					RenderPipelines.GUI_TEXTURED,
					CUSTOM_LOGO,
					0, 0, 0.0F, 0.0F,
					LOGO_ACTUAL_SIZE, LOGO_ACTUAL_SIZE,
					LOGO_ACTUAL_SIZE, LOGO_ACTUAL_SIZE,
					((int)(alpha * 255.0F) << 24) | 0xFFFFFF
			);
		} finally {
			pose.popMatrix();
		}
	}
}
