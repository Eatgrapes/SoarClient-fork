package com.soarclient.management.mod.impl.render;

import com.soarclient.management.mod.Mod;
import com.soarclient.management.mod.ModCategory;
import com.soarclient.management.mod.settings.Setting;
import com.soarclient.management.mod.settings.impl.BooleanSetting;
import com.soarclient.management.mod.settings.impl.NumberSetting;
import com.soarclient.skia.font.Icon;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ActionCameraMod extends Mod {
    private final BooleanSetting disableFirstPers = new BooleanSetting("mod.actioncamera.disable_first_person", "mod.actioncamera.disable_first_person.desc", Icon.CAMERA, this, true);
    private final NumberSetting smoothness = new NumberSetting("mod.actioncamera.smoothness", "mod.actioncamera.smoothness.desc", Icon.TIMELINE, this, 0.3f, 0.1f, 0.95f, 0.01f);
    private final NumberSetting maxDistance = new NumberSetting("mod.actioncamera.max_distance", "mod.actioncamera.max_distance.desc", Icon.ZOOM_OUT, this, 20.0f, 1.0f, 50.0f, 0.5f);

    private Vec3d cameraPos;
    private int key = GLFW.GLFW_KEY_F6;

    public ActionCameraMod() {
        super("mod.ActionCameraMod.name", "mod.ActionCameraMod.description", Icon.CAMERA, ModCategory.RENDER);
    }

    @Override
    public void onEnable() {
        if (client != null && client.player != null) {
            cameraPos = client.player.getPos();
        }
    }

    @Override
    public void onDisable() {
        cameraPos = null;
    }

    private boolean firstPerson() {
        return client != null && client.options != null &&
               client.options.getPerspective() == Perspective.FIRST_PERSON;
    }

    public Vec3d getCameraPos() {
        if (firstPerson() && client.player != null) {
            return new Vec3d(
                client.player.getX(),
                client.player.getY() + client.player.getEyeHeight(client.player.getPose()),
                client.player.getZ()
            );
        }
        return cameraPos;
    }

    public void update(Vec3d playerPos) {
        if (client == null || client.player == null || cameraPos == null) return;

        double distance = cameraPos.distanceTo(playerPos);
        float maxDist = maxDistance.getValue();

        if (distance > maxDist) {
            cameraPos = playerPos;
            return;
        }

        float smoothFactor = smoothness.getValue();
        double dynamicFactor = smoothFactor * (1.0 - Math.exp(-distance / maxDist));

        double dx = playerPos.x - cameraPos.x;
        double dy = playerPos.y + client.player.getEyeHeight(client.player.getPose()) - cameraPos.y;
        double dz = playerPos.z - cameraPos.z;

        cameraPos = new Vec3d(
                cameraPos.x + dx * dynamicFactor,
                cameraPos.y + dy * dynamicFactor,
                cameraPos.z + dz * dynamicFactor
        );
    }

    public boolean shouldModifyCamera() {
        return isEnabled() && (!disableFirstPers.isEnabled() || !firstPerson());
    }

    public int getKey() {
        return this.key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public List<Setting> getSettings() {
        List<Setting> settings = new ArrayList<>();
        settings.add(disableFirstPers);
        settings.add(smoothness);
        settings.add(maxDistance);
        return settings;
    }
}
