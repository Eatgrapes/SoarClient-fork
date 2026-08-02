package com.soarclient.event.server;

import com.soarclient.event.EventBus;
import com.soarclient.event.client.ReceivePacketEvent;
import com.soarclient.event.client.SendPacketEvent;
import com.soarclient.event.server.impl.AttackEntityEvent;
import com.soarclient.event.server.impl.DamageEntityEvent;
import com.soarclient.event.server.impl.GameJoinEvent;
import com.soarclient.event.server.impl.ReceiveChatEvent;
import com.soarclient.event.server.impl.SendChatEvent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;

public class PacketHandler {

	public final EventBus.EventListener<SendPacketEvent> onSendPacket = packetEvent -> {
		Packet<?> packet = packetEvent.getPacket();

		if (packet instanceof ServerboundAttackPacket attackPacket) {
			EventBus.getInstance().post(new AttackEntityEvent(attackPacket.entityId()));
		}

		if (packet instanceof ServerboundChatPacket chatPacket) {
			SendChatEvent event = new SendChatEvent(chatPacket.message());
			EventBus.getInstance().post(event);
			if (event.isCancelled()) {
				packetEvent.setCancelled(true);
			}
		}
	};

	public final EventBus.EventListener<ReceivePacketEvent> onReceivePacket = packetEvent -> {
		Packet<?> packet = packetEvent.getPacket();

		if (packet instanceof ClientboundDamageEventPacket damagePacket) {
			EventBus.getInstance().post(new DamageEntityEvent(damagePacket.entityId()));
		}

		if (packet instanceof ClientboundPlayerChatPacket chatPacket) {
			ReceiveChatEvent event = new ReceiveChatEvent(chatPacket.body().content());
			EventBus.getInstance().post(event);
			if (event.isCancelled()) {
				packetEvent.setCancelled(true);
			}
		}

		if (packet instanceof ClientboundSystemChatPacket chatPacket) {
			ReceiveChatEvent event = new ReceiveChatEvent(chatPacket.content().getString());
			EventBus.getInstance().post(event);
			if (event.isCancelled()) {
				packetEvent.setCancelled(true);
			}
		}

		if (packet instanceof ClientboundLoginPacket) {
			EventBus.getInstance().post(new GameJoinEvent());
		}
	};
}
