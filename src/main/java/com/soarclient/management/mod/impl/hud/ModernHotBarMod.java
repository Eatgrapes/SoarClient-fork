package com.soarclient.management.mod.impl.hud;

import com.soarclient.event.EventBus;
import com.soarclient.event.client.RenderHotbarEvent;
import com.soarclient.event.client.RenderSkiaEvent;
import com.soarclient.event.client.RenderSkiaPostEvent;
import com.soarclient.management.mod.api.hud.HUDMod;
import com.soarclient.management.mod.settings.impl.BooleanSetting;
import com.soarclient.management.mod.settings.impl.ColorSetting;
import com.soarclient.skia.font.Icon;
import com.soarclient.skia.font.Fonts;
import com.soarclient.skia.Skia;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

import java.awt.Color;

public class ModernHotBarMod extends HUDMod {

    private static ModernHotBarMod instance;

    public static ModernHotBarMod getInstance() {
        if (instance == null) {
            instance = new ModernHotBarMod();
        }
        return instance;
    }

    private final BooleanSetting showHealthBar = new BooleanSetting("setting.showhealthbar", "setting.showhealthbar.description", Icon.SETTINGS, this, true);
    private final BooleanSetting showHungerBar = new BooleanSetting("setting.showhungerbar", "setting.showhungerbar.description", Icon.SETTINGS, this, true);

    private long lastTimeNanos = System.nanoTime();
    private int visualSelectedSlot = -1;
    private int animStartSlot = -1;
    private int animTargetSlot = -1;
    private float animProgress = 1f;
    private boolean animating = false;
    private static final float SELECTION_ANIM_DURATION = 0.18f;
    private float animatedSlotX = 0f;

    public ModernHotBarMod() {
        super("mod.modernhotbar.name", "mod.modernhotbar.desc", Icon.SETTINGS);
    }

    @SuppressWarnings("unused")
    public final EventBus.EventListener<RenderSkiaEvent> onRenderSkia = event -> {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        long now = System.nanoTime();
        float delta = (now - lastTimeNanos) / 1_000_000_000.0f;
        lastTimeNanos = now;

        int currentSlot = mc.player.getInventory().selectedSlot;
        if (visualSelectedSlot == -1) {
            visualSelectedSlot = currentSlot;
            animStartSlot = currentSlot;
            animTargetSlot = currentSlot;
            animatedSlotX = currentSlot * 20f;
            animProgress = 1f;
            animating = false;
        }

        if (currentSlot != animTargetSlot) {
            animStartSlot = Math.round(animatedSlotX / 20f);
            animTargetSlot = currentSlot;
            animProgress = 0f;
            animating = true;
        }

        if (animating) {
            animProgress += delta / Math.max(0.0001f, SELECTION_ANIM_DURATION);
            if (animProgress >= 1f) {
                animProgress = 1f;
                animating = false;
                visualSelectedSlot = animTargetSlot;
            }
        }

        float eased = 1f - (float)Math.pow(1f - animProgress, 3);
        float startX = animStartSlot * 20f;
        float targetX = animTargetSlot * 20f;
        animatedSlotX = startX + (targetX - startX) * eased;

        int width = mc.getWindow().getScaledWidth();
        int height = mc.getWindow().getScaledHeight();
        float x = width / 2f - 91f;
        float y = height - 22f;

        position.setSize(182f, 22f);
        position.setPosition(x, y);

        float drawX = getX();
        float drawY = getY();

        begin();
        drawBackground(drawX, drawY, 182f, 22f);
        float slotX = animatedSlotX;
        drawBackground(drawX + slotX, drawY, 22f, 22f);
        drawBlurBackground(drawX + slotX, drawY, 22f, 22f);

        if (!mc.player.getOffHandStack().isEmpty()) {
            float offhandSlotX = drawX - 22f - 4f;
            drawBackground(offhandSlotX, drawY, 22f, 22f);
        }

        if (showHealthBar.isEnabled()) {
            float healthPercentage = MathHelper.clamp(mc.player.getHealth() / mc.player.getMaxHealth(), 0.0f, 1.0f);
            renderBar(drawX, drawY, healthPercentage, getHealthColor(healthPercentage));
        }

        if (showHungerBar.isEnabled()) {
            float hungerPercentage = MathHelper.clamp(mc.player.getHungerManager().getFoodLevel() / 20.0f, 0.0f, 1.0f);
            renderBar(drawX, drawY, hungerPercentage, getHungerColor(hungerPercentage));
        }

        finish();
    };

