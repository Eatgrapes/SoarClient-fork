package com.soarclient.mixin.mixins.minecraft.client.gui;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.soarclient.gui.mainmenu.MainMenuGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

@Mixin(value = TitleScreen.class, priority = 1001)
public abstract class MixinTitleScreen extends Screen {

    protected MixinTitleScreen(Component title) {
        super(title);
    }

    @Inject(method = "init()V", at = @At("HEAD"), cancellable = true)
    public void onInit(CallbackInfo ci) {
        Minecraft.getInstance().gui.setScreen(new MainMenuGui().build());
        ci.cancel();
    }

}
