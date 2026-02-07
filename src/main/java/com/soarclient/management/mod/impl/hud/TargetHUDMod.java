package com.soarclient.management.mod.impl.hud;

import java.awt.Color;
import java.util.Arrays;

import com.soarclient.Soar;
import com.soarclient.event.EventBus;
import com.soarclient.event.client.ClientTickEvent;
import com.soarclient.event.client.RenderSkiaEvent;
import com.soarclient.gui.edithud.api.HUDCore;
import com.soarclient.management.color.api.ColorPalette;
import com.soarclient.management.mod.api.hud.HUDMod;
import com.soarclient.management.mod.settings.impl.BooleanSetting;
import com.soarclient.management.mod.settings.impl.ComboSetting;
import com.soarclient.skia.Skia;
import com.soarclient.skia.font.Fonts;
import com.soarclient.skia.font.Icon;
import com.soarclient.utils.HealthUtils;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;

public class TargetHUDMod extends HUDMod {

    private static TargetHUDMod instance;
    private PlayerEntity targetPlayer;
    private long lastAttackTime = 0;
    private static final long DISPLAY_DURATION = 10000;

    private float animatedWidth, animatedHeight;
    private float targetWidth, targetHeight;
    private float animatedHealthWidth = 0;
    private float targetHealthWidth = 0;

    private final ComboSetting healthDisplaySetting = new ComboSetting("setting.health.display",
        "setting.health.display.description", Icon.FAVORITE, this,
        Arrays.asList("setting.text", "setting.bar"), "setting.text");

    private final BooleanSetting backgroundSetting = new BooleanSetting("setting.background",
        "setting.background.description", Icon.IMAGE, this, true);

    public TargetHUDMod() {
        super("mod.targethud.name", "mod.targethud.description", Icon.PERSON);
        instance = this;
        this.animatedWidth = 0;
        this.animatedHeight = 0;
        this.targetWidth = 0;
        this.targetHeight = 0;
    }

