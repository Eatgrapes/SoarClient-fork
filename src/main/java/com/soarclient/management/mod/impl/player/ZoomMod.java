
package com.soarclient.management.mod.impl.player;

import org.lwjgl.glfw.GLFW;
import net.minecraft.client.input.KeyEvent;
import com.mojang.blaze3d.platform.InputConstants;
import com.soarclient.animation.SimpleAnimation;
import com.soarclient.event.EventBus;
import com.soarclient.event.client.ClientTickEvent;
import com.soarclient.event.client.MouseScrollEvent;
import com.soarclient.management.mod.Mod;
import com.soarclient.management.mod.ModCategory;
import com.soarclient.management.mod.settings.impl.BooleanSetting;
import com.soarclient.management.mod.settings.impl.KeybindSetting;
import com.soarclient.management.mod.settings.impl.NumberSetting;
import com.soarclient.skia.font.Icon;

public class ZoomMod extends Mod {

	private static ZoomMod instance;

	private SimpleAnimation animation = new SimpleAnimation();

	private boolean active;
	private float currentFactor = 1;

	public boolean wasSmooth;

	private BooleanSetting scrollSetting = new BooleanSetting("setting.scroll", "setting.zoom.scroll.description",
			Icon.SCROLLABLE_HEADER, this, false);
	private BooleanSetting smoothZoomSetting = new BooleanSetting("setting.smoothzoom",
			"setting.smoothzoom.description", Icon.MOTION_BLUR, this, false);
	private NumberSetting zoomSpeedSetting = new NumberSetting("setting.zoomspeed", "setting.zoomspeed.description",
			Icon.SPEED, this, 0.6F, 0, 1, 0.1F);
	private NumberSetting factorSetting = new NumberSetting("setting.zoomfactor", "setting.zoomfactor.description",
			Icon.ZOOM_OUT, this, 4, 2, 15, 1);
	private BooleanSetting smoothCameraSetting = new BooleanSetting("setting.smoothcamera",
			"setting.smoothcamera.description", Icon.MOTION_BLUR, this, true);
	private KeybindSetting keybindSetting = new KeybindSetting("setting.keybind", "setting.keybind.description",
			Icon.KEYBOARD, this, InputConstants.getKey(new KeyEvent(GLFW.GLFW_KEY_C, 0, 0)));

	public ZoomMod() {
		super("mod.zoom.name", "mod.zoom.description", Icon.ZOOM_IN, ModCategory.PLAYER);

		instance = this;
	}

	public final EventBus.EventListener<ClientTickEvent> onClientTick = event -> {
		if (keybindSetting.isKeyDown()) {
			if (!active) {
				active = true;
				resetFactor();
				wasSmooth = client.options.smoothCamera;
				client.options.smoothCamera = smoothCameraSetting.isEnabled();
			}
		} else if (active) {
			active = false;
			setFactor(1);
			client.options.smoothCamera = wasSmooth;
		}
	};

	public final EventBus.EventListener<MouseScrollEvent> onMouseScroll = event -> {
		if (active && scrollSetting.isEnabled()) {
			event.setCancelled(true);
			if (event.getAmount() < 0) {
				if (currentFactor < 0.98) {
					currentFactor += 0.03;
				}
			} else if (event.getAmount() > 0) {
				if (currentFactor > 0.06) {
					currentFactor -= 0.03;
				}
			}
		}
	};

	public void resetFactor() {
		setFactor(1 / factorSetting.getValue());
	}

	public void setFactor(float factor) {
		currentFactor = factor;
	}

	public float getFov(float fov) {

		if (smoothZoomSetting.isEnabled()) {
			animation.onTick(currentFactor, zoomSpeedSetting.getValue() * 10);
		}

		return fov * (smoothZoomSetting.isEnabled() ? animation.getValue() : currentFactor);
	}

	public static ZoomMod getInstance() {
		return instance;
	}
}
