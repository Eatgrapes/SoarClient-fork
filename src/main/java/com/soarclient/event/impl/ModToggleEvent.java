package com.soarclient.event.impl;

import com.soarclient.event.Event;
import com.soarclient.management.mod.Mod;

public class ModToggleEvent extends Event {

    private final Mod mod;
    private final boolean state;

    public ModToggleEvent(Mod mod, boolean state) {
        this.mod = mod;
        this.state = state;
    }

    public Mod getMod() {
        return mod;
    }

    public boolean isState() {
        return state;
    }
}
