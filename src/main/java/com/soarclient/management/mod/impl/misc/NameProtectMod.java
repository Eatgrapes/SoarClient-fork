package com.soarclient.management.mod.impl.misc;

import com.soarclient.management.mod.Mod;
import com.soarclient.management.mod.ModCategory;
import com.soarclient.management.mod.settings.impl.StringSetting;
import com.soarclient.skia.font.Icon;
import net.minecraft.client.MinecraftClient;

public class NameProtectMod extends Mod {

    private static NameProtectMod instance;

    private StringSetting nameSetting = new StringSetting("setting.nameprotect.name",
        "setting.nameprotect.name.description", Icon.PERSON, this, "You");

    public NameProtectMod() {
        super("mod.nameprotect.name", "mod.nameprotect.description", Icon.SHIELD_PERSON, ModCategory.MISC);
        instance = this;
    }

    public static NameProtectMod getInstance() {
        return instance;
    }

    public String replaceName(String text) {
        if (!isEnabled()) {
            return text;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return text;
        }

        String playerName = client.player.getName().getString();
        String replacementName = nameSetting.getValue();

        if (text.contains(playerName)) {
            return text.replace(playerName, replacementName);
        }

        return text;
    }

    public StringSetting getNameSetting() {
        return nameSetting;
    }
}
