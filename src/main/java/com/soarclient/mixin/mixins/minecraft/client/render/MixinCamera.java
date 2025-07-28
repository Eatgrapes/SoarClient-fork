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

    @Unique private Entity soarClient$focusedEntity;
    @Unique private boolean soarClient$wasFreelookActive = false;

    // 我们需要先拿到实体，这个小咒语负责准备工作
    @Inject(method = "update", at = @At("HEAD"))
    private void soarClient$prepareFocusedEntity(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo info) {
        this.soarClient$focusedEntity = focusedEntity;
    }

    // 这就是【黄金时刻·奇迹咒语】！ (ﾉ>ω<)ﾉ
    // 它在游戏设置完自己的“角度”之后，设置“位置”之前发动！
    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V", ordinal = 1, shift = At.Shift.AFTER))
    private void soarClient$miracleAtTheGoldenMoment(CallbackInfo ci) {
        if (!(this.soarClient$focusedEntity instanceof ClientPlayerEntity)) return;

        ClientPlayerEntity player = (ClientPlayerEntity) this.soarClient$focusedEntity;
        IMixinCameraEntity cameraOverriddenEntity = (IMixinCameraEntity) player;
        FreelookMod freelookMod = FreelookMod.getInstance();
        ActionCameraMod actionCamera = Soar.getInstance().getModManager().getMod(ActionCameraMod.class);

        // 如果上一帧的暂停开关开着，这一帧就把它关上，让 ActionCamera 恢复工作
        if (actionCamera != null && actionCamera.isFreelookHandoverCooldown) {
            actionCamera.isFreelookHandoverCooldown = false;
        }

        boolean isCurrentlyActive = freelookMod.isEnabled() && freelookMod.isActive();

        if (isCurrentlyActive) {
            // [当自由视角开着]
            if (!this.soarClient$wasFreelookActive) {
                // 第一次开启时，同步视角
                cameraOverriddenEntity.setCameraYaw(player.getYaw());
                cameraOverriddenEntity.setCameraPitch(player.getPitch());
            }
            // 覆盖镜头的角度
            this.setRotation(cameraOverriddenEntity.getCameraYaw(), cameraOverriddenEntity.getCameraPitch());
        }
        else if (this.soarClient$wasFreelookActive) {
            // [当自由视角【刚刚被关掉】的时候！]
            // 这个时刻，就是我们的黄金时刻！
            float finalYaw = cameraOverriddenEntity.getCameraYaw();
            float finalPitch = cameraOverriddenEntity.getCameraPitch();

            // 1. 同步玩家身体朝向
            player.setYaw(finalYaw);
            player.setPitch(finalPitch);

            // 2. 暂停并重置 ActionCamera
            if (actionCamera != null) {
                actionCamera.isFreelookHandoverCooldown = true; // 打开暂停开关
                actionCamera.resetCameraPos();
            }

            // 3. 为了万无一失，再强行设置一次最终的正确角度
            this.setRotation(finalYaw, finalPitch);
        }

        // 更新记录，为下一帧做准备
        this.soarClient$wasFreelookActive = isCurrentlyActive;
    }

    // 这个咒语负责让 ActionCamera 动起来，我们保持不变
    @ModifyArgs(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setPos(DDD)V", ordinal = 0))
    private void onSetCameraPosition(Args args) {
        ActionCameraMod actionCamera = Soar.getInstance().getModManager().getMod(ActionCameraMod.class);
        if (actionCamera == null || !actionCamera.shouldModifyCamera()) return;

        Vec3d cameraPos = actionCamera.getCameraPos();
        if (cameraPos == null) return;

        // 直接设置相机位置，不做额外判断
        args.set(0, cameraPos.x);
        args.set(1, cameraPos.y);
        args.set(2, cameraPos.z);
    }
}
