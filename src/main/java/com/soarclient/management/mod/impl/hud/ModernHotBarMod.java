package com.soarclient.management.mod.impl.hud;

import com.soarclient.event.EventBus;
import com.soarclient.event.client.RenderHotbarEvent;
import com.soarclient.event.client.RenderSkiaEvent;
import com.soarclient.event.client.RenderSkiaPostEvent;
import com.soarclient.management.mod.api.hud.HUDMod;
import com.soarclient.management.mod.settings.impl.BooleanSetting;
import com.soarclient.skia.font.Icon;
import com.soarclient.skia.font.Fonts;
import com.soarclient.skia.Skia;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

import java.awt.Color;

@SuppressWarnings("all")
public class ModernHotBarMod extends HUDMod {

    private static ModernHotBarMod instance;

    public static ModernHotBarMod getInstance() {
        if (instance == null) {
            instance = new ModernHotBarMod();
        }
        return instance;
    }

    private final BooleanSetting showArmorBar = new BooleanSetting("setting.showarmorbar", "setting.showarmorbar.description", Icon.SETTINGS, this, true);
    private final BooleanSetting showExperienceBar = new BooleanSetting("setting.showexperiencebar", "setting.showexperiencebar.description", Icon.SETTINGS, this, true);
    private final BooleanSetting showNumericalValues = new BooleanSetting("setting.shownumericalvalues", "setting.shownumericalvalues.description", Icon.SETTINGS, this, true);

    private long lastTimeNanos = System.nanoTime();
    private int visualSelectedSlot = -1;
    private int animStartSlot = -1;
    private int animTargetSlot = -1;
    private float animProgress = 1f;
    private boolean animating = false;
    private static final float SELECTION_ANIM_DURATION = 0.18f;
    private float animatedSlotX = 0f;

    private float displayedExpFillW = 0f;
    private float displayedAbsorptionFillW = 0f;
    private float displayedHealthFillW = 0f;
    private float displayedArmorFillW = 0f;
    private float displayedHungerFillW = 0f;
    private static final float BAR_ANIM_DURATION = 0.12f;

    public ModernHotBarMod() {
        super("mod.modernhotbar.name", "mod.modernhotbar.desc", Icon.SETTINGS);
        instance = this;
    }

