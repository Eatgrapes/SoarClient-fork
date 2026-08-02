package com.soarclient.mixin.mixins.minecraft.client.gui;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.soarclient.gui.GuiResourcePackConvert;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.network.chat.Component;

@Mixin(PackSelectionScreen.class)
public class MixinPackScreen extends Screen {

	protected MixinPackScreen(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("HEAD"))
	private void onInit(CallbackInfo ci) {

		Button.Builder builder = Button
				.builder(Component.nullToEmpty("Convert"), button -> minecraft.gui.setScreen(new GuiResourcePackConvert(this))).size(98, 20);

		builder.pos(width - 98 - 5, 5);
		this.addRenderableWidget(builder.build());
	}
}
