package com.soarclient.mixin.mixins.minecraft.entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.soarclient.management.mod.impl.player.ForceMainHandMod;
import com.soarclient.management.mod.impl.player.OldAnimationsMod;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Arm;

@Mixin(PlayerEntity.class)
public class MixinPlayerEntity {

	@Inject(method = "getAttackCooldownProgress", at = @At("HEAD"), cancellable = true)
	public void disableCooldown(CallbackInfoReturnable<Float> cir) {
		if (OldAnimationsMod.getInstance().isEnabled() && OldAnimationsMod.getInstance().isDisableAttackCooldown()) {
			cir.setReturnValue(1F);
		}
	}

	@Inject(method = "getMainArm", at = @At("HEAD"), cancellable = true)
	private void injectGetMainArm(CallbackInfoReturnable<Arm> cir) {

		MinecraftClient client = MinecraftClient.getInstance();
		PlayerEntity player = client.player;
		PlayerEntity e = ((PlayerEntity) (Object) this);

		if (ForceMainHandMod.getInstance().isEnabled() && e.getId() != player.getId()) {
			cir.setReturnValue(ForceMainHandMod.getInstance().isRightHand() ? Arm.RIGHT : Arm.LEFT);
		}
	}
}
