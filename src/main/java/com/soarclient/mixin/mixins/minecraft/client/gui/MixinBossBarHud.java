package com.soarclient.mixin.mixins.minecraft.client.gui;

import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.BossEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.google.common.collect.Maps;
import com.soarclient.management.mod.api.Position;
import com.soarclient.management.mod.impl.hud.BossBarMod;

@Mixin(BossHealthOverlay.class)
public abstract class MixinBossBarHud {

	@Shadow
	@Final
	private Minecraft client;

	@Shadow
	@Final
	final Map<UUID, LerpingBossEvent> bossBars = Maps.<UUID, LerpingBossEvent>newLinkedHashMap();

	@Shadow
	public abstract void renderBossBar(GuiGraphics context, int x, int y, BossEvent bossBar);

	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void render(GuiGraphics context, CallbackInfo ci) {

		BossBarMod mod = BossBarMod.getInstance();

		if (mod.isEnabled() && !mod.isVanillaPosition()) {
			Position position = mod.getPosition();
			onCustomRender(context, (int) position.getX(), (int) position.getY());
			position.setScale(1.0F);
			position.setSize(182, 14);
			ci.cancel();
		} else if (!mod.isEnabled()) {
			ci.cancel();
		}
	}

	public void onCustomRender(GuiGraphics context, int x, int y) {

		if (!this.bossBars.isEmpty()) {

			ProfilerFiller profiler = Profiler.get();
			profiler.push("bossHealth");
			int j = y;

			for (LerpingBossEvent clientBossBar : this.bossBars.values()) {

				Component text = clientBossBar.getName();
				int m = this.client.font.width(text);
				int n = x - m / 2;
				context.drawString(this.client.font, text, n, j, 16777215);

				int k = x - 91;
				this.renderBossBar(context, k, j + 9, clientBossBar);

				j += 10 + 9;
				if (j >= context.guiHeight() / 3) {
					break;
				}
			}

			profiler.pop();
		}
	}
}
