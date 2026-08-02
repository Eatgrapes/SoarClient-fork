package com.soarclient.management.mod.impl.hud;

import java.text.DecimalFormat;
import net.minecraft.client.input.KeyEvent;

import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.platform.InputConstants;
import com.soarclient.event.EventBus;
import com.soarclient.event.client.ClientTickEvent;
import com.soarclient.event.client.RenderSkiaEvent;
import com.soarclient.management.mod.api.hud.SimpleHUDMod;
import com.soarclient.management.mod.settings.impl.KeybindSetting;
import com.soarclient.skia.font.Icon;
import com.soarclient.utils.TimerUtils;

public class StopwatchMod extends SimpleHUDMod {

	private TimerUtils timer = new TimerUtils();
	private int pressCount;
	private float currentTime;
	private DecimalFormat timeFormat = new DecimalFormat("0.00");

	private KeybindSetting keybindSetting = new KeybindSetting("setting.keybind", "setting.keybind.description",
			Icon.KEYBOARD, this, InputConstants.getKey(new KeyEvent(GLFW.GLFW_KEY_P, 0, 0)));

	public StopwatchMod() {
		super("mod.stopwatch.name", "mod.stopwatch.description", Icon.TIMER);
	}

	public final EventBus.EventListener<RenderSkiaEvent> onRenderSkia = event -> {
		this.draw();
	};

	public final EventBus.EventListener<ClientTickEvent> onClientTick = event -> {

		if (keybindSetting.isPressed()) {
			pressCount++;
		}

		switch (pressCount) {
		case 0:
			timer.reset();
			break;
		case 1:
			currentTime = (timer.getElapsedTime() / 1000F);
			break;
		case 3:
			timer.reset();
			currentTime = 0;
			pressCount = 0;
			break;
		}
	};

	@Override
	public void onEnable() {

		super.onEnable();

		if (timer != null) {
			timer.reset();
		}

		pressCount = 0;
		currentTime = 0;
	}

	@Override
	public String getText() {
		return timeFormat.format(currentTime) + " s";
	}

	@Override
	public String getIcon() {
		return Icon.TIMER;
	}
}