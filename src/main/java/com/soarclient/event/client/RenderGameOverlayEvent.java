package com.soarclient.event.client;

import com.soarclient.event.Event;
import net.minecraft.client.gui.GuiGraphics;

public class RenderGameOverlayEvent extends Event {
	
	private final GuiGraphics context;
	
	public RenderGameOverlayEvent(GuiGraphics context) {
		this.context = context;
	}

	public GuiGraphics getContext() {
		return context;
	}
}
