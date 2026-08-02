package com.soarclient.management.mod.impl.hud;

import java.text.DecimalFormat;

import com.soarclient.event.EventBus;
import com.soarclient.event.client.RenderSkiaEvent;
import com.soarclient.management.mod.api.hud.SimpleHUDMod;
import com.soarclient.skia.font.Icon;

public class PitchDisplayMod extends SimpleHUDMod {

	private DecimalFormat df = new DecimalFormat("0.##");

	public PitchDisplayMod() {
		super("mod.pitchdisplay.name", "mod.pitchdisplay.description", Icon.ARROW_UPWARD);
	}

	public EventBus.EventListener<RenderSkiaEvent> onRenderSkia = event -> {
		this.draw();
	};

	@Override
	public String getText() {
		return "Pitch: " + df.format(client.player.getXRot());
	}

	@Override
	public String getIcon() {
		return Icon.ARROW_UPWARD;
	}
}