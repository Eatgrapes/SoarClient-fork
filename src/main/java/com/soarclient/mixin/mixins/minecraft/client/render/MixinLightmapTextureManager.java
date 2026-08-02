package com.soarclient.mixin.mixins.minecraft.client.render;

import com.soarclient.management.mod.impl.render.FullbrightMod;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapRenderStateExtractor.class)
public class MixinLightmapTextureManager {

    @Inject(method = "extract", at = @At("TAIL"))
    private void applyFullbright(LightmapRenderState state, float partialTicks, CallbackInfo ci) {
        FullbrightMod mod = FullbrightMod.getInstance();
        if (mod != null && mod.isEnabled()) {
            state.brightness = mod.getGamma();
            state.needsUpdate = true;
        }
    }
}
