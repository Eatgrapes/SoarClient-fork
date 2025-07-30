package com.soarclient.management.mod.impl.hud;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.soarclient.event.EventBus;
import com.soarclient.event.client.ClientTickEvent;
import com.soarclient.event.client.RenderSkiaEvent;
import com.soarclient.gui.edithud.api.HUDCore;
import com.soarclient.management.mod.api.hud.HUDMod;
import com.soarclient.management.mod.settings.impl.BooleanSetting;
import com.soarclient.skia.Skia;
import com.soarclient.skia.font.Fonts;
import com.soarclient.skia.font.Icon;

import io.github.humbleui.types.Rect;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PotionItem;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;

public class ArmourStatsMod extends HUDMod {

    private final List<ItemStack> armourItems = new ArrayList<>();
    private ItemStack mainHandItem;
    private int maxString;

    private float animatedWidth, animatedHeight;
    private float targetWidth, targetHeight;

    private final BooleanSetting backgroundSetting = new BooleanSetting("setting.background",
        "setting.background1.description", Icon.IMAGE, this, true);

    private final BooleanSetting showIconSetting = new BooleanSetting("mod.watermark.show_icon1",
        "mod.watermark.show_icon1.description", Icon.IMAGE, this, true);

    private final BooleanSetting showTextSetting = new BooleanSetting("mod.watermark.show_text",
        "mod.watermark.show_text1.description", Icon.TEXT_FIELDS, this, true);

    public ArmourStatsMod() {
        super("mod.armourstats.name", "mod.armourstats.description", Icon.SHIELD);
        this.animatedWidth = 0;
        this.animatedHeight = 0;
        this.targetWidth = 0;
        this.targetHeight = 0;
    }

    public final EventBus.EventListener<ClientTickEvent> onClientTick = event -> {
        mainHandItem = null;
        armourItems.clear();

        boolean shouldShowDummy = HUDCore.isEditing && (client.player == null || !hasValidItems());

        if (shouldShowDummy) {
            mainHandItem = new ItemStack(Items.DIAMOND_SWORD);
            mainHandItem.setDamage(123);
            armourItems.add(new ItemStack(Items.DIAMOND_HELMET));
            armourItems.add(new ItemStack(Items.DIAMOND_CHESTPLATE));
        } else if (client.player != null) {
            ItemStack handStack = client.player.getMainHandStack();
            if (!handStack.isEmpty()) {
                Item item = handStack.getItem();
                if (!(item instanceof BlockItem) && !(item instanceof PotionItem)) {
                    mainHandItem = handStack;
                }
            }

            for (ItemStack itemStack : client.player.getInventory().armor) {
                if (!itemStack.isEmpty()) {
                    armourItems.add(itemStack);
                }
            }
        }

        calculateDimensions();

        int totalItemCount = armourItems.size() + (mainHandItem != null ? 1 : 0);

        if (!showIconSetting.isEnabled() && !showTextSetting.isEnabled()) {
            this.targetWidth = 0;
            this.targetHeight = 0;
            return;
        }

        int iconWidth = showIconSetting.isEnabled() ? 25 : 0;
        int textWidth = showTextSetting.isEnabled() ? maxString : 0;
        this.targetWidth = (totalItemCount > 0) ? iconWidth + textWidth + 4 : 0;
        this.targetHeight = (totalItemCount > 0) ? (20 * totalItemCount) + 6 : 0;
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
            int ySize = 20;
            int offsetY = 16;

            if (mainHandItem != null) {
                drawItem(mainHandItem, offsetY);
                offsetY += ySize;
            }
            for (int i = armourItems.size() - 1; i >= 0; i--) {
                ItemStack itemStack = armourItems.get(i);
                drawItem(itemStack, offsetY);
                offsetY += ySize;
            }
        }

        this.finish();
        position.setSize(animatedWidth, animatedHeight);
    }

    private boolean hasValidItems() {
        if (client.player == null) return false;

        ItemStack handStack = client.player.getMainHandStack();
        if (!handStack.isEmpty()) {
            Item item = handStack.getItem();
            if (!(item instanceof BlockItem) && !(item instanceof PotionItem)) {
                return true;
            }
        }

        for (ItemStack itemStack : client.player.getInventory().armor) {
            if (!itemStack.isEmpty()) {
                return true;
            }
        }

        return false;
    }

    private void drawItem(ItemStack itemStack, int offsetY) {
        float offsetX = 0;

        if (showIconSetting.isEnabled()) {
            drawArmourIcon(itemStack, offsetY);
            offsetX += 25;
        } else {
            offsetX += 4;
        }

        if (showTextSetting.isEnabled()) {
            String name = itemStack.getName().getString();
            String detailText;

            if (itemStack.getCount() > 1) {
                detailText = "x" + itemStack.getCount();
            } else if (itemStack.isDamageable()) {
                detailText = (itemStack.getMaxDamage() - itemStack.getDamage()) + " / " + itemStack.getMaxDamage();
            } else {
                detailText = "";
            }

            this.drawText(name, getX() + offsetX, getY() + offsetY - 12, Fonts.getRegular(9));
            if (!detailText.isEmpty()) {
                this.drawText(detailText, getX() + offsetX, getY() + offsetY - 1, Fonts.getRegular(8));
            }
        }
    }

    private void drawArmourIcon(ItemStack itemStack, int offsetY) {
        Identifier itemId = Registries.ITEM.getId(itemStack.getItem());
        String texturePath = String.format("textures/item/%s.png", itemId.getPath());

        float iconSize = 16;
        float iconX = getX() + 4;
        float iconY = getY() + offsetY - 12;

        try {
            Skia.drawMinecraftImage(texturePath, iconX, iconY, iconSize, iconSize);
        } catch (Exception e) {
            drawIconPlaceholder(iconX, iconY, iconSize);
        }
    }

    private void calculateDimensions() {
        maxString = 0;
        List<ItemStack> allItems = new ArrayList<>();
        if (mainHandItem != null) allItems.add(mainHandItem);
        allItems.addAll(armourItems);

        if (allItems.isEmpty() || !showTextSetting.isEnabled()) {
            maxString = 0;
            return;
        }

        for (ItemStack itemStack : allItems) {
            String name = itemStack.getName().getString();
            String detailText = "";
            if (itemStack.getCount() > 1) detailText = "x" + itemStack.getCount();
            else if (itemStack.isDamageable()) detailText = (itemStack.getMaxDamage() - itemStack.getDamage()) + " / " + itemStack.getMaxDamage();

            Rect nameBounds = Skia.getTextBounds(name, Fonts.getRegular(9));
            float currentItemWidth = nameBounds.getWidth();

            if(!detailText.isEmpty()){
                Rect detailBounds = Skia.getTextBounds(detailText, Fonts.getRegular(8));
                currentItemWidth = Math.max(nameBounds.getWidth(), detailBounds.getWidth());
            }

            if (maxString < currentItemWidth) {
                maxString = (int) currentItemWidth;
            }
        }
    }

    private void drawIconPlaceholder(float x, float y, float size) {
        Skia.drawRoundedRect(x, y, size, size, 2, this.getDesign().getTextColor().darker());
    }

    @Override
    public float getRadius() {
        return 6;
    }
}
