package com.soarclient.mixin.mixins.minecraft.client.render;

import com.soarclient.Soar;
import com.soarclient.management.color.api.ColorPalette;
import com.soarclient.management.mod.impl.render.BlockOverlayMod;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(WorldRenderer.class)
public class MixinWorldRendererBlockOutline {

    @ModifyArg(method = "drawBlockOutline", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/VertexRendering;drawOutline(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/util/shape/VoxelShape;DDDI)V"), index = 6)
    private int changeColors(int color) {
        BlockOverlayMod mod = BlockOverlayMod.getInstance();
        if (mod != null && mod.isEnabled()) {
            return getGradientColor();
        }
        return color;
    }

    private int getGradientColor() {
        ColorPalette palette = Soar.getInstance().getColorManager().getPalette();

        long currentTime = System.nanoTime();
        double speed = 0.000000002;
        double cycle = (currentTime * speed) % (2 * Math.PI);

        java.awt.Color color1 = palette.getPrimary();
        java.awt.Color color2 = palette.getSecondary();
        java.awt.Color color3 = palette.getTertiary();

        if (color1 == null) color1 = java.awt.Color.WHITE;
        if (color2 == null) color2 = java.awt.Color.LIGHT_GRAY;
        if (color3 == null) color3 = java.awt.Color.GRAY;

        double normalizedCycle = (cycle / (2 * Math.PI)) * 3;

        java.awt.Color resultColor;
        if (normalizedCycle < 1) {
            float factor = (float) normalizedCycle;
            resultColor = blendColors(color1, color2, factor);
        } else if (normalizedCycle < 2) {
            float factor = (float) (normalizedCycle - 1);
            resultColor = blendColors(color2, color3, factor);
        } else {
            float factor = (float) (normalizedCycle - 2);
            resultColor = blendColors(color3, color1, factor);
        }

        return resultColor.getRGB();
    }

    private java.awt.Color blendColors(java.awt.Color color1, java.awt.Color color2, float factor) {
        int red = (int) (color1.getRed() * (1 - factor) + color2.getRed() * factor);
        int green = (int) (color1.getGreen() * (1 - factor) + color2.getGreen() * factor);
        int blue = (int) (color1.getBlue() * (1 - factor) + color2.getBlue() * factor);
        int alpha = (int) (color1.getAlpha() * (1 - factor) + color2.getAlpha() * factor);

        return new java.awt.Color(red, green, blue, alpha);
    }
}
