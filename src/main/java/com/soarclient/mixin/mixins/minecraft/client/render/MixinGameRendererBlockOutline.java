package com.soarclient.mixin.mixins.minecraft.client.render;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameRenderer.class)
public class MixinGameRendererBlockOutline {
    @Final
    @Shadow
    private MinecraftClient client;

    @ModifyReturnValue(method = "shouldRenderBlockOutline", at = @At("RETURN"))
    private boolean overrideRenderingCondition(boolean original) {

        HitResult hitResult = this.client.crosshairTarget;
        if (!(hitResult != null && hitResult.getType() == HitResult.Type.BLOCK)) {
            return original;
        }

        assert this.client.world != null;
        BlockPos blockPos = ((BlockHitResult)hitResult).getBlockPos();
        BlockState blockState = this.client.world.getBlockState(blockPos);

        if (blockState.getBlock() == Blocks.BARRIER) {
            return false;
        }

        if (blockState.getBlock() == Blocks.TALL_GRASS || blockState.getBlock() == Blocks.SHORT_GRASS) {
            return false;
        }

        assert this.client.interactionManager != null;
        boolean adventure = this.client.interactionManager.getCurrentGameMode() == GameMode.ADVENTURE;
        boolean spectator = this.client.interactionManager.getCurrentGameMode() == GameMode.SPECTATOR;
        boolean hiddenHud = this.client.options.hudHidden;

        if (adventure || spectator || hiddenHud) {
            if (adventure) {
                return !hiddenHud;
            }

            if (spectator) {
                return !hiddenHud;
            }

            if (hiddenHud) {
                return false;
            }
        }

        return original;
    }
}
