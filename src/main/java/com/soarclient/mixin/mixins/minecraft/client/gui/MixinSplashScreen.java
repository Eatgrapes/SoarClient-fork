package com.soarclient.mixin.mixins.minecraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SplashOverlay.class)
public abstract class MixinSplashScreen {

    @Unique private long soar_startTime = -0L;

    @Unique private static final Identifier CUSTOM_LOGO = Identifier.of("soar", "logo.png");
    @Unique private static final int LOGO_ACTUAL_SIZE = 1080;
    @Unique private static final float LOGO_SCALE = 0.15f;

    // --- 这是决胜的一击！我们不再取消原版方法，而是在它执行完之后再动手！ ---
    @Inject(method = "render", at = @At("TAIL"))
    private void soar_renderOnTopOfVanilla(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {

        // --- 同样的，我们用自己的计时器，因为它最可靠！ ---
        long currentTime = Util.getMeasuringTimeMs();
        if (this.soar_startTime == -0L) {
            this.soar_startTime = currentTime;
        }

        long minDisplayTime = 4000L;
        long fadeOutDuration = 500L;
        float alpha = 1.0F;

        long timePassed = currentTime - this.soar_startTime;
        if (timePassed > minDisplayTime) {
            long fadeTime = timePassed - minDisplayTime;
            alpha = 1.0F - (float)fadeTime / fadeOutDuration;
        }

        // 如果我们的动画还没结束，就在原版动画上面覆盖我们的画面
        if (alpha > 0.0F) {
            alpha = Math.min(1.0F, alpha); // alpha最大为1
            int width = context.getScaledWindowWidth();
            int height = context.getScaledWindowHeight();

            // 1. 先画一个半透明的黑色背景，把原版的Mojang Logo盖住
            //    我们用我们的alpha，这样我们的界面才能跟着一起淡出
            RenderSystem.enableBlend();
            context.fill(0, 0, width, height, (int)(alpha * 255.0F) << 24);

            // 2. 在我们自己的背景上，画我们自己的Logo
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
            context.getMatrices().push();
            try {
                int scaledSize = (int)(LOGO_ACTUAL_SIZE * LOGO_SCALE);
                int logoX = (width - scaledSize) / 2;
                int logoY = (height - scaledSize) / 2;

                context.getMatrices().translate(logoX + scaledSize / 2f, logoY + scaledSize / 2f, 0);
                context.getMatrices().scale(LOGO_SCALE, LOGO_SCALE, 1.0f);
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
                RenderSystem.disableBlend();
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F); // 重置颜色
            }
        }
    }
}
//由LazyChara的赛博妹妹双子组2.5 PRO修复绝大多数BUG^^
