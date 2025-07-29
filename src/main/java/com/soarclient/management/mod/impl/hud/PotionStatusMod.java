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

    private int maxString, prevPotionCount;

    private final BooleanSetting compactSetting = new BooleanSetting("setting.compact", "setting.compact.description",
        Icon.COMPRESS, this, false);

    private Collection<StatusEffectInstance> potions;

    // 动画变量
    private float animatedWidth, animatedHeight;

    public PotionStatusMod() {
        super("mod.potionstatus.name", "mod.potionstatus.description", Icon.HEALING);
        this.animatedWidth = 0;
        this.animatedHeight = 0;
        this.potions = Collections.emptyList(); // 初始化为空，避免NullPointerException
    }

    public final EventBus.EventListener<ClientTickEvent> onClientTick = event -> {
        // --- 修正后的逻辑 ---
        if (HUDCore.isEditing) {
            // 1. 如果是编辑模式，则固定显示示例药水
            potions = Arrays.asList(
                new StatusEffectInstance(StatusEffects.SPEED, 1200, 0),
                new StatusEffectInstance(StatusEffects.REGENERATION, 600, 1)
            );
        } else {
            // 2. 如果是实际游戏
            if (client.player != null) {
                // 获取玩家身上的真实药水效果 (如果为空，这里会得到一个空集合，这是正确的)
                potions = client.player.getStatusEffects();
            } else {
                // 如果玩家不存在 (例如在主菜单)，则确保列表为空
                potions = Collections.emptyList();
            }
        }
    };

    public final EventBus.EventListener<RenderSkiaEvent> onRenderSkia = event -> {
        this.draw();
    };

    private void draw() {
        int ySize = compactSetting.isEnabled() ? 16 : 23;

        calculateDimensions(ySize);

        float targetWidth = potions.isEmpty() ? 0 : maxString + 29;
        float targetHeight = potions.isEmpty() ? 0 : (ySize * potions.size()) + 6;

        if (targetWidth == 0 && animatedWidth < 1) {
            animatedWidth = 0;
            animatedHeight = 0;
            return;
        }

        float animationSpeed = 0.15f;
        animatedWidth += (targetWidth - animatedWidth) * animationSpeed;
        animatedHeight += (targetHeight - animatedHeight) * animationSpeed;

        this.begin();
        this.drawBackground(getX(), getY(), animatedWidth, animatedHeight);

        float animationProgress = targetWidth > 0 ? animatedWidth / targetWidth : 0;

        if (animationProgress > 0.95f) {
            int offsetY = 16;

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
        }

        this.finish();
        position.setSize(animatedWidth, animatedHeight);
    }

    // 以下所有方法均保持不变

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
        int minutes = duration / 1200;
        int seconds = (duration % 1200) / 20;
        return String.format("%d:%02d", minutes, seconds);
    }

    private void calculateDimensions(int ySize) {
        if (potions.isEmpty()) {
            maxString = 0;
            return;
        }
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
                }
            }
        }
        prevPotionCount = potions.size();
    }

    @Override
    public float getRadius() {
        return 6;
    }
}
