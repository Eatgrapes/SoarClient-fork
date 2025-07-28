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

public class ActionCameraMod extends Mod {
    private final BooleanSetting disableFirstPers = new BooleanSetting("mod.actioncamera.disable_first_person", "mod.actioncamera.disable_first_person.desc", Icon.CAMERA, this, true);
    private final NumberSetting smoothness = new NumberSetting("mod.actioncamera.smoothness", "mod.actioncamera.smoothness.desc", Icon.TIMELINE, this, 0.3f, 0.1f, 0.95f, 0.01f);
    private final NumberSetting maxDistance = new NumberSetting("mod.actioncamera.max_distance", "mod.actioncamera.max_distance.desc", Icon.ZOOM_OUT, this, 20.0f, 1.0f, 50.0f, 0.5f);

    private Vec3d cameraPos;
    private int key = GLFW.GLFW_KEY_F6;
    public boolean isFreelookHandoverCooldown = false;

    public ActionCameraMod() {
        super("mod.ActionCameraMod.name", "mod.ActionCameraMod.description", Icon.CAMERA, ModCategory.RENDER);
    }

    @Override
    public void onEnable() {
        this.resetCameraPos();
    }

    @Override
    public void onDisable() {
        this.cameraPos = null;
    }

    public void resetCameraPos() {
        if (client != null && client.player != null) {
            this.cameraPos = new Vec3d(
                client.player.getX(),
                client.player.getY() + client.player.getEyeHeight(client.player.getPose()),
                client.player.getZ()
            );
        }
    }

    private boolean firstPerson() {
        return client != null && client.options != null &&
            client.options.getPerspective() == Perspective.FIRST_PERSON;
    }

    public Vec3d getCameraPos() {
        if (!isEnabled()) return null;
        return this.cameraPos;
    }

    public void update(Vec3d playerPos) {
        if (!isEnabled() || client == null || client.player == null) return;

        if (this.cameraPos == null) {
            resetCameraPos();
            return;
        }

        double factor = smoothness.getValue();
        double dy = client.player.getEyeHeight(client.player.getPose());

        this.cameraPos = new Vec3d(
            this.cameraPos.x + (playerPos.x - this.cameraPos.x) * factor,
            this.cameraPos.y + ((playerPos.y + dy) - this.cameraPos.y) * factor,
            this.cameraPos.z + (playerPos.z - this.cameraPos.z) * factor
        );
    }

    public boolean shouldModifyCamera() {
        return isEnabled() && !isFreelookHandoverCooldown && (!disableFirstPers.isEnabled() || !firstPerson());
    }
}
