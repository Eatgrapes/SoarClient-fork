package com.soarclient.mixin.mixins.minecraft.client.render;

import com.soarclient.Soar;
import com.soarclient.event.EventBus;
import com.soarclient.event.client.RenderGameOverlayEvent;
import com.soarclient.event.client.RenderHotbarEvent;
import com.soarclient.event.impl.Render3DEvent;
import com.soarclient.management.mod.impl.hud.ModernHotBarMod;
import com.soarclient.management.mod.impl.hud.PotionStatusMod;
import com.soarclient.management.mod.impl.player.OldAnimationsMod;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public class MixinInGameHud {

    @ModifyArg(
        method = "extractHearts",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Hud;extractHeart(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Hud$HeartType;IIZZZ)V"),
        index = 5
    )
    private boolean disableHeartFlash(boolean blinking) {
        OldAnimationsMod mod = OldAnimationsMod.getInstance();
        return mod != null && mod.isEnabled() && mod.isDisableHeartFlash() ? false : blinking;
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
        EventBus.getInstance().post(new Render3DEvent(partialTick, graphics));
        EventBus.getInstance().post(new RenderGameOverlayEvent(graphics));
    }

    @Inject(method = "extractEffects", at = @At("HEAD"), cancellable = true)
    private void extractEffects(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        PotionStatusMod mod = PotionStatusMod.getInstance();
        if (mod != null && mod.shouldDisableVanillaDisplay()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractItemHotbar", at = @At("HEAD"), cancellable = true)
    private void extractItemHotbar(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        RenderHotbarEvent event = new RenderHotbarEvent(graphics, deltaTracker.getGameTimeDeltaPartialTick(false));
        EventBus.getInstance().post(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractPlayerHealth", at = @At("HEAD"), cancellable = true)
    private void extractPlayerHealth(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        ModernHotBarMod mod = Soar.getInstance().getModManager().getMod(ModernHotBarMod.class);
        if (mod != null && mod.isEnabled()) {
            ci.cancel();
        }
    }
}
