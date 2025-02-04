package com.soarclient.management.mod.impl.misc;

import com.soarclient.management.mod.Mod;
import com.soarclient.management.mod.ModCategory;
import com.soarclient.management.mod.settings.impl.NumberSetting;
import com.soarclient.skia.font.Icon;

public class TimeChangerMod extends Mod {

	private static TimeChangerMod instance;
	private NumberSetting timeSetting = new NumberSetting("setting.time", "setting.time.description", Icon.UPDATE, this,
			12, 0, 24, 1);

	public TimeChangerMod() {
		super("mod.timechanger.name", "mod.timechanger.description", Icon.UPDATE, ModCategory.MISC);

		instance = this;
	}

	public static TimeChangerMod getInstance() {
		return instance;
	}

	public long getTime() {
		return (long) (timeSetting.getValue() * 1000L) + 18000L;
	}
}