    @SuppressWarnings("unused")
    public final EventBus.EventListener<RenderSkiaPostEvent> onRenderSkiaPost = event -> {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        float drawX = getX();
        float drawY = getY();

        Skia.save();
        try {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (!stack.isEmpty() && stack.getCount() > 1) {
                    String countText = String.valueOf(stack.getCount());
                    float textWidth = Skia.getTextBounds(countText, Fonts.getRegular(9f)).getWidth();
                    float textXBase = drawX + (i * 20f) + 19f - textWidth;
                    float textY = drawY + 11f;
                    Skia.drawText(countText, textXBase + 0.5f, textY + 0.5f, new Color(0,0,0,160), Fonts.getRegular(9f));
                    Skia.drawText(countText, textXBase, textY, Color.WHITE, Fonts.getRegular(9f));
                }
            }

            ItemStack offhand = mc.player.getOffHandStack();
            if (!offhand.isEmpty() && offhand.getCount() > 1) {
                String countText = String.valueOf(offhand.getCount());
                float textWidth = Skia.getTextBounds(countText, Fonts.getRegular(9f)).getWidth();
                float offhandSlotX = drawX - 22f - 4f;
                float textX = offhandSlotX + 19f - textWidth;
                float textY = drawY + 11f;
                Skia.drawText(countText, textX + 0.5f, textY + 0.5f, new Color(0,0,0,160), Fonts.getRegular(9f));
                Skia.drawText(countText, textX, textY, Color.WHITE, Fonts.getRegular(9f));
            }
        } finally {
            Skia.restore();
        }
    };

    @SuppressWarnings("unused")
    public final EventBus.EventListener<RenderHotbarEvent> onRenderHotbarEvent = event -> {
        DrawContext context = event.getContext();
        if (context == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        int width = mc.getWindow().getScaledWidth();
        int height = mc.getWindow().getScaledHeight();
        float drawX = width / 2f - 91f;
        float drawY = height - 22f;

        event.setCancelled(true);

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                int itemX = (int)(drawX + i * 20f + 3);
                int itemY = (int)(drawY + 3);
                renderItem(stack, itemX, itemY, context);
            }
        }

        ItemStack offhand = mc.player.getOffHandStack();
        if (!offhand.isEmpty()) {
            float offhandSlotX = drawX - 22f - 4f;
            int offhandX = (int)(offhandSlotX + 3);
            int offhandY = (int)(drawY + 3);
            renderItem(offhand, offhandX, offhandY, context);
        }
    };

    private Color getHealthColor(float percentage) {
        int red = percentage < 0.5f ? 255 : (int)(255 - (percentage - 0.5f) * 255);
        int green = percentage > 0.5f ? 255 : (int)(percentage * 2 * 255);
        return new Color(red, green, 50, 200);
    }

    private Color getHungerColor(float percentage) {
        int green = (int)(175 + percentage * 80);
        int blue = (int)(80 + percentage * 40);
        return new Color(76, green, blue, 200);
    }

    private void renderBar(float baseX, float baseY, float percentage, @SuppressWarnings("unused") Color color) {
        begin();
        drawBackground(baseX, baseY, 182f, 4f);
        if (percentage > 0) {
            float barWidth = 182f * percentage;
            drawBackground(baseX, baseY, barWidth, 4f);
            drawBlurBackground(baseX, baseY, barWidth, 4f);
        }
        finish();
    }

    private void renderItem(ItemStack stack, int x, int y, DrawContext context) {
        if (context != null) {
            context.drawItem(stack, x, y);
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        position.setSize(182f, 22f);
    }

    @Override
    public float getRadius() {
        return 4.0f;
    }
}
