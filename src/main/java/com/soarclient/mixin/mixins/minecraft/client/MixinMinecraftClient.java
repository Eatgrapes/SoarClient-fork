package com.soarclient.mixin.mixins.minecraft.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.platform.Window;
import com.soarclient.Soar;
import com.soarclient.event.EventBus;
import com.soarclient.event.client.ClientTickEvent;
import com.soarclient.event.client.GameLoopEvent;
import com.soarclient.management.config.ConfigType;
import com.soarclient.management.mod.impl.player.HitDelayFixMod;
import com.soarclient.management.mod.impl.player.OldAnimationsMod;
import com.soarclient.mixin.interfaces.IMixinLivingEntity;
import com.soarclient.mixin.interfaces.IMixinMinecraftClient;
import com.soarclient.skia.Skia;
import com.soarclient.skia.context.SkiaContext;
import java.io.File;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Minecraft.class, priority = 300)
public abstract class MixinMinecraftClient implements IMixinMinecraftClient {

    @Shadow @Final private Window window;
    @Shadow private int missTime;
    @Shadow public Options options;
    @Shadow public HitResult hitResult;
    @Shadow public ClientLevel level;
    @Shadow public LocalPlayer player;

    @Unique
    private File assetDir;

    @Inject(method = "<init>(Lnet/minecraft/client/main/GameConfig;)V", at = @At("TAIL"))
    private void onInit(GameConfig config, CallbackInfo ci) {
        assetDir = config.location.assetDirectory;
        Soar.getInstance().start();
    }

    @Inject(method = "stop", at = @At("HEAD"))
    private void onStop(CallbackInfo ci) {
        Soar.getInstance().getConfigManager().save(ConfigType.MOD);
    }

    @Inject(method = "continueAttack(Z)V", at = @At("HEAD"))
    private void handleBlockBreaking(boolean breaking, CallbackInfo ci) {
        OldAnimationsMod mod = OldAnimationsMod.getInstance();
        if (mod == null || !mod.isEnabled() || !mod.isOldBreaking()) {
            return;
        }
        if (!options.keyAttack.isDown() || !options.keyUse.isDown()) {
            return;
        }
        if (!breaking || !(hitResult instanceof BlockHitResult blockHitResult) || level == null || player == null) {
            return;
        }
        if (!level.getBlockState(blockHitResult.getBlockPos()).isAir()) {
            level.addBreakingBlockEffect(blockHitResult.getBlockPos(), blockHitResult.getDirection());
            ((IMixinLivingEntity) player).fakeSwingHand(InteractionHand.MAIN_HAND);
        }
    }

    @Inject(method = "startAttack()Z", at = @At("HEAD"))
    private void onHitDelayFix(CallbackInfoReturnable<Boolean> cir) {
        HitDelayFixMod mod = HitDelayFixMod.getInstance();
        if (mod != null && mod.isEnabled()) {
            missTime = 0;
        }
    }

    @ModifyReturnValue(method = "createTitle()Ljava/lang/String;", at = @At("RETURN"))
    private String customizeWindowTitle(String original) {
        return Soar.getInstance().getName() + " Client v" + Soar.getInstance().getVersion() + " for " + original;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onClientTick(CallbackInfo ci) {
        EventBus.getInstance().post(new ClientTickEvent());
    }

    @Inject(method = "runTick(Z)V", at = @At("HEAD"))
    private void onGameLoop(boolean advanceGameTime, CallbackInfo ci) {
        EventBus.getInstance().post(new GameLoopEvent());
    }

    @Inject(method = "framebufferSizeChanged()V", at = @At("HEAD"))
    private void onResolutionChanged(CallbackInfo ci) {
        SkiaContext.invalidate();
    }

    @Inject(method = "close()V", at = @At("HEAD"))
    private void closeSkia(CallbackInfo ci) {
        Soar.getInstance().close();
        Skia.close();
    }

    @Override
    public File getAssetDir() {
        return assetDir;
    }
}
