package com.soarclient.event.impl;

import com.soarclient.event.Event;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class Render3DEvent extends Event {

    private final float partialTicks;
    private final GuiGraphicsExtractor context;

    public Render3DEvent(float partialTicks, GuiGraphicsExtractor context) {
        this.partialTicks = partialTicks;
        this.context = context;
    }

    public float getPartialTicks() {
        return partialTicks;
    }

    public GuiGraphicsExtractor getContext() {
        return context;
    }
}
