package com.soarclient;

import java.util.List;

import com.soarclient.event.EventBus;
import com.soarclient.event.client.ClientTickEvent;
import com.soarclient.event.client.ServerJoinEvent;
import com.soarclient.event.impl.Render3DEvent;
import com.soarclient.event.server.impl.GameJoinEvent;
import com.soarclient.management.profile.Profile;

public class SoarHandler {

	public final EventBus.EventListener<ClientTickEvent> onClientTick = event -> {
		Soar.getInstance().getColorManager().onTick();
		Soar.getInstance().getHypixelManager().update();
		Soar.getInstance().getUserManager().update();
	};

	public final EventBus.EventListener<GameJoinEvent> onGameJoin = event -> {
		Soar.getInstance().getHypixelManager().clear();
		Soar.getInstance().getUserManager().clear();
	};

	public final EventBus.EventListener<ServerJoinEvent> onServerJoin = event -> {

		List<Profile> profiles = Soar.getInstance().getProfileManager().getProfiles();

		for (Profile p : profiles) {

			String address = p.getServerIp();

			if (event.getAddress().contains(address)) {
				Soar.getInstance().getProfileManager().load(p);
				break;
			}
		}
	};

	public final EventBus.EventListener<Render3DEvent> onRender3D = event -> {
		Soar.getInstance().getModManager().onRender3D(event.getContext(), event.getPartialTicks());
	};
}
