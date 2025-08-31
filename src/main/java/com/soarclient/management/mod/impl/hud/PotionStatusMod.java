package com.soarclient.management.mod.impl.hud;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import com.soarclient.event.EventBus;
import com.soarclient.event.client.ClientTickEvent;
import com.soarclient.event.client.RenderSkiaEvent;
import com.soarclient.gui.edithud.api.HUDCore;
import com.soarclient.management.mod.api.hud.HUDMod;
import com.soarclient.management.mod.settings.impl.BooleanSetting;
import com.soarclient.management.mod.settings.impl.ComboSetting;
import com.soarclient.skia.Skia;
import com.soarclient.skia.font.Fonts;
import com.soarclient.skia.font.Icon;
import com.soarclient.utils.ColorUtils;

import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import io.github.humbleui.types.Rect;

public class PotionStatusMod extends HUDMod {

    private int maxString;

    private static PotionStatusMod instance;

    private final ComboSetting displayModeSetting = new ComboSetting("setting.display_mode", "setting.display_mode.description", Icon.SETTINGS, this, Arrays.asList("ui.Default", "ui.Modern"), "ui.Default");

    private final BooleanSetting disableVanillaDisplaySetting = new BooleanSetting("setting.vanilla_effects", "setting.vanilla_effects.description", Icon.VISIBILITY_OFF, this, true);

    private final BooleanSetting backgroundSetting = new BooleanSetting("setting.background",
        "setting.background1.description", Icon.IMAGE, this, true){
        @Override
        public boolean isVisible() {
            return !displayModeSetting.getOption().equals("ui.Modern");
        }
    };

    private final BooleanSetting compactSetting = new BooleanSetting("setting.compact", "setting.compact.description",
        Icon.COMPRESS, this, false){
        @Override
        public boolean isVisible() {
            return !displayModeSetting.getOption().equals("ui.Modern");
        }
    };

    private final BooleanSetting showIconSetting = new BooleanSetting("mod.watermark.show_icon1",
        "mod.watermark.show_icon1.description", Icon.IMAGE, this, true){
        @Override
        public boolean isVisible() {
            return !displayModeSetting.getOption().equals("ui.Modern");
        }
    };

    private final BooleanSetting showTextSetting = new BooleanSetting("mod.watermark.show_text",
        "mod.watermark.show_text1.description", Icon.TEXT_FIELDS, this, true){
        @Override
        public boolean isVisible() {
            return !displayModeSetting.getOption().equals("ui.Modern");
        }
    };

    private Collection<StatusEffectInstance> potions;

    private float animatedWidth, animatedHeight;
    private float targetWidth, targetHeight;

    private final Map<StatusEffect, Integer> potionMaxDurations = new HashMap<>();
    private final List<AnimatedPotionEffect> animatedEffects = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();

    public PotionStatusMod() {
        super("mod.potionstatus.name", "mod.potionstatus.description", Icon.HEALING);
        instance = this;
        this.animatedWidth = 0;
        this.animatedHeight = 0;
        this.targetWidth = 0;
        this.targetHeight = 0;
        this.potions = Collections.emptyList();
    }

    public static PotionStatusMod getInstance() {
        return instance;
    }

