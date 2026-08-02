package com.soarclient.mixin.mixins.minecraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.soarclient.Soar;
import com.soarclient.management.hypixel.api.HypixelUser;
import com.soarclient.management.mod.impl.misc.HypixelMod;
import com.soarclient.utils.server.Server;
import com.soarclient.utils.server.ServerUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer<T extends Entity, S extends EntityRenderState> {

    @Inject(method = "submitNameDisplay(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;I)V", at = @At("TAIL"))
    private <R extends EntityRenderState> void renderLevelHead(R state, PoseStack poseStack,
            SubmitNodeCollector collector, CameraRenderState camera, int offset, CallbackInfo ci) {
        if (!(state instanceof AvatarRenderState avatarState) || avatarState.nameTag == null) {
            return;
        }
        if (!ServerUtils.isJoin(Server.HYPIXEL)) {
            return;
        }

        HypixelMod mod = HypixelMod.getInstance();
        Minecraft minecraft = Minecraft.getInstance();
        if (mod == null || !mod.isEnabled() || !mod.getLevelHeadSetting().isEnabled() || minecraft.level == null) {
            return;
        }
        if (!(minecraft.level.getEntity(avatarState.id) instanceof AbstractClientPlayer player)) {
            return;
        }

        HypixelUser user = Soar.getInstance().getHypixelManager()
                .getByUuid(player.getUUID().toString().replace("-", ""));
        if (user == null) {
            return;
        }

        Component levelText = Component.literal("Level: ")
                .withStyle(ChatFormatting.AQUA)
                .append(Component.literal(user.getNetworkLevel()).withStyle(ChatFormatting.YELLOW));
        collector.submitNameTag(poseStack, avatarState.nameTagAttachment,
                offset - (avatarState.scoreText != null ? 20 : 10), levelText,
                !avatarState.isDiscrete, avatarState.lightCoords, camera);
    }
}
