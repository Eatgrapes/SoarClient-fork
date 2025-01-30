package com.soarclient.management.mod.impl.settings;

import java.util.Arrays;

import com.soarclient.Soar;
import com.soarclient.event.EventBus;
import com.soarclient.event.client.ClientTickEvent;
import com.soarclient.management.mod.Mod;
import com.soarclient.management.mod.ModCategory;
import com.soarclient.management.mod.settings.impl.BooleanSetting;
import com.soarclient.management.mod.settings.impl.ComboSetting;
import com.soarclient.management.mod.settings.impl.NumberSetting;
import com.soarclient.skia.font.Icon;

public class HUDModSettings extends Mod {

	private static HUDModSettings instance;

	private BooleanSetting blurSetting = new BooleanSetting("setting.blur", "setting.blur.description", Icon.LENS_BLUR,
			this, true);
	private ComboSetting designSetting = new ComboSetting("setting.design", "setting.design.description", Icon.PALETTE,
			this, Arrays.asList("design.simple", "design.classic", "design.clear", "design.materialyou"),
			"design.simple");
	private NumberSetting blurIntensitySetting = new NumberSetting("setting.blurintensity",
			"setting.blurintensity.description", Icon.BLUR_LINEAR, this, 20, 1, 50, 1);

	public HUDModSettings() {
		super("mod.hudsettings.name", "mod.hudsettings.description", Icon.BROWSE_ACTIVITY, ModCategory.MISC);
		this.setHidden(true);
		this.setEnabled(true);

		instance = this;
	}

	public final EventBus.EventListener<ClientTickEvent> onClientTick = event -> {
		if (!designSetting.getOption().equals(Soar.getInstance().getModManager().getCurrentDesign().getName())) {
			Soar.getInstance().getModManager().setCurrentDesign(designSetting.getOption());
		}
	};

	@Override
	public void onDisable() {
		this.setEnabled(true);
	}

	public static HUDModSettings getInstance() {
		return instance;
	}

	public BooleanSetting getBlurSetting() {
		return blurSetting;
	}

	public NumberSetting getBlurIntensitySetting() {
		return blurIntensitySetting;
	}
}
