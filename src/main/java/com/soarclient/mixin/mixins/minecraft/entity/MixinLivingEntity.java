package com.soarclient.mixin.mixins.minecraft.entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.soarclient.management.mod.impl.hud.JumpResetIndicatorMod;
import com.soarclient.management.mod.impl.player.NoJumpDelayMod;
import com.soarclient.mixin.interfaces.IMixinLivingEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity implements IMixinLivingEntity {

	@Shadow
	private int noJumpDelay;

	@Shadow
	public int swingTime;

	@Shadow
	public boolean swinging;

	@Shadow
	public InteractionHand swingingArm;

	@Inject(method = "aiStep", at = @At("HEAD"))
	public void onNoJumpDelay(CallbackInfo ci) {
		if (NoJumpDelayMod.getInstance().isEnabled()) {
			noJumpDelay = 0;
		}
	}

	@Inject(method = "jumpFromGround", at = @At("HEAD"))
	private void onJump(CallbackInfo info) {

		JumpResetIndicatorMod mod = JumpResetIndicatorMod.getInstance();
		Minecraft client = Minecraft.getInstance();

		if ((LivingEntity) (Object) this == client.player) {
			mod.setJumpAge(client.player.tickCount);
			mod.setLastTime(System.currentTimeMillis());
		}
	}

	@Inject(method = "handleDamageEvent", at = @At("HEAD"))
	private void onDamage(net.minecraft.world.damagesource.DamageSource source, CallbackInfo info) {

		JumpResetIndicatorMod mod = JumpResetIndicatorMod.getInstance();
		Minecraft client = Minecraft.getInstance();

		if ((LivingEntity) (Object) this == client.player) {
			mod.setHurtAge(client.player.tickCount);
		}
	}

	@Override
	public void fakeSwingHand(InteractionHand hand) {
		if (!this.swinging || this.swingTime >= 3 || this.swingTime < 0) {
			this.swingTime = -1;
			this.swinging = true;
			this.swingingArm = hand;
		}
	}
}
