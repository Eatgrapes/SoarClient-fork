package com.soarclient.mixin.mixins.minecraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.soarclient.management.mod.impl.render.OverlayEditorMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public class MixinInGameOverlayRenderer {

    @Inject(method = "submitWater", at = @At("HEAD"), cancellable = true)
    private static void clearWater(Minecraft minecraft, PoseStack poseStack,
            SubmitNodeCollector collector, CallbackInfo ci) {
        OverlayEditorMod mod = OverlayEditorMod.getInstance();
        if (mod != null && mod.isEnabled() && mod.isClearWater()) {
            ci.cancel();
        }
    }

    @Inject(method = "submitFire", at = @At("HEAD"), cancellable = true)
    private static void clearFire(PoseStack poseStack, SubmitNodeCollector collector,
            TextureAtlasSprite sprite, CallbackInfo ci) {
        OverlayEditorMod mod = OverlayEditorMod.getInstance();
        if (mod != null && mod.isEnabled() && mod.isClearFire()) {
            ci.cancel();
        }
    }
}
