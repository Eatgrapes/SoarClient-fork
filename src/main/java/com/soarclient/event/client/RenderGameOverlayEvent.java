package com.soarclient.event.client;

import com.soarclient.event.Event;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class RenderGameOverlayEvent extends Event {
	
	private final GuiGraphicsExtractor context;
	
	public RenderGameOverlayEvent(GuiGraphicsExtractor context) {
		this.context = context;
	}

	public GuiGraphicsExtractor getContext() {
		return context;
	}
}
