package com.soarclient.management.mod.impl.misc;

import com.soarclient.event.EventBus;
import com.soarclient.event.client.ClientTickEvent;
import com.soarclient.management.mod.Mod;
import com.soarclient.management.mod.ModCategory;
import com.soarclient.management.mod.settings.impl.BooleanSetting;
import com.soarclient.management.mod.settings.impl.StringSetting;
import com.soarclient.skia.font.Icon;
import com.soarclient.utils.TimerUtils;
import com.soarclient.utils.server.Server;
import com.soarclient.utils.server.ServerUtils;

public class HypixelMod extends Mod {

	private static HypixelMod instance;
	private StringSetting apiKeySetting = new StringSetting("setting.hypixel.apikey", "setting.hypixel.apikey.description",
			Icon.KEY, this, "");
	private BooleanSetting levelHeadSetting = new BooleanSetting("setting.levelhead", "setting.levelhead.description",
			Icon._123, this, false);
	private BooleanSetting autoTipSetting = new BooleanSetting("setting.autotip", "setting.autotip.description",
			Icon.MONEY, this, false);

	private TimerUtils tipTimer = new TimerUtils();

	public HypixelMod() {
		super("mod.hypixel.name", "mod.hypixel.description", Icon.CONSTRUCTION, ModCategory.MISC);

		instance = this;
	}

	public final EventBus.EventListener<ClientTickEvent> onClientTick = event -> {
		if (autoTipSetting.isEnabled() && client.player != null && ServerUtils.isJoin(Server.HYPIXEL)) {
			if (tipTimer.delay(1200000)) {
				client.player.networkHandler.sendChatCommand("tip all");
				tipTimer.reset();
			}
		} else {
			tipTimer.reset();
		}
	};

	public static HypixelMod getInstance() {
		return instance;
	}

	public StringSetting getApiKeySetting() {
		return apiKeySetting;
	}

	public BooleanSetting getLevelHeadSetting() {
		return levelHeadSetting;
	}
}