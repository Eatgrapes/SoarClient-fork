package com.soarclient.mixin.mixins.minecraft.client.render;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import com.soarclient.Soar;
import com.soarclient.management.mod.impl.player.FreelookMod;
import com.soarclient.management.mod.impl.render.ActionCameraMod;
import com.soarclient.mixin.interfaces.IMixinCameraEntity;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;

@Mixin(Camera.class)
public abstract class MixinCamera {
    @Shadow protected abstract void setPos(double x, double y, double z);
    @Shadow protected abstract void setRotation(float yaw, float pitch);

    @Unique private boolean firstTime = true;
    @Unique private Entity focusedEntity;

    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V", ordinal = 1, shift = At.Shift.AFTER))
    public void lockRotation(BlockView focusedBlock, Entity cameraEntity, boolean isThirdPerson, boolean isFrontFacing, float tickDelta, CallbackInfo ci) {

        MinecraftClient client = MinecraftClient.getInstance();

        if (FreelookMod.getInstance().isEnabled() && FreelookMod.getInstance().isActive() && cameraEntity instanceof ClientPlayerEntity) {
            IMixinCameraEntity cameraOverriddenEntity = (IMixinCameraEntity) cameraEntity;

            if (firstTime && MinecraftClient.getInstance().player != null) {
                cameraOverriddenEntity.setCameraPitch(client.player.getPitch());
                cameraOverriddenEntity.setCameraYaw(client.player.getYaw());
                firstTime = false;
            }
            this.setRotation(cameraOverriddenEntity.getCameraYaw(), cameraOverriddenEntity.getCameraPitch());

        }
        if (FreelookMod.getInstance().isEnabled() && !FreelookMod.getInstance().isActive() && cameraEntity instanceof ClientPlayerEntity) {
            firstTime = true;
        }
    }

    @Inject(method = "update", at = @At("HEAD"))
    private void onUpdateHead(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo info) {
        this.focusedEntity = focusedEntity;
    }

    @ModifyArgs(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setPos(DDD)V"))
    private void onSetCameraPosition(Args args) {
        ActionCameraMod actionCamera = Soar.getInstance().getModManager().getMod(ActionCameraMod.class);

        if (actionCamera != null && actionCamera.isEnabled() && actionCamera.shouldModifyCamera() && focusedEntity != null) {
            Vec3d playerPos = focusedEntity.getPos();
            actionCamera.update(playerPos);

            Vec3d cameraPos = actionCamera.getCameraPos();
            if (cameraPos != null) {
                args.set(0, cameraPos.x);
                args.set(1, cameraPos.y);
                args.set(2, cameraPos.z);
            }
        }
    }
}
