package com.soarclient.mixin.mixins.minecraft.entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.soarclient.management.mod.impl.player.ForceMainHandMod;
import com.soarclient.management.mod.impl.player.OldAnimationsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;

@Mixin(Player.class)
public class MixinPlayerEntity {

	@Inject(method = "getAttackStrengthScale", at = @At("HEAD"), cancellable = true)
	public void disableCooldown(float partialTick, CallbackInfoReturnable<Float> cir) {
		if (OldAnimationsMod.getInstance().isEnabled() && OldAnimationsMod.getInstance().isDisableAttackCooldown()) {
			cir.setReturnValue(1F);
		}
	}

	@Inject(method = "getMainArm", at = @At("HEAD"), cancellable = true)
	private void injectGetMainArm(CallbackInfoReturnable<HumanoidArm> cir) {

		Minecraft client = Minecraft.getInstance();
		Player player = client.player;
		Player e = ((Player) (Object) this);

		if (ForceMainHandMod.getInstance().isEnabled() && e.getId() != player.getId()) {
			cir.setReturnValue(ForceMainHandMod.getInstance().isRightHand() ? HumanoidArm.RIGHT : HumanoidArm.LEFT);
		}
	}
}
