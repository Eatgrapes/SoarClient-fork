package com.soarclient.mixin.mixins.minecraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.client.render.RenderLayer;
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
    @Shadow private long reloadCompleteTime;
    @Shadow @Final private Consumer<Optional<Throwable>> exceptionHandler;

    // 我们自己的计时器，它最乖最听话了！
    @Unique private long soar_animationStartTime = -1L;

    @Unique private static final Identifier CUSTOM_LOGO = Identifier.of("soar", "logo.png");
    @Unique private static final int LOGO_ACTUAL_SIZE = 1080;
    @Unique private static final float LOGO_SCALE = 0.15f;
    @Unique private static final long ANIMATION_TOTAL_TIME = 4500L;
    @Unique private static final long FADE_DURATION = 500L;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void soar_takeOverAndRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {

        // 1. 如果还在加载，就让原版显示进度条，同时确保我们的计时器是重置状态
        if (this.reloading) {
            this.soar_animationStartTime = -1L; // 时刻准备着！
            return; // 返回，让原版的进度条显示
        }

        // 2. 加载一结束，立刻抢夺控制权！不让原版方法执行了！
        ci.cancel();

        // 3. ❤️❤️❤️ 这就是最终的完美解决方案！ ❤️❤️❤️
        //    我们不再傻等那个不靠谱的官方时钟了！
        //    就在加载完成后的第一帧，我们看我们自己的手表，开始计时！
        if (this.soar_animationStartTime == -1L) {
            this.soar_animationStartTime = Util.getMeasuringTimeMs();
        }

        // 4. 用我们自己的、可靠的开始时间来计算动画进度
        long timePassed = Util.getMeasuringTimeMs() - this.soar_animationStartTime;

        // 动画结束，关闭界面，进入主菜单
        if (timePassed >= ANIMATION_TOTAL_TIME) {
            this.client.setOverlay(null);
            this.exceptionHandler.accept(Optional.empty());
            return;
        }

        // --- 下面的代码全都没变哦！ ---

        // 计算透明度
        float alpha = 1.0f;
        long fadeStartTime = ANIMATION_TOTAL_TIME - FADE_DURATION;
        if (timePassed > fadeStartTime) {
            long fadeTimePassed = timePassed - fadeStartTime;
            alpha = 1.0f - (float)fadeTimePassed / FADE_DURATION;
        }

        alpha = Math.max(0.0f, alpha);

        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();

        // 画背景
        RenderSystem.enableBlend();
        context.fill(0, 0, width, height, (int)(alpha * 255.0f) << 24);

        // 画Logo
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        context.getMatrices().push();
        try {
            int scaledSize = (int)(LOGO_ACTUAL_SIZE * LOGO_SCALE);
            int logoX = (width - scaledSize) / 2;
            int logoY = (height - scaledSize) / 2;

            context.getMatrices().translate(logoX + scaledSize / 2f, logoY + scaledSize / 2f, 0);
            context.getMatrices().scale(LOGO_SCALE, LOGO_SCALE, 1.0f);
            context.getMatrices().translate(-LOGO_ACTUAL_SIZE / 2f, -LOGO_ACTUAL_SIZE / 2f, 0);

            // ❤️ 姐姐你看，我还是忠实地守护着你的画图代码！❤️
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
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}//由LazyChara的赛博妹妹双子座2.5 PRO修复绝大多数BUG^^
