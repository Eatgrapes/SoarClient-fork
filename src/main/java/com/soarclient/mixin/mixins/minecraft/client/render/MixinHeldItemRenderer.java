package com.soarclient.mixin.mixins.minecraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.soarclient.management.mod.impl.player.OldAnimationsMod;
import com.soarclient.management.mod.impl.render.CustomHandMod;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class MixinHeldItemRenderer {

    @Shadow
    private void applyItemArmAttackTransform(PoseStack poseStack, HumanoidArm arm, float attack) {
        throw new AssertionError();
    }

    @Inject(method = "submitArmWithItem", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER))
    private void applyHandTransforms(AbstractClientPlayer player, float frameInterp, float xRot,
            InteractionHand hand, float attack, ItemStack itemStack, float inverseArmHeight,
            PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, CallbackInfo ci) {
        CustomHandMod handMod = CustomHandMod.getInstance();
        if (handMod != null && handMod.isEnabled()) {
            poseStack.translate(handMod.getX(), handMod.getY(), handMod.getZ());
            poseStack.scale(handMod.getScale(), handMod.getScale(), handMod.getScale());
        }

        OldAnimationsMod animations = OldAnimationsMod.getInstance();
        if (animations == null || !animations.isEnabled()) {
            return;
        }
        if (itemStack.getItem() instanceof BowItem && animations.isOldBow()) {
            poseStack.translate(0.0F, 0.05F, 0.04F);
            poseStack.scale(0.93F, 1.0F, 1.0F);
        } else if (itemStack.getItem() instanceof FishingRodItem && animations.isOldRod()) {
            poseStack.translate(0.08F, -0.027F, -0.33F);
            poseStack.scale(0.93F, 1.0F, 1.0F);
        }

        if (animations.isOldBreaking() && player.isUsingItem()) {
            ItemUseAnimation useAnimation = itemStack.getUseAnimation();
            if (useAnimation == ItemUseAnimation.EAT || useAnimation == ItemUseAnimation.DRINK
                    || useAnimation == ItemUseAnimation.BLOCK || useAnimation == ItemUseAnimation.BOW) {
                HumanoidArm arm = hand == InteractionHand.MAIN_HAND
                        ? player.getMainArm()
                        : player.getMainArm().getOpposite();
                applyItemArmAttackTransform(poseStack, arm, attack);
            }
        }
    }
}