    public final EventBus.EventListener<ClientTickEvent> onClientTick = event -> {

        Collection<StatusEffectInstance> currentPotions;
        if (HUDCore.isEditing) {
            currentPotions = Arrays.asList(
                new StatusEffectInstance(StatusEffects.SPEED, 1200, 0),
                new StatusEffectInstance(StatusEffects.REGENERATION, 600, 1)
            );
        } else {
            if (client.player != null) {
                currentPotions = client.player.getStatusEffects();
            } else {
                currentPotions = Collections.emptyList();
            }
        }

        for (StatusEffectInstance effect : currentPotions) {
            if (!potionMaxDurations.containsKey(effect.getEffectType().value()) || effect.getDuration() > potionMaxDurations.get(effect.getEffectType().value())) {
                potionMaxDurations.put(effect.getEffectType().value(), effect.getDuration());
            }
        }

        if (displayModeSetting.getOption().equals("ui.Modern")) {
            for (AnimatedPotionEffect ape : animatedEffects) {
                if (currentPotions.stream().noneMatch(p -> p.getEffectType().value().equals(ape.effectType))) {
                    ape.isFadingOut = true;
                }
            }

            for (StatusEffectInstance p : currentPotions) {
                Optional<AnimatedPotionEffect> existing = animatedEffects.stream()
                    .filter(ape -> ape.effectType.equals(p.getEffectType().value()))
                    .findFirst();

                if (existing.isPresent()) {
                    existing.get().effect = p;
                    existing.get().isFadingOut = false;
                } else {
                    float initialY = getY() + (animatedEffects.size() * 30) + 3;
                    animatedEffects.add(new AnimatedPotionEffect(p, initialY));
                }
            }

            long visibleCount = animatedEffects.stream().filter(e -> !e.isFadingOut).count();

            if (visibleCount == 0) {
                this.targetWidth = 0;
            } else {
                this.targetWidth = 120;
            }
            this.targetHeight = visibleCount == 0 ? 0 : (30 * visibleCount) + 6;

        } else {
            this.potions = currentPotions;
            calculateDimensions();

            if (!showIconSetting.isEnabled() && !showTextSetting.isEnabled()) {
                this.targetWidth = 0;
                this.targetHeight = 0;
                return;
            }

            int ySize = compactSetting.isEnabled() ? 16 : 23;
            int iconWidth = showIconSetting.isEnabled() ? 29 : 0;
            int textWidth = showTextSetting.isEnabled() ? maxString : 0;
            int padding = showIconSetting.isEnabled() ? 0 : 8;

            this.targetWidth = potions.isEmpty() ? 0 : iconWidth + textWidth + padding;
            this.targetHeight = potions.isEmpty() ? 0 : (ySize * potions.size()) + 6;
        }
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

        if (animatedWidth < 1 && animatedEffects.stream().allMatch(AnimatedPotionEffect::isDead)) {
            if (position.getWidth() != 0 || position.getHeight() != 0) {
                position.setSize(0, 0);
            }
            animatedEffects.clear();
            particles.clear();
            return;
        }

        this.begin();

        if (displayModeSetting.getOption().equals("ui.Modern")) {
            updateAndDrawParticles();

            float offsetY = 3;
            animatedEffects.removeIf(AnimatedPotionEffect::isDead);

            animatedEffects.sort(Comparator.comparing(ape -> I18n.translate(ape.effect.getTranslationKey())));

            for (AnimatedPotionEffect ape : animatedEffects) {
                if (!ape.isFadingOut) {
                    ape.targetY = getY() + offsetY;
                    offsetY += 30;
                }
                ape.update(animationSpeed);
                drawModernPotionEffect(ape);

                if (ape.justCreated && ape.alpha > 0.1f) {
                    float iconCenterX = getX() + 3 + ape.offsetX + 5 + 9;
                    float iconCenterY = ape.y + 14;
                    spawnParticles(iconCenterX, iconCenterY, new Color(ape.effect.getEffectType().value().getColor()));
                    ape.justCreated = false;
                }
            }
        } else {
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
        }

        this.finish();
        position.setSize(animatedWidth, animatedHeight);
    }

    private void drawModernPotionEffect(AnimatedPotionEffect ape) {
        StatusEffectInstance potionEffect = ape.effect;
        StatusEffect effect = potionEffect.getEffectType().value();
        float alpha = ape.alpha;
        float y = ape.y;
        float x = getX() + 3 + ape.offsetX;

        if (alpha <= 0) return;

        String name = I18n.translate(effect.getTranslationKey());

        if (potionEffect.getAmplifier() > 0) {
            name = name + " " + I18n.translate("enchantment.level." + (potionEffect.getAmplifier() + 1));
        }

        String time = formatDuration(potionEffect);
        int duration = potionEffect.getDuration();
        int maxDuration = potionMaxDurations.getOrDefault(effect, duration);
        if (maxDuration == 0) maxDuration = 1;
        float progress = (float) duration / maxDuration;
        if(duration == -1) progress = 1;

        float width = animatedWidth - 6;
        float height = 28;

        Skia.drawRoundedRect(x, y, width, height, 4, new Color(0, 0, 0, (int)(80 * alpha)));

        Color potionColor = new Color(effect.getColor());
        Skia.drawRoundedRect(x, y, width * progress, height, 4, ColorUtils.applyAlpha(potionColor, (int)(120 * alpha)));

        long currentTime = System.currentTimeMillis();
        float pulse = 1.0f + 0.05f * (float) Math.sin(currentTime / 300.0 + ape.hashCode() / 10.0);

        float baseIconSize = 18;
        float iconSize = baseIconSize * pulse;
        float iconX = x + 5 + (baseIconSize - iconSize) / 2.0f;
        float iconY = y + (height / 2) - (iconSize / 2);

        Identifier effectId = Registries.STATUS_EFFECT.getId(effect);
        String effectName = effectId.getPath().toLowerCase().replace(" ", "_");
        String texturePath = "textures/mob_effect/" + effectName + ".png";
        try {
            Skia.drawMinecraftImage(texturePath, iconX, iconY, iconSize, iconSize);
        } catch (Exception e) {
            Skia.drawRoundedRect(iconX, iconY, iconSize, iconSize, 2, ColorUtils.applyAlpha(this.getDesign().getTextColor().darker(), alpha));
        }

        float textX = x + 5 + baseIconSize + 5;
        Skia.drawText(name, textX, y + 5, ColorUtils.applyAlpha(Color.WHITE, alpha), Fonts.getRegular(9));
        Skia.drawText(time, textX, y + 16, ColorUtils.applyAlpha(new Color(200, 200, 200), alpha), Fonts.getRegular(8));
    }

