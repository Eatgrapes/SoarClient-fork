package com.soarclient.mixin.mixins.minecraft.client;

import java.io.File;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.soarclient.Soar;
import com.soarclient.event.EventBus;
import com.soarclient.event.client.ClientTickEvent;
import com.soarclient.event.client.GameLoopEvent;
import com.soarclient.libraries.browser.JCefBrowser;
import com.soarclient.management.config.ConfigType;
import com.soarclient.management.mod.impl.player.HitDelayFixMod;
import com.soarclient.management.mod.impl.player.OldAnimationsMod;
import com.soarclient.mixin.interfaces.IMixinLivingEntity;
import com.soarclient.mixin.interfaces.IMixinMinecraftClient;
import com.soarclient.shader.impl.KawaseBlur;
import com.soarclient.skia.context.SkiaContext;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.util.Window;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

@Mixin(value = MinecraftClient.class, priority = 300)
public abstract class MixinMinecraftClient implements IMixinMinecraftClient {

	@Shadow
	@Final
	private Window window;

	@Shadow
	public int attackCooldown;

	@Shadow
	public ClientPlayerInteractionManager interactionManager;

	@Final
	@Shadow
	public ParticleManager particleManager;

	@Shadow
	public GameOptions options;

	@Shadow
	public HitResult crosshairTarget;

	@Shadow
	public ClientWorld world;

	@Shadow
	public ClientPlayerEntity player;

	@Shadow
	public abstract String getWindowTitle();

	@Unique
	private File assetDir;

	@Inject(method = "<init>(Lnet/minecraft/client/RunArgs;)V", at = @At("TAIL"))
	public void onInit(RunArgs args, CallbackInfo ci) {
		assetDir = args.directories.assetDir;
	}

	@Inject(method = "stop", at = @At("HEAD"))
	public void onStop(CallbackInfo ci) {
		Soar.getInstance().getConfigManager().save(ConfigType.MOD);
		JCefBrowser.close();
	}

	@Inject(method = "handleBlockBreaking", at = @At("HEAD"), cancellable = true)
	private void handleBlockBreaking(boolean breaking, CallbackInfo ci) {

		if (OldAnimationsMod.getInstance().isEnabled() && OldAnimationsMod.getInstance().isOldBreaking()) {
			if (this.options.attackKey.isPressed() && this.options.useKey.isPressed()) {

				if (breaking && this.crosshairTarget != null && this.crosshairTarget.getType() == Type.BLOCK) {

					BlockHitResult blockHitResult = (BlockHitResult) this.crosshairTarget;
					BlockPos blockPos = blockHitResult.getBlockPos();

					if (!this.world.getBlockState(blockPos).isAir()) {
						Direction direction = blockHitResult.getSide();
						this.particleManager.addBlockBreakingParticles(blockPos, direction);
						((IMixinLivingEntity) player).fakeSwingHand(Hand.MAIN_HAND);
					}
				}
			}
		}
	}

	@Inject(method = "doAttack", at = @At("HEAD"))
	private void onHitDelayFix(CallbackInfoReturnable<Boolean> cir) {
		if (HitDelayFixMod.getInstance().isEnabled()) {
			attackCooldown = 0;
		}
	}

	/**
	 * Overrides the vanilla Minecraft window title update method to customize the window title format.
	 * The format will be: "Soar Client v[version] for [original window title]"
	 * @reason To display custom client branding in the window title
	 * @see net.minecraft.client.MinecraftClient#updateWindowTitle()
	 * @author EldoDebug
	 */
	@Overwrite
	public void updateWindowTitle() {
		this.window.setTitle(Soar.getInstance().getName() + " Client v" + Soar.getInstance().getVersion() + " for "
				+ getWindowTitle());
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	public void init(CallbackInfo ci) {
		SkiaContext.createSurface(window.getWidth(), window.getHeight());
		Soar.getInstance().start();
	}

	@Inject(method = "tick", at = @At("HEAD"))
	public void onClientTick(CallbackInfo ci) {
		EventBus.getInstance().post(new ClientTickEvent());
	}

	@Inject(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;printCrashReport()V"))
	public void onGameLoop(CallbackInfo ci) {
		EventBus.getInstance().post(new GameLoopEvent());
	}

	@Inject(method = "onResolutionChanged", at = @At("TAIL"))
	public void onResolutionChanged(CallbackInfo info) {
		KawaseBlur.GUI_BLUR.resize();
		KawaseBlur.INGAME_BLUR.resize();
	}

	@Override
	public File getAssetDir() {
		return this.assetDir;
	}
}
