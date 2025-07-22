package com.soarclient.management.cape;

import com.soarclient.skia.Skia;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

public class CapeRenderer {

    public static void renderCapePreview(Identifier capeTexture, float x, float y, float width, float height) {
        if (capeTexture == null) return;

        int textureId = MinecraftClient.getInstance().getTextureManager().getTexture(capeTexture).getGlId();

        Skia.save();
        Skia.translate(x + 2, y + 8);
        Skia.scale(2f, 2f, 1f);
        renderCapeSection(textureId, 0, 0, 1, 1, 10, 16, 64, 32);
        Skia.restore();

        Skia.save();
        Skia.translate(x + 26, y + 8);
        Skia.scale(2f, 2f, 1f);
        renderCapeSection(textureId, 0, 0, 12, 1, 10, 16, 64, 32);
        Skia.restore();
    }

    private static void renderCapeSection(int textureId, float destX, float destY,
                                          float srcX, float srcY, float srcWidth, float srcHeight,
                                          float textureWidth, float textureHeight) {
        Skia.drawImage(textureId, destX, destY, srcWidth, srcHeight);
    }

    public static void renderRoundedCapePreview(Identifier capeTexture, float x, float y,
                                                float width, float height, float radius) {
        if (capeTexture == null) return;

        int textureId = MinecraftClient.getInstance().getTextureManager().getTexture(capeTexture).getGlId();
        Skia.drawRoundedImage(textureId, x, y, width, height, radius);
    }
}
