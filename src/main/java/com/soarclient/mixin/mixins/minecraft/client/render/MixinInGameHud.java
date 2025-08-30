package com.soarclient.mixin.mixins.minecraft.client.render;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.soarclient.event.EventBus;
import com.soarclient.event.client.RenderGameOverlayEvent;
import com.soarclient.management.mod.impl.hud.PotionStatusMod;
import com.soarclient.management.mod.impl.player.OldAnimationsMod;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderTickCounter;

@Mixin(InGameHud.class)
public class MixinInGameHud {

    /**
     * Overrides the vanilla heart rendering method to support old animation mod features.
     * When the old animations mod is enabled and heart flashing is disabled, it will override the vanilla flashing logic.
     *@reason To provide customizable heart rendering behavior for old animation preferences
     * @param context The drawing context
     * @param type The type of heart to render
     * @param x The x coordinate to render at
     * @param y The y coordinate to render at
     * @param hardcore Whether in hardcore mode
     * @param blinking Whether the heart should blink (vanilla logic)
     * @param half Whether to render half a heart
     *
     *             @author EldoDebug
     */
	@Overwrite
	private void drawHeart(DrawContext context, InGameHud.HeartType type, int x, int y, boolean hardcore, boolean blinking, boolean half) {
		
    	OldAnimationsMod mod = OldAnimationsMod.getInstance();
    	
		context.drawGuiTexture(RenderLayer::getGuiTextured, type.getTexture(hardcore, half, mod.isEnabled() && mod.isDisableHeartFlash() ? false : blinking), x, y, 9, 9);
	}
    
	@Inject(method = "renderMainHud", at = @At("TAIL"))
	private void renderMainHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
		EventBus.getInstance().post(new RenderGameOverlayEvent(context));
	}

	@Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true)
	private void onRenderStatusEffectOverlay(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
		PotionStatusMod mod = PotionStatusMod.getInstance();
		if (mod != null && mod.shouldDisableVanillaDisplay()) {
			ci.cancel();
		}
	}
}
