package com.soarclient.mixin.mixins.viafabricplus;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.soarclient.management.mod.impl.player.OldAnimationsMod;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = Minecraft.class, priority = 2000)
public class MixinMinecraftClient {

    @ModifyExpressionValue(
        method = "startUseItem",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;isDestroying()Z")
    )
    private boolean allowOldUseAnimation(boolean original) {
        OldAnimationsMod mod = OldAnimationsMod.getInstance();
        return mod != null && mod.isEnabled() ? false : original;
    }
}
