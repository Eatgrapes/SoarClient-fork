package com.soarclient.event.client;

import com.soarclient.event.Event;
import com.soarclient.utils.render.Render3D;

public class RenderWorldEvent extends Event {

	private final Render3D.Renderer renderer;

	public RenderWorldEvent(Render3D.Renderer renderer) {
		this.renderer = renderer;
	}

	public Render3D.Renderer getRenderer() {
		return renderer;
	}
}
