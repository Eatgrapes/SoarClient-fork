package com.soarclient.ui.component.handler.impl;

import com.mojang.blaze3d.platform.InputConstants.Key;
import com.soarclient.ui.component.handler.ComponentHandler;

public abstract class KeybindHandler extends ComponentHandler {
	public abstract void onBinded(Key key);
}