    public static TargetHUDMod getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        EventBus.getInstance().register(this);
        registerFabricCallbacks();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        EventBus.getInstance().unregister(this);
    }

    private void registerFabricCallbacks() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient && player instanceof ClientPlayerEntity && entity instanceof PlayerEntity newTarget) {
                updateTarget(newTarget);
            }
            return ActionResult.PASS;
        });
    }

    private void updateTarget(PlayerEntity newTarget) {
        if (targetPlayer == null || !targetPlayer.equals(newTarget)) {
            targetPlayer = newTarget;
        }
        lastAttackTime = System.currentTimeMillis();
    }

    public final EventBus.EventListener<ClientTickEvent> onClientTick = event -> {
        boolean shouldShow = shouldShowTarget() || HUDCore.isEditing;

        if (shouldShow) {
            this.targetWidth = 180;
            this.targetHeight = 45;
        } else {
            this.targetWidth = 0;
            this.targetHeight = 0;
        }
    };

    public EventBus.EventListener<RenderSkiaEvent> onRenderSkia = event -> {
        if (!this.isEnabled()) return;

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
        drawTargetInfo(animatedWidth, animatedHeight);
        this.finish();
        position.setSize(animatedWidth, animatedHeight);
    };

    private boolean shouldShowTarget() {
        return targetPlayer != null &&
            (System.currentTimeMillis() - lastAttackTime) < DISPLAY_DURATION;
    }

    private void drawTargetInfo(float width, float height) {
        boolean isEditing = HUDCore.isEditing;
        PlayerEntity displayPlayer;

        if (isEditing) {
            displayPlayer = MinecraftClient.getInstance().player;
        } else {
            displayPlayer = targetPlayer;
        }

        if (displayPlayer == null && !isEditing) {
            return;
        }

        if (!isEditing) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null || client.world.getEntityById(displayPlayer.getId()) == null) {
                targetPlayer = null;
                return;
            }
        }

        final float padding = 4.5f;
        final float avatarSize = height - (padding * 2);

        if (backgroundSetting.isEnabled()) {
            this.drawBackground(getX(), getY(), width, height);
        }

        drawPlayerAvatar(displayPlayer, getX() + padding, getY() + padding, avatarSize);

        String playerName = displayPlayer != null ? displayPlayer.getName().getString() : "Unknown";
        float health = getPlayerHealth(displayPlayer);
        float maxHealth = 20.0f;

        float textX = getX() + padding + avatarSize + padding;
        float nameY = getY() + padding + 5;

        Skia.drawText(playerName, textX, nameY, this.getDesign().getTextColor(), Fonts.getRegular(12));

        if (healthDisplaySetting.getOption().equals("setting.text")) {
            float healthY = nameY + 15;
            Skia.drawText(String.format("%.1f HP", health), textX, healthY,
                this.getDesign().getTextColor(), Fonts.getRegular(10));
        } else {
            drawHealthBar(textX, nameY + 15, health, maxHealth);
        }
    }

    private void drawHealthBar(float x, float y, float health, float maxHealth) {
        float barWidth = 120;
        float barHeight = 8;

        float healthPercentage = Math.min(health / maxHealth, 1.0f);
        targetHealthWidth = barWidth * healthPercentage;

        float animationSpeed = 0.1f;
        float diff = targetHealthWidth - animatedHealthWidth;

        if (Math.abs(diff) > 0.5f) {
            animatedHealthWidth += diff * animationSpeed;
        } else {
            animatedHealthWidth = targetHealthWidth;
        }

        Skia.drawRoundedRect(x, y, barWidth, barHeight, 4, new Color(0, 0, 0, 100));

        if (animatedHealthWidth > 0) {
            Color healthColor = getAnimatedHealthColor();
            Skia.drawRoundedRect(x, y, animatedHealthWidth, barHeight, 4, healthColor);
        }
    }

    private Color getAnimatedHealthColor() {
        try {
            ColorPalette palette = Soar.getInstance().getColorManager().getPalette();

            if (palette == null) {
                return Color.RED;
            }

            long currentTime = System.nanoTime();
            double speed = 0.000000002;
            double cycle = (currentTime * speed) % (2 * Math.PI);

            Color color1 = palette.getPrimary();
            Color color2 = palette.getSecondary();
            Color color3 = palette.getTertiary();

            if (color1 == null) color1 = Color.RED;
            if (color2 == null) color2 = Color.ORANGE;
            if (color3 == null) color3 = Color.YELLOW;

            double normalizedCycle = (cycle / (2 * Math.PI)) * 3;

            Color resultColor;
            if (normalizedCycle < 1) {
                float factor = (float) normalizedCycle;
                resultColor = blendColors(color1, color2, factor);
            } else if (normalizedCycle < 2) {
                float factor = (float) (normalizedCycle - 1);
                resultColor = blendColors(color2, color3, factor);
            } else {
                float factor = (float) (normalizedCycle - 2);
                resultColor = blendColors(color3, color1, factor);
            }

            return resultColor;

        } catch (Exception e) {
            return Color.RED;
        }
    }

    private Color blendColors(Color color1, Color color2, float factor) {
        int red = (int) (color1.getRed() * (1 - factor) + color2.getRed() * factor);
        int green = (int) (color1.getGreen() * (1 - factor) + color2.getGreen() * factor);
        int blue = (int) (color1.getBlue() * (1 - factor) + color2.getBlue() * factor);
        int alpha = (int) (color1.getAlpha() * (1 - factor) + color2.getAlpha() * factor);

        return new Color(red, green, blue, alpha);
    }

    private void drawPlayerAvatar(PlayerEntity player, float x, float y, float size) {
        Skia.save();

        try {
            var skin = getSkin(player);

            Skia.save();
            try {
                Skia.drawPlayerHead(skin, x, y, size, size, 6);
            } finally {
                Skia.restore();
            }
        } catch (Exception e) {
            Skia.drawRoundedRect(x, y, size, size, 6, this.getDesign().getTextColor());
        } finally {
            Skia.restore();
        }
    }

    private Identifier getSkin(PlayerEntity player) {
        if (player instanceof AbstractClientPlayerEntity clientPlayer) {
            if (clientPlayer.getSkinTextures() != null) {
                return clientPlayer.getSkinTextures().texture();
            }
        }
        return null;
    }

    private float getPlayerHealth(PlayerEntity player) {
        if (player == null) {
            return 20.0f;
        }

        try {
            float health = HealthUtils.getActualHealth(player);
            return health > 0 ? health : 20.0f;
        } catch (Exception e) {
            return 20.0f;
        }
    }

    @Override
    public float getRadius() {
        return 6;
    }
}
