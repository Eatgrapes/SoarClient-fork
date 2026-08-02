package com.soarclient.event.client;

import com.soarclient.event.Event;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class RenderHotbarEvent extends Event {

    private final GuiGraphicsExtractor context;
    private final float tickDelta;

    public RenderHotbarEvent(GuiGraphicsExtractor context, float tickDelta) {
        this.context = context;
        this.tickDelta = tickDelta;
    }

    public GuiGraphicsExtractor getContext() {
        return context;
    }

    public float getTickDelta() {
        return tickDelta;
    }
}
