package com.soarclient.mixin.mixins.minecraft.client.render;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.soarclient.Soar;
import com.soarclient.management.mod.impl.player.FreelookMod;
import com.soarclient.management.mod.impl.player.ZoomMod;
import com.soarclient.management.mod.impl.render.ActionCameraMod;
import com.soarclient.mixin.interfaces.IMixinCameraEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Camera.class)
public abstract class MixinCamera {

    @Shadow
    private void setRotation(float yaw, float pitch) {
        throw new AssertionError();
    }

    @Unique
    private boolean firstTime = true;

    @Inject(method = "alignWithEntity", at = @At("TAIL"))
    private void applyFreelook(float partialTicks, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity cameraEntity = minecraft.getCameraEntity();
        FreelookMod freelook = FreelookMod.getInstance();
        if (freelook != null && freelook.isEnabled() && freelook.isActive() && cameraEntity instanceof LocalPlayer) {
            IMixinCameraEntity camera = (IMixinCameraEntity) cameraEntity;
            if (firstTime && minecraft.player != null) {
                camera.setCameraPitch(minecraft.player.getXRot());
                camera.setCameraYaw(minecraft.player.getYRot());
                firstTime = false;
            }
            setRotation(camera.getCameraYaw(), camera.getCameraPitch());
        } else {
            firstTime = true;
        }
    }

    @ModifyArgs(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V"))
    private void applyActionCamera(Args args) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity focusedEntity = minecraft.getCameraEntity();
        ActionCameraMod actionCamera = Soar.getInstance().getModManager().getMod(ActionCameraMod.class);
        if (actionCamera == null || !actionCamera.isEnabled() || !actionCamera.shouldModifyCamera() || focusedEntity == null) {
            return;
        }

        Vec3 playerPos = focusedEntity.position();
        actionCamera.update(playerPos);
        Vec3 cameraPos = actionCamera.getCameraPos();
        if (cameraPos != null) {
            args.set(0, cameraPos.x);
            args.set(1, cameraPos.y);
            args.set(2, cameraPos.z);
        }
    }

    @ModifyReturnValue(method = "calculateFov(F)F", at = @At("RETURN"))
    private float modifyWorldFov(float original) {
        return zoom(original);
    }

    @ModifyReturnValue(method = "calculateHudFov(F)F", at = @At("RETURN"))
    private float modifyHudFov(float original) {
        return zoom(original);
    }

    @Unique
    private float zoom(float original) {
        ZoomMod zoom = ZoomMod.getInstance();
        return zoom != null && zoom.isEnabled() ? zoom.getFov(original) : original;
    }
}
