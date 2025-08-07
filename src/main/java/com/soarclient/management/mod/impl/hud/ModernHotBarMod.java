package com.soarclient.management.mod.impl.hud;

import com.soarclient.event.EventBus;
import com.soarclient.event.client.RenderSkiaEvent;
import com.soarclient.management.mod.api.hud.HUDMod;
import com.soarclient.management.mod.settings.impl.BooleanSetting;
import com.soarclient.management.mod.settings.impl.ColorSetting;
import com.soarclient.skia.font.Fonts;
import com.soarclient.skia.font.Icon;

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
    private final ColorSetting backgroundColor = new ColorSetting("setting.backgroundcolor", "setting.backgroundcolor.description", Icon.SETTINGS, this, new Color(20, 20, 20, 150), false);
    private final ColorSetting selectionColor = new ColorSetting("setting.selectioncolor", "setting.selectioncolor.description", Icon.SETTINGS, this, new Color(100, 100, 255, 200), false);

    public ModernHotBarMod() {
        super("mod.modernhotbar.name", "mod.modernhotbar.desc", Icon.SETTINGS);
    }

    public final EventBus.EventListener<RenderSkiaEvent> onRenderSkia = event -> {
        this.render(0, 0, 0);
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

    private void renderBar(float y, float percentage, Color color) {
        begin();
        drawBackground(0, y, 182f, 4f);
        if (percentage > 0) {
            float barWidth = 182f * percentage;
            drawBackground(0, y, barWidth, 4f);
            drawBlurBackground(0, y, barWidth, 4f);
        }
        finish();
    }

    private void renderItem(ItemStack stack, int x, int y, DrawContext context) {
        begin();
        if (context != null) {
            context.drawItem(stack, x, y);
        }

        if (stack.getCount() > 1) {
            String countText = String.valueOf(stack.getCount());
            float textX = x + 17 - MinecraftClient.getInstance().textRenderer.getWidth(countText);
            float textY = y + 9;

            drawText(countText, textX, textY, Fonts.getRegular(8));
        }
        finish();
    }

    public void render(int mouseX, int mouseY, float delta) {
        if (!isEnabled()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        int width = mc.getWindow().getScaledWidth();
        int height = mc.getWindow().getScaledHeight();
        float x = width / 2f - 91f;
        float y = height - 22f;

        position.setPosition(x, y);

        begin();
        drawBackground(0, 0, 182f, 22f);
        float slotX = mc.player.getInventory().selectedSlot * 20f;
        drawBackground(slotX, 0, 22f, 22f);
        drawBlurBackground(slotX, 0, 22f, 22f);
        finish();

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                int itemX = (int)(x + i * 20f + 3);
                int itemY = (int)(y + 3);
                renderItem(stack, itemX, itemY, null);
            }
        }

        if (showHealthBar.isEnabled()) {
            float healthPercentage = MathHelper.clamp(
                mc.player.getHealth() / mc.player.getMaxHealth(),
                0.0f, 1.0f
            );
            renderBar(-10f, healthPercentage, getHealthColor(healthPercentage));
        }

        if (showHungerBar.isEnabled()) {
            float hungerPercentage = MathHelper.clamp(
                mc.player.getHungerManager().getFoodLevel() / 20.0f,
                0.0f, 1.0f
            );
            renderBar(-5f, hungerPercentage, getHungerColor(hungerPercentage));
        }
    }

    @Override
    public void onEnable() {
        EventBus.getInstance().register(this);
        position.setSize(182f, 22f);
    }

    @Override
    public float getRadius() {
        return 4.0f;
    }
}
