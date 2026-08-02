package com.soarclient.mixin.mixins.minecraft.client.gui;

import com.soarclient.management.mod.api.Position;
import com.soarclient.management.mod.impl.hud.BossBarMod;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.BossEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BossHealthOverlay.class)
public abstract class MixinBossBarHud {

    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private Map<UUID, LerpingBossEvent> events;

    @Shadow
    private void extractBar(GuiGraphicsExtractor graphics, int x, int y, BossEvent event) {
        throw new AssertionError();
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void extractRenderState(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        BossBarMod mod = BossBarMod.getInstance();
        if (mod == null) {
            return;
        }
        if (mod.isEnabled() && !mod.isVanillaPosition()) {
            Position position = mod.getPosition();
            renderCustom(graphics, (int) position.getX(), (int) position.getY());
            position.setScale(1.0F);
            position.setSize(182, 14);
            ci.cancel();
        } else if (!mod.isEnabled()) {
            ci.cancel();
        }
    }

    private void renderCustom(GuiGraphicsExtractor graphics, int x, int y) {
        if (events.isEmpty()) {
            return;
        }

        graphics.nextStratum();
        ProfilerFiller profiler = Profiler.get();
        profiler.push("bossHealth");
        int currentY = y;

        for (LerpingBossEvent event : events.values()) {
            Component text = event.getName();
            int textX = x - minecraft.font.width(text) / 2;
            graphics.text(minecraft.font, text, textX, currentY, -1);
            extractBar(graphics, x - 91, currentY + 9, event);

            currentY += 19;
            if (currentY >= graphics.guiHeight() / 3) {
                break;
            }
        }

        profiler.pop();
    }
}
