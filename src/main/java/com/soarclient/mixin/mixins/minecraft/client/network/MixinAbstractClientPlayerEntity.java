package com.soarclient.mixin.mixins.minecraft.client.network;

import com.mojang.authlib.GameProfile;
import com.soarclient.Soar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.core.ClientAsset;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbstractClientPlayer.class, priority = 2000)
public abstract class MixinAbstractClientPlayerEntity extends Player {

    @Unique
    private boolean enableCape;

    @Unique
    private boolean shownCape = false;

    public MixinAbstractClientPlayerEntity(Level world, GameProfile gameProfile) {
        super(world, gameProfile);
    }


    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    public void getSkin(CallbackInfoReturnable<PlayerSkin> cir) {
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (localPlayer == null) {
            return;
        }

        boolean isSameUuid = this.getUUID().equals(localPlayer.getUUID());
        boolean isSameName = this.getName().getString().equals(localPlayer.getName().getString());

        if (!isSameUuid || !isSameName) {
            return;
        }

        Identifier customCape = Soar.getInstance().getCapeManager().getSelectedCapeTexture();
        if (customCape != null) {
            PlayerSkin current = cir.getReturnValue();
            cir.setReturnValue(new PlayerSkin(
                current.body(),
                new ClientAsset.ResourceTexture(customCape, customCape),
                current.elytra(),
                current.model(),
                current.secure()
            ));
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> data) {
        super.onSyncedDataUpdated(data);
        if (DATA_PLAYER_MODE_CUSTOMISATION.equals(data)) {
            boolean showCape = isModelPartShown(PlayerModelPart.CAPE);
            if (showCape != shownCape) {
                shownCape = showCape;
            }
        }
    }

    @Unique
    public void enableCapeNextTick() {
        enableCape = true;
    }
}
