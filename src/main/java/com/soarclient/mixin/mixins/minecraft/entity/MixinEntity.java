package com.soarclient.mixin.mixins.minecraft.entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.soarclient.event.EventBus;
import com.soarclient.event.client.PlayerDirectionChangeEvent;
import com.soarclient.management.mod.impl.player.FreelookMod;
import com.soarclient.mixin.interfaces.IMixinCameraEntity;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

@Mixin(Entity.class)
public abstract class MixinEntity implements IMixinCameraEntity {

	@Unique
	private float cameraPitch;

	@Unique
	private float cameraYaw;

	@Shadow
	public abstract float getXRot();

	@Shadow
	public abstract float getYRot();

	@Inject(method = "turn", at = @At("HEAD"))
	private void onPlayerDirectionChange(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {

		float prevPitch = getXRot();
		float prevYaw = getYRot();
		float pitch = prevPitch + (float) (cursorDeltaY * .15);
		float yaw = prevYaw + (float) (cursorDeltaX * .15);
		pitch = Mth.clamp(pitch, -90.0F, 90.0F);

		EventBus.getInstance().post(new PlayerDirectionChangeEvent(prevPitch, prevYaw, pitch, yaw));
	}

	@Inject(method = "turn", at = @At("HEAD"), cancellable = true)
	public void changeCameraLookDirection(double xDelta, double yDelta, CallbackInfo ci) {
		if (FreelookMod.getInstance().isEnabled() && FreelookMod.getInstance().isActive()
				&& (Entity) (Object) this instanceof LocalPlayer) {
			double pitchDelta = (yDelta * 0.15);
			double yawDelta = (xDelta * 0.15);

			this.cameraPitch = Mth.clamp(this.cameraPitch + (float) pitchDelta, -90.0f, 90.0f);
			this.cameraYaw += (float) yawDelta;

			ci.cancel();

		}
	}

	@Override
	@Unique
	public float getCameraPitch() {
		return this.cameraPitch;
	}

	@Override
	@Unique
	public float getCameraYaw() {
		return this.cameraYaw;
	}

	@Override
	@Unique
	public void setCameraPitch(float pitch) {
		this.cameraPitch = pitch;
	}

	@Override
	@Unique
	public void setCameraYaw(float yaw) {
		this.cameraYaw = yaw;
	}
}