    private void drawPotionEffect(StatusEffectInstance potionEffect, int offsetY) {
        StatusEffect effect = potionEffect.getEffectType().value();
        float offsetX = 0;

        if (showIconSetting.isEnabled()) {
            drawPotionIcon(effect, offsetY);
            offsetX += (compactSetting.isEnabled() ? 20 : 25);
        } else {
            offsetX += 4;
        }

        if (showTextSetting.isEnabled()) {
            String name = I18n.translate(effect.getTranslationKey());

            if (potionEffect.getAmplifier() > 0) {
                name = name + " " + I18n.translate("enchantment.level." + (potionEffect.getAmplifier() + 1));
            }

            String time = formatDuration(potionEffect);

            if (compactSetting.isEnabled()) {
                this.drawText(name + " | " + time, getX() + offsetX, getY() + offsetY - 10.5F, Fonts.getRegular(9));
            } else {
                this.drawText(name, getX() + offsetX, getY() + offsetY - 12, Fonts.getRegular(9));
                this.drawText(time, getX() + offsetX, getY() + offsetY - 1, Fonts.getRegular(8));
            }
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
        if (potions.isEmpty() || !showTextSetting.isEnabled()) {
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

    private void spawnParticles(float x, float y, Color color) {
        int amount = 20 + random.nextInt(15);
        for (int i = 0; i < amount; i++) {
            particles.add(new Particle(x, y, random, color));
        }
    }

    private void updateAndDrawParticles() {
        Iterator<Particle> iterator = particles.iterator();
        while (iterator.hasNext()) {
            Particle p = iterator.next();
            p.update();
            if (p.isDead()) {
                iterator.remove();
            } else {
                p.draw();
            }
        }
    }

    @Override
    public float getRadius() {
        return 6;
    }

    public boolean shouldDisableVanillaDisplay() {
        return isEnabled() && disableVanillaDisplaySetting.isEnabled();
    }

    private static class AnimatedPotionEffect {
        StatusEffectInstance effect;
        final StatusEffect effectType;
        float y;
        float alpha;
        float offsetX;
        boolean isFadingOut = false;
        boolean justCreated = true;
        float targetY;

        AnimatedPotionEffect(StatusEffectInstance effect, float initialY) {
            this.effect = effect;
            this.effectType = effect.getEffectType().value();
            this.y = initialY;
            this.targetY = initialY;
            this.alpha = 0;
            this.offsetX = -120f;
        }

        void update(float animationSpeed) {
            if (isFadingOut) {
                alpha -= animationSpeed;
                if (alpha < 0) alpha = 0;
            } else {
                alpha += animationSpeed;
                if (alpha > 1) alpha = 1;
            }

            float targetOffsetX = isFadingOut ? -120f : 0f;
            float diffX = targetOffsetX - offsetX;
            if (Math.abs(diffX) > 0.5f) {
                offsetX += diffX * animationSpeed;
            } else {
                offsetX = targetOffsetX;
            }

            float diffY = targetY - y;
            if (Math.abs(diffY) > 0.5f) {
                y += diffY * animationSpeed;
            } else {
                y = targetY;
            }
        }

        boolean isDead() {
            return isFadingOut && alpha <= 0;
        }
    }

    private static class Particle {
        private float x, y;
        private float vx, vy;
        private float alpha;
        private final float size;
        private final float gravity = 0.04f;
        private final float friction = 0.99f;
        private final Color color;

        Particle(float x, float y, Random random, Color color) {
            this.x = x;
            this.y = y;
            double angle = random.nextDouble() * 2 * Math.PI;
            float speed = 1.0f + random.nextFloat() * 1.5f;
            this.vx = (float) (Math.cos(angle) * speed);
            this.vy = (float) (Math.sin(angle) * speed);
            this.alpha = 1.0f;
            this.size = 1.0f + random.nextFloat() * 1.5f;
            this.color = color;
        }

        void update() {
            this.x += this.vx;
            this.y += this.vy;
            this.vy += gravity;
            this.vx *= friction;
            this.vy *= friction;
            this.alpha -= 0.02f;
        }

        void draw() {
            Color particleColor = ColorUtils.applyAlpha(this.color, this.alpha);
            Skia.drawCircle(this.x, this.y, this.size, particleColor);
        }

        boolean isDead() {
            return this.alpha <= 0;
        }
    }
}
