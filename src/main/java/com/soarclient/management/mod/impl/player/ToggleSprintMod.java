package com.soarclient.management.mod.impl.player;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.KeyEvent;
import com.soarclient.Soar;
import com.soarclient.event.EventBus;
import com.soarclient.event.client.ClientTickEvent;
import com.soarclient.management.config.ConfigType;
import com.soarclient.management.mod.Mod;
import com.soarclient.management.mod.ModCategory;
import com.soarclient.management.mod.settings.impl.BooleanSetting;
import com.soarclient.management.mod.settings.impl.KeybindSetting;
import com.soarclient.skia.font.Icon;
import org.lwjgl.glfw.GLFW;

public class ToggleSprintMod extends Mod {

    private static ToggleSprintMod instance;

    private final BooleanSetting toggled = new BooleanSetting("setting.togglesprint.toggled", "setting.togglesprint.toggled.description", Icon.TOGGLE_ON, this, true);
    private final KeybindSetting sprintKey = new KeybindSetting("setting.togglesprint.keybind", "setting.togglesprint.keybind.description", Icon.KEYBOARD, this, InputConstants.getKey(new KeyEvent(GLFW.GLFW_KEY_LEFT_CONTROL, 0, 0)));

    public ToggleSprintMod() {
        super("mod.togglesprint.name", "mod.togglesprint.description", Icon.DIRECTIONS_RUN, ModCategory.PLAYER);
        instance = this;
    }

    public static ToggleSprintMod getInstance() {
        return instance;
    }

    public final EventBus.EventListener<ClientTickEvent> onClientTick = event -> {
        if (client.player == null) return;

        if (sprintKey.isPressed()) {
            toggled.setEnabled(!toggled.isEnabled());
            
            if (!toggled.isEnabled()) {
                client.options.keySprint.setDown(false);
            }
            
            Soar.getInstance().getConfigManager().save(ConfigType.MOD);
        }

        if (toggled.isEnabled()) {
            client.options.keySprint.setDown(true);
        }
    };

    @Override
    public void onDisable() {
        super.onDisable();
        if (client != null && client.options != null && client.options.keySprint != null) {
            client.options.keySprint.setDown(false);
        }
    }
}
