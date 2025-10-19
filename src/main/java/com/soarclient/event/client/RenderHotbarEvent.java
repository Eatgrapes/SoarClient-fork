package com.soarclient.event.client;

import com.soarclient.event.Event;

import net.minecraft.client.gui.DrawContext;

public class RenderHotbarEvent extends Event {

    private final DrawContext context;
    private final float tickDelta;

    public RenderHotbarEvent(DrawContext context, float tickDelta) {
        this.context = context;
        this.tickDelta = tickDelta;
    }

    public DrawContext getContext() {
        return context;
    }

    public float getTickDelta() {
        return tickDelta;
    }
}
