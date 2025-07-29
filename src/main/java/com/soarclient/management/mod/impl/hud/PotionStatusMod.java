package com.soarclient.management.mod.impl.hud;

import java.util.Arrays;
import java.util.Collection;

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

    private int maxString, prevPotionCount;

    private BooleanSetting compactSetting = new BooleanSetting("setting.compact", "setting.compact.description",
        Icon.COMPRESS, this, false);

    private Collection<StatusEffectInstance> potions;

    public PotionStatusMod() {
        super("mod.potionstatus.name", "mod.potionstatus.description", Icon.HEALING);
    }

    public final EventBus.EventListener<ClientTickEvent> onClientTick = event -> {
        if (HUDCore.isEditing || client.player == null) {
            potions = Arrays.asList(
                new StatusEffectInstance(StatusEffects.SPEED, 1200, 0),
                new StatusEffectInstance(StatusEffects.REGENERATION, 600, 1)
            );
        } else {
            potions = client.player.getStatusEffects();
        }
    };

    public final EventBus.EventListener<RenderSkiaEvent> onRenderSkia = event -> {
        this.draw();
    };

    private void draw() {
        int ySize = compactSetting.isEnabled() ? 16 : 23;
        int offsetY = 16;

        if (potions.isEmpty()) {
            maxString = 0;
            return;
        }

        calculateDimensions(ySize);

        this.begin();
        this.drawBackground(getX(), getY(), maxString + 29, (ySize * potions.size()) + 2);

        for (StatusEffectInstance potionEffect : potions) {
            StatusEffect effect = potionEffect.getEffectType().value();

            drawPotionIcon(effect, offsetY);

            String name = I18n.translate(effect.getTranslationKey());

            if (potionEffect.getAmplifier() == 1) {
                name = name + " " + I18n.translate("enchantment.level.2");
            } else if (potionEffect.getAmplifier() == 2) {
                name = name + " " + I18n.translate("enchantment.level.3");
            } else if (potionEffect.getAmplifier() == 3) {
                name = name + " " + I18n.translate("enchantment.level.4");
            }

            String time = formatDuration(potionEffect);

            if (compactSetting.isEnabled()) {
                this.drawText(name + " | " + time, getX() + 20, getY() + offsetY - 10.5F, Fonts.getRegular(9));
            } else {
                this.drawText(name, getX() + 25, getY() + offsetY - 12, Fonts.getRegular(9));
                this.drawText(time, getX() + 25, getY() + offsetY - 1, Fonts.getRegular(8));
            }

            offsetY += ySize;
        }

        this.finish();
        position.setSize(maxString + 29, (ySize * potions.size()) + 2);
    }

    private void drawPotionIcon(StatusEffect effect, int offsetY) {
        Identifier effectId = Registries.STATUS_EFFECT.getId(effect);
        String effectName = effectId.getPath().toLowerCase().replace(" ", "_");//OMG IT IS NullPointerException(maybe)
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
        Skia.drawRoundedRect(x, y, size, size, 2,
            this.getDesign().getTextColor().darker());
    }

    private String formatDuration(StatusEffectInstance effect) {
        int duration = effect.getDuration();
        int minutes = duration / 1200;
        int seconds = (duration % 1200) / 20;
        return String.format("%d:%02d", minutes, seconds);
    }

    private void calculateDimensions(int ySize) {
        maxString = 0;

        for (StatusEffectInstance potionEffect : potions) {
            StatusEffect effect = potionEffect.getEffectType().value();
            String name = I18n.translate(effect.getTranslationKey());

            if (potionEffect.getAmplifier() == 1) {
                name = name + " " + I18n.translate("enchantment.level.2");
            } else if (potionEffect.getAmplifier() == 2) {
                name = name + " " + I18n.translate("enchantment.level.3");
            } else if (potionEffect.getAmplifier() == 3) {
                name = name + " " + I18n.translate("enchantment.level.4");
            }

            String time = formatDuration(potionEffect);

            if (compactSetting.isEnabled()) {
                Rect textBounds = Skia.getTextBounds(name + " | " + time, Fonts.getRegular(9));
                if (maxString < textBounds.getWidth() || prevPotionCount != potions.size()) {
                    maxString = (int) textBounds.getWidth() - 4;
                }
            } else {
                Rect nameBounds = Skia.getTextBounds(name, Fonts.getRegular(9));
                Rect timeBounds = Skia.getTextBounds(time, Fonts.getRegular(8));

                float levelWidth = nameBounds.getWidth();
                float timeWidth = timeBounds.getWidth();

                if (maxString < levelWidth || maxString < timeWidth || prevPotionCount != potions.size()) {
                    if (levelWidth > timeWidth) {
                        maxString = (int) levelWidth;
                    } else {
                        maxString = (int) timeWidth;
                    }
                    prevPotionCount = potions.size();
                }
            }
        }
    }

    @Override
    public float getRadius() {
        return 6;
    }
}
