package com.soarclient.management.mod.impl.misc;

import com.soarclient.management.mod.Mod;
import com.soarclient.management.mod.ModCategory;
import com.soarclient.management.mod.settings.impl.NumberSetting;
import com.soarclient.skia.font.Icon;

public class FakeFPSMod extends Mod {

    private static FakeFPSMod instance;

    private NumberSetting multiplierSetting = new NumberSetting("setting.multiplier",
        "setting.multiplier.description", Icon.SPEED, this, 2.0f, 1.0f, 10.0f, 0.1f);

    private NumberSetting superBoosterSetting = new NumberSetting("setting.superbooster",
        "setting.superbooster.description", Icon.ROCKET, this, 0.0f, 0.0f, 10000.0f, 100.0f);

    public FakeFPSMod() {
        super("mod.fakefps.name", "mod.fakefps.description", Icon.MONITOR, ModCategory.MISC);
        instance = this;
    }

    public static FakeFPSMod getInstance() {
        return instance;
    }

    public float getMultiplier() {
        float boosterValue = superBoosterSetting.getValue();
        if (boosterValue > 0.0f) {
            return boosterValue;
        }
        return multiplierSetting.getValue();
    }

    public int getFakeFPS(int realFPS) {
        if (!isEnabled()) {
            return realFPS;
        }
        return Math.round(realFPS * getMultiplier());
    }
}
