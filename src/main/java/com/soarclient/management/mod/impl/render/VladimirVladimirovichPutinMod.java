package com.soarclient.management.mod.impl.render;

import com.soarclient.management.mod.Mod;
import com.soarclient.management.mod.ModCategory;
import com.soarclient.management.mod.settings.Setting;
import com.soarclient.management.mod.settings.impl.NumberSetting;
import com.soarclient.skia.font.Icon;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class VladimirVladimirovichPutinMod extends Mod {

    private final NumberSetting aspectWidth = new NumberSetting(
        "mod.putin.aspect_width", "mod.putin.aspect_width.desc",
        Icon.SCREEN_SHARE, this, 16f, 1f, 32f, 0.5f
    );
    private final NumberSetting aspectHeight = new NumberSetting(
        "mod.putin.aspect_height", "mod.putin.aspect_height.desc",
        Icon.SCREEN_SHARE, this, 9f, 1f, 32f, 0.5f
    );

    public VladimirVladimirovichPutinMod() {
        super("mod.vladimirvladimirovichputin.name",
            "mod.vladimirvladimirovichputin.description",
            Icon.SCREEN_SHARE,
            ModCategory.RENDER);
    }

    public void onRender3D() {
        int screenWidth = client.getWindow().getWidth();
        int screenHeight = client.getWindow().getHeight();

        if (screenHeight == 0) return;

        float targetAspect = aspectWidth.getValue() / aspectHeight.getValue();
        float currentAspect = (float) screenWidth / (float) screenHeight;

        float scaleX = 1.0f;
        float scaleY = 1.0f;

        if (currentAspect > targetAspect) {
            scaleX = currentAspect / targetAspect;
        } else if (currentAspect < targetAspect) {
            scaleY = targetAspect / currentAspect;
        }

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glScalef(scaleX, scaleY, 1.0f);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }

    public void onRender3DFinish() {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }

    public List<Setting> getSettings() {
        List<Setting> settings = new ArrayList<>();
        settings.add(aspectWidth);
        settings.add(aspectHeight);
        return settings;
    }
}
