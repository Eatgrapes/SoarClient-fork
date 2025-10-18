package com.soarclient.mixin.mixins.minecraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.function.Consumer;

@Mixin(SplashOverlay.class)
public abstract class MixinSplashScreen {

    @Shadow @Final private MinecraftClient client;
    @Shadow @Final private boolean reloading;
    @Shadow @Final private Consumer<Optional<Throwable>> exceptionHandler;
    @Unique private long soar_animationStartTime = -1L;
    @Unique private long soar_reloadStartTime = -1L;
    @Unique private static final long MAX_RELOAD_TIME = 15_000L;
    @Unique private static final Identifier CUSTOM_LOGO = Identifier.of("soar", "logo.png");
    @Unique private static final int LOGO_ACTUAL_SIZE = 1080;
    @Unique private static final float LOGO_SCALE = 0.15f;
    @Unique private static final long ANIMATION_TOTAL_TIME = 4500L;
    @Unique private static final long FADE_DURATION = 500L;
    @Unique private static final int PROGRESS_BAR_HEIGHT = 2;
    @Unique private static final int PROGRESS_BAR_BASE_COLOR = 0xFFFFFF;
    @Unique private static final int PROGRESS_BAR_BG_BASE_COLOR = 0x303030;
    @Unique private int lastWindowWidth = -1;
    @Unique private int lastWindowHeight = -1;
    @Unique private boolean skipNextFrame = false;

    @Unique
    private void ensureLogoTexture() {
        var tm = this.client.getTextureManager();
        if (tm.getTexture(CUSTOM_LOGO) == null) {
            tm.registerTexture(CUSTOM_LOGO, new ResourceTexture(CUSTOM_LOGO));
        }
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void soar_takeOverAndRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        if (lastWindowWidth != -1 && lastWindowHeight != -1 &&
            (width != lastWindowWidth || height != lastWindowHeight)) {
            skipNextFrame = true;
        }

        lastWindowWidth = width;
        lastWindowHeight = height;

        if (skipNextFrame || width <= 0 || height <= 0) {
            skipNextFrame = false;
            return;
        }

        ci.cancel();


        ensureLogoTexture();

        if (this.reloading) {
            if (this.soar_reloadStartTime == -1L) this.soar_reloadStartTime = Util.getMeasuringTimeMs();
            this.soar_animationStartTime = -1L;

            long reloadElapsed = Util.getMeasuringTimeMs() - this.soar_reloadStartTime;
            if (reloadElapsed > MAX_RELOAD_TIME) {
                try {
                    this.client.setOverlay(null);
                    this.exceptionHandler.accept(Optional.empty());
                } catch (Exception ignored) {}
                this.soar_reloadStartTime = -1L;
                return;
            }

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            try {
                // 背景
                context.fill(0, 0, width, height, 0xFF000000);

                // Logo
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                context.getMatrices().push();
                try {
                    int scaledSize = (int)(LOGO_ACTUAL_SIZE * LOGO_SCALE);
                    int logoX = (width - scaledSize) / 2;
                    int logoY = (height - scaledSize) / 2;

                    context.getMatrices().translate(logoX + scaledSize / 2f, logoY + scaledSize / 2f, 0);
                    context.getMatrices().scale(LOGO_SCALE, LOGO_SCALE, 1f);
                    context.getMatrices().translate(-LOGO_ACTUAL_SIZE / 2f, -LOGO_ACTUAL_SIZE / 2f, 0);

                    context.drawTexture(
                        RenderLayer::getGuiTextured,
                        CUSTOM_LOGO,
                        0, 0, 0, 0,
                        LOGO_ACTUAL_SIZE, LOGO_ACTUAL_SIZE,
                        LOGO_ACTUAL_SIZE, LOGO_ACTUAL_SIZE
                    );
                } finally {
                    context.getMatrices().pop();
                }

                //进度条
                long cycle = 1500L;
                float p = (float)(Util.getMeasuringTimeMs() % cycle) / (float)cycle;
                int barWidth = Math.max(1, width / 3);
                int start = (int)((width + barWidth) * p) - barWidth;
                int end = start + barWidth;

                int bgColor = (0xFF << 24) | PROGRESS_BAR_BG_BASE_COLOR;
                int progressBarY = height - PROGRESS_BAR_HEIGHT;
                context.fill(0, progressBarY, width, height, bgColor);

                int fgColor = (0xFF << 24) | PROGRESS_BAR_BASE_COLOR;
                context.fill(Math.max(0, start), progressBarY, Math.min(width, end), height, fgColor);

            } finally {
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                RenderSystem.disableBlend();
            }
            return;
        }

        this.soar_reloadStartTime = -1L;
        if (this.soar_animationStartTime == -1L) {
            this.soar_animationStartTime = Util.getMeasuringTimeMs();
        }

        long timePassed = Util.getMeasuringTimeMs() - this.soar_animationStartTime;
        if (timePassed >= ANIMATION_TOTAL_TIME) {
            try {
                this.client.setOverlay(null);
                this.exceptionHandler.accept(Optional.empty());
            } catch (Exception ignored) {}
            this.soar_animationStartTime = -1L;
            return;
        }

        float alpha = 1f;
        long fadeStartTime = ANIMATION_TOTAL_TIME - FADE_DURATION;
        if (timePassed > fadeStartTime) {
            long fadeTimePassed = timePassed - fadeStartTime;
            alpha = 1f - (float)fadeTimePassed / FADE_DURATION;
        }
        alpha = Math.max(0f, alpha);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        try {
            context.fill(0, 0, width, height, 0xFF000000);

            // Logo
            RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
            context.getMatrices().push();
            try {
                int scaledSize = (int)(LOGO_ACTUAL_SIZE * LOGO_SCALE);
                int logoX = (width - scaledSize) / 2;
                int logoY = (height - scaledSize) / 2;

                context.getMatrices().translate(logoX + scaledSize / 2f, logoY + scaledSize / 2f, 0);
                context.getMatrices().scale(LOGO_SCALE, LOGO_SCALE, 1f);
                context.getMatrices().translate(-LOGO_ACTUAL_SIZE / 2f, -LOGO_ACTUAL_SIZE / 2f, 0);

                context.drawTexture(
                    RenderLayer::getGuiTextured,
                    CUSTOM_LOGO,
                    0, 0, 0, 0,
                    LOGO_ACTUAL_SIZE, LOGO_ACTUAL_SIZE,
                    LOGO_ACTUAL_SIZE, LOGO_ACTUAL_SIZE
                );
            } finally {
                context.getMatrices().pop();
            }

            // 进度条
            float progress = Math.min(1f, (float) timePassed / ANIMATION_TOTAL_TIME);
            int progressBarY = height - PROGRESS_BAR_HEIGHT;
            int progressWidth = (int) (width * progress);

            int bgColor = (0xFF << 24) | PROGRESS_BAR_BG_BASE_COLOR;
            context.fill(0, progressBarY, width, height, bgColor);

            int fgColor = ((int)(alpha * 255f) << 24) | PROGRESS_BAR_BASE_COLOR;
            context.fill(0, progressBarY, progressWidth, height, fgColor);

        } finally {
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.disableBlend();
        }
    }
}
