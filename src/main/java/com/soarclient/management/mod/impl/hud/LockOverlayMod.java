package com.soarclient.management.mod.impl.hud;

import com.soarclient.event.EventBus;
import com.soarclient.event.client.RenderSkiaEvent;
import com.soarclient.management.mod.api.hud.SimpleHUDMod;
import com.soarclient.management.mod.settings.impl.BooleanSetting;
import com.soarclient.skia.Skia;
import com.soarclient.skia.font.Fonts;
import io.github.humbleui.types.Rect;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.awt.Color;
import java.lang.reflect.Method;

public class LockOverlayMod extends SimpleHUDMod {

    private final MinecraftClient mc = MinecraftClient.getInstance();

    private final BooleanSetting showCoordinates = new BooleanSetting(
        "setting.showCoordinates",
        "setting.showCoordinates.description",
        null, this, true
    );

    private final BooleanSetting showBackground = new BooleanSetting(
        "setting.background",
        "setting.background.description",
        null, this, true
    );

    private final BooleanSetting showNamespace = new BooleanSetting(
        "setting.namespace",
        "setting.namespace.description",
        null, this, false
    );

    public LockOverlayMod() {
        super("mod.LockOverlayMod.name", "mod.LockOverlayMod.description", null);
        setEnabled(true);
    }

    public EventBus.EventListener<RenderSkiaEvent> onRenderSkia = event -> {
        if (!isEnabled() || mc.world == null || mc.player == null) return;
        drawOverlay();
    };

    private void drawOverlay() {
        HitResult hit = mc.crosshairTarget;
        if (!(hit instanceof BlockHitResult bhr)) return;

        BlockPos pos = bhr.getBlockPos();
        BlockState state = mc.world.getBlockState(pos);
        if (state == null || state.isAir()) return;

        Item asItem = state.getBlock().asItem();
        ItemStack stack = (asItem != null && asItem != Items.AIR) ? new ItemStack(asItem) : ItemStack.EMPTY;

        String id = Registries.BLOCK.getId(state.getBlock()).toString();
        String pretty = showNamespace.isEnabled() ? id : trimNamespace(id);

        String text = pretty;
        if (showCoordinates.isEnabled()) {
            text += " §7(" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")";
        }

        float fontSize = 10.5f;
        float padding = 4.5f;
        float iconSize = 16.0f;
        float gap = 5.0f;

        Rect textBounds = Skia.getTextBounds(text, Fonts.getRegular(fontSize));
        float height = Math.max(fontSize, iconSize) + padding * 2f;
        float width = padding + (stack.isEmpty() ? 0 : iconSize + gap) + textBounds.getWidth() + padding;

        begin();
        if (showBackground.isEnabled()) {
            drawBackground(getX(), getY(), width, height);
        }
        finish();

        float centerY = getY() + height / 2f;
        float drawX = getX() + padding;

        if (!stack.isEmpty()) {
            drawItemStackVanilla(stack, (int) drawX, (int) (centerY - iconSize / 2f));
            drawX += iconSize + gap;
        }

        begin();
        Skia.drawFullCenteredText(
            text,
            drawX + textBounds.getWidth() / 2f,
            centerY,
            getDesign().getTextColor(),
            Fonts.getRegular(fontSize)
        );
        finish();

        position.setSize(width, height);
    }

    /**
     * 多方法兼容 ItemRenderer HUD 绘制
     */
    private void drawItemStackVanilla(ItemStack stack, int x, int y) {
        try {
            Class<?> cls = mc.getItemRenderer().getClass();

            // 按常见映射顺序依次尝试
            Method m = null;
            for (String name : new String[]{"renderGuiItemIcon", "renderInGuiWithOverrides", "renderGuiItem"}) {
                for (Method candidate : cls.getDeclaredMethods()) {
                    if (candidate.getName().equals(name)) {
                        m = candidate;
                        break;
                    }
                }
                if (m != null) break;
            }

            if (m != null) {
                m.setAccessible(true);
                Class<?>[] params = m.getParameterTypes();
                if (params.length >= 3 &&
                    ItemStack.class.isAssignableFrom(params[0]) &&
                    params[1] == int.class && params[2] == int.class) {
                    m.invoke(mc.getItemRenderer(), stack, x, y);
                    return;
                }
            }

            // 如果全都不存在，则画个占位
            Skia.drawRoundedRect(x, y, 16, 16, 3, new Color(255, 255, 255, 40));

        } catch (Throwable t) {
            // 兜底占位
            Skia.drawRoundedRect(x, y, 16, 16, 3, new Color(255, 255, 255, 40));
        }
    }

    private String trimNamespace(String id) {
        int idx = id.indexOf(':');
        return idx >= 0 ? id.substring(idx + 1) : id;
    }

    @Override
    public String getText() {
        return "Lock Overlay";
    }

    @Override
    public String getIcon() {
        return null;
    }

    @Override public void onEnable() { super.onEnable(); EventBus.getInstance().register(onRenderSkia); }
    @Override public void onDisable() { super.onDisable(); EventBus.getInstance().unregister(onRenderSkia); }
}
