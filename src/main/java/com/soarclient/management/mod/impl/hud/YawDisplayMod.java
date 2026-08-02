package com.soarclient.management.mod.impl.hud;

import java.text.DecimalFormat;

import com.soarclient.event.EventBus;
import com.soarclient.event.client.RenderSkiaEvent;
import com.soarclient.management.mod.api.hud.SimpleHUDMod;
import com.soarclient.skia.font.Icon;

public class YawDisplayMod extends SimpleHUDMod {

	private DecimalFormat df = new DecimalFormat("0.##");

	public YawDisplayMod() {
		super("mod.yawdisplay.name", "mod.yawdisplay.description", Icon.ARROW_RANGE);
	}

	public EventBus.EventListener<RenderSkiaEvent> onRenderSkia = event -> {
		this.draw();
	};

	@Override
	public String getText() {
		return "Yaw: " + df.format(Math.abs(client.player.getYRot() % 90));
	}

	@Override
	public String getIcon() {
		return Icon.ARROW_RANGE;
	}
}