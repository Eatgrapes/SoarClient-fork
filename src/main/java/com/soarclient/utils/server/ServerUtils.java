package com.soarclient.utils.server;

import net.minecraft.client.Minecraft;

public class ServerUtils {

	private static Minecraft client = Minecraft.getInstance();

	public static boolean isJoin(Server server) {
		return isMultiplayer() && getAddress().contains(server.getAddress());
	}

	public static boolean isSingleplayer() {
		return client.hasSingleplayerServer();
	}

	public static boolean isMultiplayer() {
		return client.getCurrentServer() != null;
	}

	public static String getAddress() {
		return isMultiplayer() ? client.getCurrentServer().ip : "null";
	}
}
