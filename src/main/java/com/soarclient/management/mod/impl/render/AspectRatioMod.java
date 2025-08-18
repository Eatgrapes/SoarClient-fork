package com.soarclient.management.mod.impl.render;

import com.soarclient.management.mod.Mod;
import com.soarclient.management.mod.ModCategory;
import com.soarclient.management.mod.settings.impl.NumberSetting;
import com.soarclient.skia.font.Icon;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.opengl.GL11;

public class AspectRatioMod extends Mod {
    private final NumberSetting widthRatio;
    private final NumberSetting heightRatio;
    private static AspectRatioMod instance;

    public AspectRatioMod() {
        super("Aspect Ratio", "AspectRatio", Icon.SCREEN_SHARE, ModCategory.RENDER);
        this.widthRatio = new NumberSetting("宽度", "设置画面宽度比例", "width", this, 16, 4, 21, 1);
        this.heightRatio = new NumberSetting("高度", "设置画面高度比例", "height", this, 9, 3, 10, 1);
        instance = this;
    }

    public static AspectRatioMod getInstance() {
        return instance;
    }

    private float getTargetAspectRatio() {
        if (!isEnabled()) return 0;
        return widthRatio.getValue() / heightRatio.getValue();
    }

    public void applyAspectRatio(int width, int height) {
        if (!isEnabled()) return;

        float targetRatio = getTargetAspectRatio();
        if (targetRatio == 0) {
            resetViewport(width, height);
            return;
        }

        float currentRatio = (float) width / height;

        if (currentRatio > targetRatio) {
            int newWidth = (int) (height * targetRatio);
            int deltaWidth = width - newWidth;
            GL11.glViewport(deltaWidth / 2, 0, newWidth, height);
        } else {
            int newHeight = (int) (width / targetRatio);
            int deltaHeight = height - newHeight;
            GL11.glViewport(0, deltaHeight / 2, width, newHeight);
        }
    }

    public void resetViewport(int width, int height) {
        GL11.glViewport(0, 0, width, height);
    }

    @Override
    public void onEnable() {
        // 强制重新初始化窗口大小
        if (MinecraftClient.getInstance().getWindow() != null) {
            MinecraftClient.getInstance().getWindow().setScaleFactor(
                MinecraftClient.getInstance().getWindow().getScaleFactor()
            );
        }
        // 在渲染循环的适当位置添加（例如在游戏的渲染管理器中）
        AspectRatioMod.getInstance().applyAspectRatio(
            MinecraftClient.getInstance().getWindow().getWidth(),
            MinecraftClient.getInstance().getWindow().getHeight()
        );
    }

    @Override
    public void onDisable() {
        // 重置视口为全屏
        if (MinecraftClient.getInstance().getWindow() != null) {
            resetViewport(
                MinecraftClient.getInstance().getWindow().getWidth(),
                MinecraftClient.getInstance().getWindow().getHeight()
            );
        }
    }
}
