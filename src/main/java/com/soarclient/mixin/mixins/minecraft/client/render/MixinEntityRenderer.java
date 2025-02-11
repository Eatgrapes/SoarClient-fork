package com.soarclient.mixin.mixins.minecraft.client.render;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.soarclient.Soar;
import com.soarclient.management.mod.impl.misc.HypixelMod;
import com.soarclient.utils.server.Server;
import com.soarclient.utils.server.ServerUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.Entity;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer<T extends Entity, S extends EntityRenderState> {

	@Shadow
	@Final
	private Font textRenderer;

	@Inject(method = "renderLabelIfPresent", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/font/TextRenderer;draw(Lnet/minecraft/text/Text;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;II)I", ordinal = 1))
	private void onRenderLevelHead(S inState, Component text, PoseStack matrices, MultiBufferSource vertexConsumers,
			int light, CallbackInfo ci) {

		Minecraft client = Minecraft.getInstance();

		if (inState instanceof PlayerRenderState state) {
			if (ServerUtils.isJoin(Server.HYPIXEL)) {

				AbstractClientPlayer player = (AbstractClientPlayer) client.level.getEntity(state.id);

				if (player != null && text.getString().contains(player.getName().getString())) {

					if (HypixelMod.getInstance().isEnabled()
							&& HypixelMod.getInstance().getLevelHeadSetting().isEnabled()) {
						String levelText = ChatFormatting.AQUA.toString() + "Level: " + ChatFormatting.YELLOW.toString()
								+ Soar.getInstance().getHypixelManager()
										.getByUuid(player.getUUID().toString().replace("-", "")).getNetworkLevel();

						float x = -textRenderer.width(levelText) / 2F;
						float y = text.getString().contains("deadmau5") ? -20 : -10;
						int color = (int) (client.options.getBackgroundOpacity(0.25F) * 255.0F) << 24;

						Matrix4f matrix4f = matrices.last().pose();

						textRenderer.drawInBatch(levelText, x, y, CommonColors.WHITE, false, matrix4f, vertexConsumers,
								Font.DisplayMode.NORMAL, color, light);
					}
				}
			}
		}
	}
}
