package com.soarclient.mixin.client;

import com.soarclient.Soar;
import com.soarclient.management.mod.impl.render.ActionCameraMod;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow protected abstract void setPos(double x, double y, double z);
    @Shadow private Vec3d pos;
    @Unique private Entity focusedEntity;

    @Inject(method = "update", at = @At("HEAD"))
    private void onUpdateHead(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo info) {
        this.focusedEntity = focusedEntity;
    }

    @ModifyArgs(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setPos(DDD)V"))
    private void onSetCameraPosition(Args args) {
        ActionCameraMod actionCamera = Soar.getInstance().getModManager().getMod(ActionCameraMod.class);

        if (actionCamera != null && actionCamera.shouldModifyCamera() && focusedEntity != null) {
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