    @SuppressWarnings("unused")
    public final EventBus.EventListener<RenderSkiaEvent> onRenderSkia = event -> {
        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity player = mc.player;
        if (player == null || player.isSpectator()) return;

        long now = System.nanoTime();
        float delta = (now - lastTimeNanos) / 1_000_000_000.0f;
        lastTimeNanos = now;

        handleAnimation(player, delta);

        int scaledWidth = mc.getWindow().getScaledWidth();
        int scaledHeight = mc.getWindow().getScaledHeight();

        float hotbarX = scaledWidth / 2f - 91f;
        float hotbarY = scaledHeight - 22f;

        position.setSize(182f, 22f);
        position.setPosition(hotbarX, hotbarY);

        begin();

        drawBackground(hotbarX, hotbarY, 182f, 22f);
        float slotX = animatedSlotX;
        drawBackground(hotbarX + slotX, hotbarY, 22f, 22f);

        if (!player.getOffHandStack().isEmpty()) {
            float offhandSlotX = hotbarX - 22f - 4f;
            drawBackground(offhandSlotX, hotbarY, 22f, 22f);
        }

        float barWidth = 81f;
        float barHeight = 7.8f;
        float barSpacing = 2f;

        float expX = scaledWidth / 2f - 91f;
        float expHeight = 4f;
        float expWidth = 182f;
        float expY = hotbarY - expHeight - barSpacing;

        if (showExperienceBar.isEnabled() && !player.isCreative()) {
            float experience = player.experienceProgress;

            drawBackground(expX, expY, expWidth, expHeight);
            float expFillW = expWidth * experience;
            float expTarget = Math.max(0f, expFillW);
            float tExp = Math.min(1f, delta / Math.max(0.0001f, BAR_ANIM_DURATION));
            float expEased = 1f - (float)Math.pow(1f - tExp, 3);
            displayedExpFillW = displayedExpFillW + (expTarget - displayedExpFillW) * expEased;
            if (displayedExpFillW > 0f) {
                if (Math.abs(displayedExpFillW - expWidth) < 0.001f) {
                    Skia.drawRoundedRect(expX, expY, displayedExpFillW, expHeight, getRadius(), new Color(60, 179, 113));
                } else {
                    Skia.drawRoundedRectVarying(expX, expY, displayedExpFillW, expHeight, getRadius(), 0f, 0f, getRadius(), new Color(60, 179, 113));
                }
            }
        }

        float healthBarY = expY - barHeight - barSpacing;
        float armorBarY = healthBarY - barHeight - barSpacing;
        float absorptionBarY = armorBarY - barHeight - barSpacing;

        float healthX = scaledWidth / 2f - 91f;
        float hungerX = scaledWidth / 2f + 91f - barWidth;

        if (!player.isCreative()) {
            float absorption = player.getAbsorptionAmount();
            float maxHealth = Math.max(1f, player.getMaxHealth());
            float absorptionPercentage = MathHelper.clamp(absorption / maxHealth, 0.0f, 1.0f);
            float absorptionFillW = barWidth * absorptionPercentage;
            float absorptionTarget = Math.max(0f, absorptionFillW);
            float tAbs = Math.min(1f, delta / Math.max(0.0001f, BAR_ANIM_DURATION));
            float absEased = 1f - (float)Math.pow(1f - tAbs, 3);
            displayedAbsorptionFillW = displayedAbsorptionFillW + (absorptionTarget - displayedAbsorptionFillW) * absEased;
            if (displayedAbsorptionFillW > 0f) {
                drawBackground(healthX, absorptionBarY, barWidth, barHeight);
                if (Math.abs(displayedAbsorptionFillW - barWidth) < 0.001f) {
                    Skia.drawRoundedRect(healthX, absorptionBarY, displayedAbsorptionFillW, barHeight, getRadius(), new Color(255, 215, 0));
                } else {
                    Skia.drawRoundedRectVarying(healthX, absorptionBarY, displayedAbsorptionFillW, barHeight, getRadius(), 0f, 0f, getRadius(), new Color(255, 215, 0));
                }
                if (showNumericalValues.isEnabled()) {
                    String absorptionText = String.valueOf((int) absorption);
                    Skia.drawHeightCenteredText(absorptionText, healthX + 4f, absorptionBarY + barHeight / 2f, Color.WHITE, Fonts.getRegular(9f));
                }
            }

            float health = player.getHealth();
            float healthPercentage = MathHelper.clamp(health / maxHealth, 0.0f, 1.0f);
            float healthFillW = barWidth * healthPercentage;
            float healthTarget = Math.max(0f, healthFillW);
            float tHealth = Math.min(1f, delta / Math.max(0.0001f, BAR_ANIM_DURATION));
            float healthEased = 1f - (float)Math.pow(1f - tHealth, 3);
            displayedHealthFillW = displayedHealthFillW + (healthTarget - displayedHealthFillW) * healthEased;
            drawBackground(healthX, healthBarY, barWidth, barHeight);
            if (displayedHealthFillW > 0f) {
                if (Math.abs(displayedHealthFillW - barWidth) < 0.001f) {
                    Skia.drawRoundedRect(healthX, healthBarY, displayedHealthFillW, barHeight, getRadius(), new Color(205, 50, 50));
                } else {
                    Skia.drawRoundedRectVarying(healthX, healthBarY, displayedHealthFillW, barHeight, getRadius(), 0f, 0f, getRadius(), new Color(205, 50, 50));
                }
            }
            if (showNumericalValues.isEnabled()) {
                String healthText = String.valueOf((int) health);
                Skia.drawHeightCenteredText(healthText, healthX + 4f, healthBarY + barHeight / 2f, Color.WHITE, Fonts.getRegular(9f));
            }
        }

        if (showArmorBar.isEnabled() && player.getArmor() > 0 && !player.isCreative()) {
            float armor = player.getArmor();
            float armorPercentage = MathHelper.clamp(armor / 20.0f, 0.0f, 1.0f);
            float armorFillW = barWidth * armorPercentage;
            float armorTarget = Math.max(0f, armorFillW);
            float tArmor = Math.min(1f, delta / Math.max(0.0001f, BAR_ANIM_DURATION));
            float armorEased = 1f - (float)Math.pow(1f - tArmor, 3);
            displayedArmorFillW = displayedArmorFillW + (armorTarget - displayedArmorFillW) * armorEased;
            drawBackground(healthX, armorBarY, barWidth, barHeight);
            if (displayedArmorFillW > 0f) {
                if (Math.abs(displayedArmorFillW - barWidth) < 0.001f) {
                    Skia.drawRoundedRect(healthX, armorBarY, displayedArmorFillW, barHeight, getRadius(), new Color(150,150,150));
                } else {
                    Skia.drawRoundedRectVarying(healthX, armorBarY, displayedArmorFillW, barHeight, getRadius(), 0f, 0f, getRadius(), new Color(150,150,150));
                }
            }
            if (showNumericalValues.isEnabled()) {
                String armorText = String.valueOf(player.getArmor());
                Skia.drawHeightCenteredText(armorText, healthX + 4f, armorBarY + barHeight / 2f, Color.WHITE, Fonts.getRegular(9f));
            }
        }

        if (!player.isCreative()) {
            float hunger = player.getHungerManager().getFoodLevel();
            float hungerPercentage = MathHelper.clamp(hunger / 20.0f, 0.0f, 1.0f);
            float hungerFillW = barWidth * hungerPercentage;
            float hungerTarget = Math.max(0f, hungerFillW);
            float tHunger = Math.min(1f, delta / Math.max(0.0001f, BAR_ANIM_DURATION));
            float hungerEased = 1f - (float)Math.pow(1f - tHunger, 3);
            displayedHungerFillW = displayedHungerFillW + (hungerTarget - displayedHungerFillW) * hungerEased;
            drawBackground(hungerX, healthBarY, barWidth, barHeight);
            if (displayedHungerFillW > 0f) {
                float hungerFillX = hungerX + (barWidth - displayedHungerFillW);
                if (Math.abs(displayedHungerFillW - barWidth) < 0.001f) {
                    Skia.drawRoundedRect(hungerFillX, healthBarY, displayedHungerFillW, barHeight, getRadius(), new Color(218,165,32));
                } else {
                    Skia.drawRoundedRectVarying(hungerFillX, healthBarY, displayedHungerFillW, barHeight, 0f, getRadius(), getRadius(), 0f, new Color(218,165,32));
                }
            }
            if (showNumericalValues.isEnabled()) {
                String hungerText = String.valueOf((int) hunger);
                Skia.drawHeightCenteredText(hungerText, hungerX + 4f, healthBarY + barHeight / 2f, Color.WHITE, Fonts.getRegular(9f));
            }
        }

        finish();
    };

    private void handleAnimation(PlayerEntity player, float delta) {
        int currentSlot = player.getInventory().selectedSlot;
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
    }

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

        event.setCancelled(true);

        float drawX = getX();
        float drawY = getY();

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
