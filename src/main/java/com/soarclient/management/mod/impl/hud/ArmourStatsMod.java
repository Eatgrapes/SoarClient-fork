package com.soarclient.management.mod.impl.hud;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.soarclient.event.EventBus;
import com.soarclient.event.client.ClientTickEvent;
import com.soarclient.event.client.RenderSkiaEvent;
import com.soarclient.gui.edithud.api.HUDCore;
import com.soarclient.management.mod.api.hud.HUDMod;
import com.soarclient.skia.Skia;
import com.soarclient.skia.font.Fonts;
import com.soarclient.skia.font.Icon;

import io.github.humbleui.types.Rect;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;

public class ArmourStatsMod extends HUDMod {

    private final List<ItemStack> armourItems = new ArrayList<>();
    private ItemStack mainHandItem;
    private int maxString, prevItemCount;

    // 动画变量
    private float animatedWidth, animatedHeight;

    public ArmourStatsMod() {
        super("mod.armourstats.name", "mod.armourstats.description", Icon.SHIELD);
        this.animatedWidth = 0;
        this.animatedHeight = 0;
    }

    public final EventBus.EventListener<ClientTickEvent> onClientTick = event -> {
        // 数据获取逻辑不变
        mainHandItem = null;
        armourItems.clear();
        boolean shouldShowDummy = HUDCore.isEditing && (client.player == null || (client.player.getMainHandStack().isEmpty() && client.player.getInventory().armor.stream().allMatch(ItemStack::isEmpty)));
        if (shouldShowDummy) {
            mainHandItem = new ItemStack(Items.DIAMOND_SWORD);
            mainHandItem.setDamage(123);
            armourItems.add(new ItemStack(Items.DIAMOND_HELMET));
            armourItems.add(new ItemStack(Items.DIAMOND_CHESTPLATE));
        } else if (client.player != null) {
            ItemStack handStack = client.player.getMainHandStack();
            if (!handStack.isEmpty()) { mainHandItem = handStack; }
            for (ItemStack itemStack : client.player.getInventory().armor) {
                if (!itemStack.isEmpty()) { armourItems.add(itemStack); }
            }
        }
    };

    public final EventBus.EventListener<RenderSkiaEvent> onRenderSkia = event -> {
        this.draw();
    };

    private void draw() {
        int totalItemCount = armourItems.size() + (mainHandItem != null ? 1 : 0);

        calculateDimensions();

        float targetWidth = (totalItemCount > 0) ? maxString + 29 : 0;
        float targetHeight = (totalItemCount > 0) ? (20 * totalItemCount) + 6 : 0;

        // 如果目标是隐藏，并且动画已经变得很小，则直接返回
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

        // 最终修正：完全移除剪裁 (Skia.clip)，因为找不到可用的方法。
        // 我们只在动画基本完成时才绘制内容，以防止溢出。
        float animationProgress = targetWidth > 0 ? animatedWidth / targetWidth : 0;

        if (animationProgress > 0.95f) {
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

    // drawItem 方法保持最简单的形式
    private void drawItem(ItemStack itemStack, int offsetY) {
        drawArmourIcon(itemStack, offsetY);

        String name = itemStack.getName().getString();
        String detailText;

        if (itemStack.getCount() > 1) {
            detailText = "x" + itemStack.getCount();
        } else if (itemStack.isDamageable()) {
            detailText = (itemStack.getMaxDamage() - itemStack.getDamage()) + " / " + itemStack.getMaxDamage();
        } else {
            detailText = "";
        }

        this.drawText(name, getX() + 25, getY() + offsetY - 12, Fonts.getRegular(9));
        if (!detailText.isEmpty()) {
            this.drawText(detailText, getX() + 25, getY() + offsetY - 1, Fonts.getRegular(8));
        }
    }

    // drawArmourIcon 方法保持最简单的形式
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

    // calculateDimensions 方法保持不变
    private void calculateDimensions() {
        maxString = 0;
        int totalItemCount = armourItems.size() + (mainHandItem != null ? 1 : 0);
        List<ItemStack> allItems = new ArrayList<>();
        if (mainHandItem != null) allItems.add(mainHandItem);
        allItems.addAll(armourItems);

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

            if (maxString < currentItemWidth || prevItemCount != totalItemCount) {
                maxString = (int) currentItemWidth;
            }
        }
        prevItemCount = totalItemCount;
    }

    private void drawIconPlaceholder(float x, float y, float size) {
        Skia.drawRoundedRect(x, y, size, size, 2, this.getDesign().getTextColor().darker());
    }

    @Override
    public float getRadius() {
        return 6;
    }
}
