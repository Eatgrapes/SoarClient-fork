package com.soarclient.management.mod.impl.player;

import java.util.Arrays;
import net.minecraft.client.CameraType;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.platform.InputConstants;
import com.soarclient.event.EventBus;
import com.soarclient.event.client.ClientTickEvent;
import com.soarclient.management.mod.Mod;
import com.soarclient.management.mod.ModCategory;
import com.soarclient.management.mod.settings.impl.BooleanSetting;
import com.soarclient.management.mod.settings.impl.ComboSetting;
import com.soarclient.management.mod.settings.impl.KeybindSetting;
import com.soarclient.skia.font.Icon;

public class TaplookMod extends Mod {

	private boolean active;
	private CameraType prevPerspective;
	private boolean toggled;

	private ComboSetting perspectiveSetting = new ComboSetting("setting.perspective", "setting.perspective.description",
			Icon.CAMERASWITCH, this, Arrays.asList("setting.front", "setting.behind"), "setting.front");
	private BooleanSetting toggleSetting = new BooleanSetting("setting.toggle", "setting.toggle.description",
			Icon.SWITCH, this, false);
	private KeybindSetting keybindSetting = new KeybindSetting("setting.keybind", "setting.keybind.description",
			Icon.KEYBOARD, this, InputConstants.getKey(new KeyEvent(GLFW.GLFW_KEY_V, 0, 0)));

	public TaplookMod() {
		super("mod.taplook.name", "mod.taplook.description", Icon.TOUCH_APP, ModCategory.PLAYER);
	}

	public final EventBus.EventListener<ClientTickEvent> onClientTick = event -> {

		if (toggleSetting.isEnabled()) {

			if (keybindSetting.isPressed()) {
				toggled = !toggled;

				if (toggled) {
					if (!active) {
						this.start();
					}
				} else {
					if (active) {
						this.stop();
					}
				}
			}
		} else {
			if (keybindSetting.isKeyDown()) {
				if (!active) {
					this.start();
				}
			} else if (active) {
				this.stop();
			}
		}
	};

	private void start() {

		String option = perspectiveSetting.getOption();
		CameraType perspective = option.equals("setting.front") ? CameraType.THIRD_PERSON_FRONT
				: CameraType.THIRD_PERSON_BACK;

		active = true;
		prevPerspective = client.options.getCameraType();
		client.options.setCameraType(perspective);
	}

	private void stop() {
		active = false;
		client.options.setCameraType(prevPerspective);
	}
}
