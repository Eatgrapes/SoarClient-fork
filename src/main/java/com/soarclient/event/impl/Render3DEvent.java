package com.soarclient.event.impl;

import com.soarclient.event.Event;
import net.minecraft.client.gui.DrawContext;

public class Render3DEvent extends Event {

    private final float partialTicks;
    private final DrawContext context;

    public Render3DEvent(float partialTicks, DrawContext context) {
        this.partialTicks = partialTicks;
        this.context = context;
    }

    public float getPartialTicks() {
        return partialTicks;
    }

    public DrawContext getContext() {
        return context;
    }
}
