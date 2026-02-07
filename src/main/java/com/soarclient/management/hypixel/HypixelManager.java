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
import com.soarclient.utils.server.Server;
import com.soarclient.utils.server.ServerUtils;

import com.soarclient.management.mod.impl.misc.HypixelMod;
import com.soarclient.utils.HttpUtils;
import com.soarclient.utils.JsonUtils;
import com.soarclient.utils.Multithreading;
import com.google.gson.JsonObject;

public class HypixelManager {

	private final Cache<String, HypixelUser> cache = Caffeine.newBuilder().maximumSize(1000).build();
	private final Set<String> requests = new HashSet<>();
	private final TimerUtils timer = new TimerUtils();

	public HypixelManager() {
	}

	public void add(HypixelUser user) {
		cache.put(user.getUuid(), user);
	}

	public void update() {

		Iterator<String> iterator = requests.iterator();

		if (ServerUtils.isJoin(Server.HYPIXEL)) {
			if (timer.delay(500)) {

				if (iterator.hasNext()) {
					String request = iterator.next();
					
					String apiKey = HypixelMod.getInstance().getApiKeySetting().getValue();
					
					if (apiKey != null && !apiKey.isEmpty()) {
						Multithreading.runAsync(() -> fetchFromApi(request, apiKey));
					} else {
						Soar.getInstance().getWebSocketManager().send(new SC_HypixelStatsPacket(request));
					}
					
					requests.remove(request);
				}

				timer.reset();
			}
		} else {
			timer.reset();
		}
	}

	private void fetchFromApi(String uuid, String apiKey) {
		try {
			String winstreakUrl = "https://api.winstreak.ws/v1/stats?uuid=" + uuid + "&key=" + apiKey;
			JsonObject winstreakJson = HttpUtils.readJson(winstreakUrl, null);
			
			if (winstreakJson != null && winstreakJson.has("success") && winstreakJson.get("success").getAsBoolean() && winstreakJson.has("data")) {
				JsonObject data = winstreakJson.getAsJsonObject("data");
				JsonObject bedwars = JsonUtils.getObjectProperty(data, "bedwars");
				
				if (bedwars != null) {
					String networkLevel = JsonUtils.getStringProperty(data, "network_level", "0");
					String bedwarsLevel = JsonUtils.getStringProperty(bedwars, "level", "0");
					
					String wlr = String.format("%.2f", JsonUtils.getFloatProperty(bedwars, "wlr", 0));
					String fkdr = String.format("%.2f", JsonUtils.getFloatProperty(bedwars, "fkdr", 0));
					String bblr = String.format("%.2f", JsonUtils.getFloatProperty(bedwars, "bblr", 0));
					
					HypixelUser user = new HypixelUser(uuid, networkLevel, bedwarsLevel, wlr, fkdr, bblr);
					add(user);
					return;
				}
			}

			String url = "https://api.hypixel.net/v2/player?uuid=" + uuid + "&key=" + apiKey;
			JsonObject json = HttpUtils.readJson(url, null);
			
			if (json != null && json.has("success") && json.get("success").getAsBoolean() && json.has("player") && !json.get("player").isJsonNull()) {
				JsonObject player = json.getAsJsonObject("player");
				
				String networkLevel = String.format("%.0f", (Math.sqrt(JsonUtils.getLongProperty(player, "networkExp", 0) + 15312.5) - 125 / Math.sqrt(2)) / (25 * Math.sqrt(2)));
				
				JsonObject stats = JsonUtils.getObjectProperty(player, "stats");
				JsonObject bedwars = JsonUtils.getObjectProperty(stats, "Bedwars");
				
				if (bedwars != null) {
					String bedwarsLevel = "0";
					if (bedwars.has("Experience")) {
						bedwarsLevel = String.valueOf(bedwars.get("Experience").getAsInt() / 5000);
					}
					
					int finalKills = JsonUtils.getIntProperty(bedwars, "final_kills_bedwars", 0);
					int finalDeaths = JsonUtils.getIntProperty(bedwars, "final_deaths_bedwars", 0);
					int wins = JsonUtils.getIntProperty(bedwars, "wins_bedwars", 0);
					int losses = JsonUtils.getIntProperty(bedwars, "losses_bedwars", 0);
					int bedsBroken = JsonUtils.getIntProperty(bedwars, "beds_broken_bedwars", 0);
					int bedsLost = JsonUtils.getIntProperty(bedwars, "beds_lost_bedwars", 0);
					
					String wlr = String.format("%.2f", (double) wins / Math.max(1, losses));
					String fkdr = String.format("%.2f", (double) finalKills / Math.max(1, finalDeaths));
					String bblr = String.format("%.2f", (double) bedsBroken / Math.max(1, bedsLost));
					
					HypixelUser user = new HypixelUser(uuid, networkLevel, bedwarsLevel, wlr, fkdr, bblr);
					add(user);
				}
			}
		} catch (Exception e) {
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
