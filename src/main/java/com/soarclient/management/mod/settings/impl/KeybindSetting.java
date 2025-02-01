package com.soarclient.management.mod.settings.impl;

import com.soarclient.Soar;
import com.soarclient.management.mod.Mod;
import com.soarclient.management.mod.settings.Setting;

import net.minecraft.client.util.InputUtil;

public class KeybindSetting extends Setting {

	private InputUtil.Key defaultKey, key;
	private boolean keyDown;
	private int pressTime;

	public KeybindSetting(String name, String description, String icon, Mod parent, InputUtil.Key key) {
		super(name, description, icon, parent);

		this.defaultKey = key;
		this.key = key;

		Soar.getInstance().getModManager().addSetting(this);
	}

	@Override
	public void reset() {
		this.key = this.defaultKey;
	}

	public InputUtil.Key getKey() {
		return key;
	}

	public void setKey(InputUtil.Key key) {
		this.key = key;
	}

	public InputUtil.Key getDefaultKey() {
		return defaultKey;
	}

	public boolean isKeyDown() {
		return keyDown;
	}
	
	public void setKeyDown(boolean keyDown) {
		this.keyDown = keyDown;
	}

	public boolean isPressed() {
		this.pressTime--;
		return pressTime >= 0;
	}

	public void setPressed() {
		this.pressTime = 1;
	}
}