package com.soarclient.management.mod.impl.hud;

import com.soarclient.event.EventBus;
import com.soarclient.event.client.RenderSkiaEvent;
import com.soarclient.management.mod.api.hud.SimpleHUDMod;
import com.soarclient.skia.font.Icon;

public class PlayerCounterMod extends SimpleHUDMod {

	public PlayerCounterMod() {
		super("mod.playercounter.name", "mod.playercounter.description", Icon.GROUPS);
	}

	public final EventBus.EventListener<RenderSkiaEvent> onRenderSkia = event -> {
		this.draw();
	};

	@Override
	public String getText() {
		return "Player: " + client.level.players().size();
	}

	@Override
	public String getIcon() {
		return Icon.GROUPS;
	}
}