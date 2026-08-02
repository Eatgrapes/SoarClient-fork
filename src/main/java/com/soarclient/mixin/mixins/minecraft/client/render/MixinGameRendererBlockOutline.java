package com.soarclient.mixin.mixins.minecraft.client.render;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameRenderer.class)
public class MixinGameRendererBlockOutline {
    @Final
    @Shadow
    private Minecraft minecraft;

    @ModifyReturnValue(method = "shouldRenderBlockOutline", at = @At("RETURN"))
    private boolean overrideRenderingCondition(boolean original) {

        HitResult hitResult = this.minecraft.hitResult;
        if (!(hitResult != null && hitResult.getType() == HitResult.Type.BLOCK)) {
            return original;
        }

        assert this.minecraft.level != null;
        BlockPos blockPos = ((BlockHitResult)hitResult).getBlockPos();
        BlockState blockState = this.minecraft.level.getBlockState(blockPos);

        if (blockState.getBlock() == Blocks.BARRIER) {
            return false;
        }

        if (blockState.getBlock() == Blocks.TALL_GRASS || blockState.getBlock() == Blocks.SHORT_GRASS) {
            return false;
        }

        assert this.minecraft.gameMode != null;
        boolean adventure = this.minecraft.gameMode.getPlayerMode() == GameType.ADVENTURE;
        boolean spectator = this.minecraft.gameMode.getPlayerMode() == GameType.SPECTATOR;
        boolean hiddenHud = this.minecraft.gui.hud.isHidden();

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
