package com.soarclient.management.mod.impl.hud;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import com.soarclient.event.EventBus;
import com.soarclient.event.client.ClientTickEvent;
import com.soarclient.event.client.RenderSkiaEvent;
import com.soarclient.gui.edithud.api.HUDCore;
import com.soarclient.management.mod.api.hud.HUDMod;
import com.soarclient.management.mod.settings.impl.BooleanSetting;
import com.soarclient.skia.Skia;
import com.soarclient.skia.font.Fonts;
import com.soarclient.skia.font.Icon;

import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import io.github.humbleui.types.Rect;

public class PotionStatusMod extends HUDMod {

    private int maxString;

    private final BooleanSetting backgroundSetting = new BooleanSetting("setting.background",
        "setting.background1.description", Icon.IMAGE, this, true);

    private final BooleanSetting compactSetting = new BooleanSetting("setting.compact", "setting.compact.description",
        Icon.COMPRESS, this, false);

    private Collection<StatusEffectInstance> potions;

    private float animatedWidth, animatedHeight;
    private float targetWidth, targetHeight;

    public PotionStatusMod() {
        super("mod.potionstatus.name", "mod.potionstatus.description", Icon.HEALING);
        this.animatedWidth = 0;
        this.animatedHeight = 0;
        this.targetWidth = 0;
        this.targetHeight = 0;
        this.potions = Collections.emptyList();
    }

    public final EventBus.EventListener<ClientTickEvent> onClientTick = event -> {
        if (HUDCore.isEditing) {
            potions = Arrays.asList(
                new StatusEffectInstance(StatusEffects.SPEED, 1200, 0),
                new StatusEffectInstance(StatusEffects.REGENERATION, 600, 1)
            );
        } else {
            if (client.player != null) {
                potions = client.player.getStatusEffects();
            } else {
                potions = Collections.emptyList();
            }
        }

        calculateDimensions();

        int ySize = compactSetting.isEnabled() ? 16 : 23;

        this.targetWidth = potions.isEmpty() ? 0 : maxString + 29;
        this.targetHeight = potions.isEmpty() ? 0 : (ySize * potions.size()) + 6;
    };

    public final EventBus.EventListener<RenderSkiaEvent> onRenderSkia = event -> {
        this.draw();
    };

    private void draw() {
        float animationSpeed = 0.15f;
        float diffW = targetWidth - animatedWidth;
        float diffH = targetHeight - animatedHeight;

        if (Math.abs(diffW) > 0.5f) {
            animatedWidth += diffW * animationSpeed;
        } else {
            animatedWidth = targetWidth;
        }

        if (Math.abs(diffH) > 0.5f) {
            animatedHeight += diffH * animationSpeed;
        } else {
            animatedHeight = targetHeight;
        }

        if (animatedWidth < 1) {
            if (position.getWidth() != 0 || position.getHeight() != 0) {
                position.setSize(0, 0);
            }
            return;
        }

        this.begin();
        if (backgroundSetting.isEnabled()) {
            this.drawBackground(getX(), getY(), animatedWidth, animatedHeight);
        }

        float animationProgress = targetWidth > 0 ? animatedWidth / targetWidth : 0;
        if (animationProgress > 0.85f) {
            int ySize = compactSetting.isEnabled() ? 16 : 23;
            int offsetY = 16;

            for (StatusEffectInstance potionEffect : potions) {
                drawPotionEffect(potionEffect, offsetY);
                offsetY += ySize;
            }
        }

        this.finish();
        position.setSize(animatedWidth, animatedHeight);
    }

    private void drawPotionEffect(StatusEffectInstance potionEffect, int offsetY) {
        StatusEffect effect = potionEffect.getEffectType().value();
        drawPotionIcon(effect, offsetY);
        String name = I18n.translate(effect.getTranslationKey());

        if (potionEffect.getAmplifier() > 0) {
            name = name + " " + I18n.translate("enchantment.level." + (potionEffect.getAmplifier() + 1));
        }

        String time = formatDuration(potionEffect);

        if (compactSetting.isEnabled()) {
            this.drawText(name + " | " + time, getX() + 20, getY() + offsetY - 10.5F, Fonts.getRegular(9));
        } else {
            this.drawText(name, getX() + 25, getY() + offsetY - 12, Fonts.getRegular(9));
            this.drawText(time, getX() + 25, getY() + offsetY - 1, Fonts.getRegular(8));
        }
    }

    private void drawPotionIcon(StatusEffect effect, int offsetY) {
        Identifier effectId = Registries.STATUS_EFFECT.getId(effect);
        String effectName = effectId.getPath().toLowerCase().replace(" ", "_");
        String texturePath = "textures/mob_effect/" + effectName + ".png";
        float iconSize = compactSetting.isEnabled() ? 13 : 18;
        float iconX = getX() + (compactSetting.isEnabled() ? 1 : 4);
        float iconY = getY() + offsetY - (compactSetting.isEnabled() ? 11 : 12);
        try {
            Skia.drawMinecraftImage(texturePath, iconX, iconY, iconSize, iconSize);
        } catch (Exception e) {
            drawIconPlaceholder(iconX, iconY, iconSize);
        }
    }

    private void drawIconPlaceholder(float x, float y, float size) {
        Skia.drawRoundedRect(x, y, size, size, 2, this.getDesign().getTextColor().darker());
    }

    private String formatDuration(StatusEffectInstance effect) {
        int duration = effect.getDuration();
        if (duration == -1) {
            return "∞";
        }
        int minutes = duration / 1200;
        int seconds = (duration % 1200) / 20;
        return String.format("%d:%02d", minutes, seconds);
    }

    private void calculateDimensions() {
        maxString = 0;
        if (potions.isEmpty()) {
            return;
        }

        for (StatusEffectInstance potionEffect : potions) {
            StatusEffect effect = potionEffect.getEffectType().value();
            String name = I18n.translate(effect.getTranslationKey());

            if (potionEffect.getAmplifier() > 0) {
                name = name + " " + I18n.translate("enchantment.level." + (potionEffect.getAmplifier() + 1));
            }

            String time = formatDuration(potionEffect);
            float currentWidth = 0;

            if (compactSetting.isEnabled()) {
                Rect textBounds = Skia.getTextBounds(name + " | " + time, Fonts.getRegular(9));
                currentWidth = textBounds.getWidth() - 4;
            } else {
                Rect nameBounds = Skia.getTextBounds(name, Fonts.getRegular(9));
                Rect timeBounds = Skia.getTextBounds(time, Fonts.getRegular(8));
                currentWidth = Math.max(nameBounds.getWidth(), timeBounds.getWidth());
            }

            if (maxString < currentWidth) {
                maxString = (int) currentWidth;
            }
        }
    }

    @Override
    public float getRadius() {
        return 6;
    }
}
