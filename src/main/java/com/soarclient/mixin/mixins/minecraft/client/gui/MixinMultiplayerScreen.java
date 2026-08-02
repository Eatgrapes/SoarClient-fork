package com.soarclient.mixin.mixins.minecraft.client.gui;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.soarclient.event.EventBus;
import com.soarclient.event.client.ServerJoinEvent;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;

@Mixin(JoinMultiplayerScreen.class)
public class MixinMultiplayerScreen {

    @Inject(method = "join(Lnet/minecraft/client/multiplayer/ServerData;)V", at = @At("HEAD"))
    private void onConnect(ServerData server, CallbackInfo ci) {
        EventBus.getInstance().post(new ServerJoinEvent(server.ip));
    }
}
