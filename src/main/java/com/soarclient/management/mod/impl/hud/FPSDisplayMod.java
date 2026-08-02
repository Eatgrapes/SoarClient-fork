package com.soarclient.management.mod.impl.hud;

import com.soarclient.event.EventBus;
import com.soarclient.event.client.RenderSkiaEvent;
import com.soarclient.management.mod.api.hud.SimpleHUDMod;
import com.soarclient.management.mod.impl.misc.FakeFPSMod;
import com.soarclient.skia.font.Icon;

public class FPSDisplayMod extends SimpleHUDMod {

	public FPSDisplayMod() {
		super("mod.fpsdisplay.name", "mod.fpsdisplay.description", Icon.MONITOR);
	}

	public final EventBus.EventListener<RenderSkiaEvent> onRenderSkia = event -> {
		this.draw();
	};

    @Override
    public String getText() {
        int fps = client.getFps();
        FakeFPSMod fakeFPSMod = FakeFPSMod.getInstance();
        if (fakeFPSMod != null) {
            fps = fakeFPSMod.getFakeFPS(fps);
        }
        return fps + " FPS";
    }

	@Override
	public String getIcon() {
		return Icon.MONITOR;
	}
}
