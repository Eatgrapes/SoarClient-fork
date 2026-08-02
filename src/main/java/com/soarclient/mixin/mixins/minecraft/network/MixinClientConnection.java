package com.soarclient.mixin.mixins.minecraft.network;

import com.soarclient.event.EventBus;
import com.soarclient.event.client.ReceivePacketEvent;
import com.soarclient.event.client.SendPacketEvent;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public abstract class MixinClientConnection {

	@Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
	private void onSendPacket(Packet<?> packet, CallbackInfo ci) {
		SendPacketEvent event = new SendPacketEvent(packet);
		EventBus.getInstance().post(event);
		if (event.isCancelled()) {
			ci.cancel();
		}
	}

	@Inject(method = "genericsFtw", at = @At("HEAD"), cancellable = true)
	private static void onReceivePacket(Packet<?> packet, PacketListener listener, CallbackInfo ci) {
		ReceivePacketEvent event = new ReceivePacketEvent(packet);
		EventBus.getInstance().post(event);
		if (event.isCancelled()) {
			ci.cancel();
		}
	}
}
