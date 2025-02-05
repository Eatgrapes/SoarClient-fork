package com.soarclient.management.hypixel;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.soarclient.Soar;
import com.soarclient.management.hypixel.api.HypixelUser;
import com.soarclient.management.websocket.packet.impl.SC_HypixelStatsPacket;
import com.soarclient.utils.TimerUtils;

import net.minecraft.client.MinecraftClient;

public class HypixelManager {

	private final MinecraftClient client = MinecraftClient.getInstance();
	private final Cache<String, HypixelUser> cache = Caffeine.newBuilder().maximumSize(500).build();
	private final Set<String> requests = new HashSet<>();
	private final TimerUtils timer = new TimerUtils();

	public HypixelManager() {
	}

	public void add(HypixelUser user) {
		cache.put(user.getUuid(), user);
	}

	public void update() {

		Iterator<String> iterator = requests.iterator();

		if (client.getCurrentServerEntry() != null && client.getCurrentServerEntry().address.contains("hypixel")) {
			if (timer.delay(500)) {

				if (iterator.hasNext()) {
					String request = iterator.next();
					Soar.getInstance().getWebSocketManager().send(new SC_HypixelStatsPacket(request));
					requests.remove(request);
				}

				timer.reset();
			}
		} else {
			timer.reset();
		}
	}

	public HypixelUser getByUuid(String uuid) {

		HypixelUser user = cache.getIfPresent(uuid);

		if (user == null) {
			user = new HypixelUser(uuid, "-1", "-1", "-1", "-1", "-1");
			cache.put(uuid, user);
			requests.add(uuid);
		}

		return user;
	}
	
	public void clear() {
		requests.clear();
	}
}
