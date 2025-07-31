package com.soarclient.gui.modmenu.pages;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.lwjgl.glfw.GLFW;

import com.soarclient.Soar;
import com.soarclient.animation.SimpleAnimation;
import com.soarclient.gui.api.SoarGui;
import com.soarclient.gui.api.page.Page;
import com.soarclient.gui.api.page.impl.LeftRightTransition;
import com.soarclient.gui.api.page.impl.RightLeftTransition;
import com.soarclient.management.color.api.ColorPalette;
import com.soarclient.management.mod.Mod;
import com.soarclient.skia.Skia;
import com.soarclient.skia.font.Fonts;
import com.soarclient.skia.font.Icon;
import com.soarclient.ui.component.api.PressAnimation;
import com.soarclient.utils.ColorUtils;
import com.soarclient.utils.SearchUtils;
import com.soarclient.utils.language.I18n;
import com.soarclient.utils.mouse.MouseUtils;

public class ModsPage extends Page {

    private List<Item> items = new ArrayList<>();
    private Category selectedCategory = Category.ALL;
    private List<Item> filteredItems = new ArrayList<>();

    public enum Category {
        ALL("text.all"),
        HUD("text.hud"),
        RENDER("text.render"),
        PLAYER("text.player"),
        MISC("text.misc");

        private final String name;

        Category(String name) {
            this.name = name;
        }

        public String getName() {
            return I18n.get(name);
        }
    }

    public ModsPage(SoarGui parent) {
        super(parent, "text.mods", Icon.INVENTORY_2, new RightLeftTransition(true));
    }

    @Override
    public void init() {
        super.init();
        items.clear();
        for (Mod m : Soar.getInstance().getModManager().getMods()) {
            Item i = new Item(m);
            if (m.isEnabled()) {
                i.pressAnimation.setPressed();
            }
            items.add(i);
        }
        for (Item i : items) {
            i.xAnimation.setFirstTick(true);
            i.yAnimation.setFirstTick(true);
        }
        updateFilteredItems();
    }

    private void updateFilteredItems() {
        filteredItems = items.stream()
            .filter(i -> {
                Mod m = i.mod;
                if (m.isHidden()) return false;
                boolean searchMatch = searchBar.getText().isEmpty() || SearchUtils.isSimilar(I18n.get(m.getName()), searchBar.getText());
                boolean categoryMatch = selectedCategory == Category.ALL ||
                    (m.getCategory() != null && m.getCategory().name().equalsIgnoreCase(selectedCategory.name()));
                return searchMatch && categoryMatch;
            })
            .collect(Collectors.toList());
    }

    private float getEstimatedTextWidth(String text) {
        float width = 0;
        float wideCharWidth = 16.0f;
        float narrowCharWidth = 8.5f;
        for (char c : text.toCharArray()) {
            if (c >= '\u4E00' && c <= '\u9FFF' || c >= '\u3000' && c <= '\u303F' ||
                c >= '\u3040' && c <= '\u309F' || c >= '\u30A0' && c <= '\u30FF' ||
                c >= '\uFF00' && c <= '\uFFEF') {
                width += wideCharWidth;
            } else {
                width += narrowCharWidth;
            }
        }
        return width;
    }

    @Override
    public void draw(double mouseX, double mouseY) {
        // 1. [核心修正] 把super.draw()也放进滚动的变换里
        updateFilteredItems();
        ColorPalette palette = Soar.getInstance().getColorManager().getPalette();

        double relativeMouseY = mouseY - scrollHelper.getValue();
        Skia.save();
        Skia.translate(0, scrollHelper.getValue());

        // 2. 绘制标题和搜索框，它们现在会跟随滚动
        super.draw(mouseX, relativeMouseY);

        // 3. 将分类栏作为第一行内容绘制
        float categoryBarY = y + 56;
        float categoryBarHeight = 24;
        float categoryBarMarginBottom = 16;
        float categoryX = x + 26;

        for (Category category : Category.values()) {
            String categoryName = category.getName();
            float textWidth = getEstimatedTextWidth(categoryName);
            float padding = 20.0f;
            float buttonWidth = textWidth + padding;
            boolean isSelected = category == selectedCategory;
            boolean isHovered = MouseUtils.isInside(mouseX, relativeMouseY, categoryX, categoryBarY, buttonWidth, categoryBarHeight);
            Skia.drawRoundedRect(categoryX, categoryBarY, buttonWidth, categoryBarHeight, 12,
                isHovered ? palette.getSurfaceContainerLow() : palette.getSurface());
            Skia.drawFullCenteredText(categoryName, categoryX + buttonWidth / 2, categoryBarY + categoryBarHeight / 2,
                isSelected ? palette.getPrimary() : palette.getOnSurfaceVariant(), Fonts.getRegular(16));
            categoryX += buttonWidth + 10;
        }

        // 4. 模组列表从分类栏下方开始
        int index = 0;
        float offsetX = 26;
        float offsetY = categoryBarHeight + categoryBarMarginBottom;

        for (Item i : filteredItems) {
            Mod m = i.mod;
            float itemX = x + offsetX;
            float itemY = y + 96 + offsetY;

            i.focusAnimation.onTick(MouseUtils.isInside(mouseX, relativeMouseY, itemX, itemY, 244, 116 + 35) ? (i.pressed ? 0.12F : 0.08F) : 0, 8);
            i.xAnimation.onTick(itemX, 14);
            i.yAnimation.onTick(itemY, 14);
            itemX = i.xAnimation.getValue();
            itemY = i.yAnimation.getValue();

            Skia.drawRoundedRectVarying(itemX, itemY, 244, 116, 26, 26, 0, 0, palette.getSurface());
            Skia.drawRoundedRectVarying(itemX, itemY + 116, 244, 35, 0, 0, 26, 26, palette.getSurfaceContainerLow());
            Skia.drawRoundedRectVarying(itemX, itemY + 116, 244, 35, 0, 0, 26, 26, ColorUtils.applyAlpha(palette.getSurfaceContainerLowest(), i.focusAnimation.getValue()));

            Skia.save();
            Skia.clip(itemX, itemY + 116, 244, 35, 0, 0, 26, 26);
            i.pressAnimation.draw(itemX, itemY + 116, 224, 35, palette.getPrimaryContainer(), 1);
            Skia.restore();

            Skia.drawFullCenteredText(I18n.get(m.getName()), itemX + (244 / 2), itemY + 116 + (35 / 2), palette.getOnSurfaceVariant(), Fonts.getRegular(16));
            Skia.drawFullCenteredText(m.getIcon(), itemX + (244 / 2), itemY + (116 / 2), palette.getOnSurfaceVariant(), Fonts.getIcon(68));

            index++;
            offsetX += 32 + 244;
            if (index % 3 == 0) {
                offsetX = 26;
                offsetY += 22 + 151;
            }
        }

        // 5. 调整总滚动高度
        scrollHelper.setMaxScroll(151, 22, filteredItems.size() + 3, 3, height);

        Skia.restore();
    }

    @Override
    public void mousePressed(double mouseX, double mouseY, int button) {
        double relativeMouseY = mouseY - scrollHelper.getValue();

        super.mousePressed(mouseX, relativeMouseY, button);

        float categoryBarY = y + 56;
        float categoryBarHeight = 24;
        float categoryX = x + 26;

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            for (Category category : Category.values()) {
                String categoryName = category.getName();
                float textWidth = getEstimatedTextWidth(categoryName);
                float padding = 20.0f;
                float buttonWidth = textWidth + padding;
                if (MouseUtils.isInside(mouseX, relativeMouseY, categoryX, categoryBarY, buttonWidth, categoryBarHeight)) {
                    if (this.selectedCategory != category) {
                        this.selectedCategory = category;
                    }
                    return;
                }
                categoryX += buttonWidth + 10;
            }
        }

        for (Item i : filteredItems) {
            float itemX = i.xAnimation.getValue();
            float itemY = i.yAnimation.getValue();
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                if (MouseUtils.isInside(mouseX, relativeMouseY, itemX, itemY + 116, 244, 35)) {
                    i.pressed = true;
                }
            }
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        double relativeMouseY = mouseY - scrollHelper.getValue();
        for (Item i : filteredItems) {
            Mod m = i.mod;
            float itemX = i.xAnimation.getValue();
            float itemY = i.yAnimation.getValue();
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                if (MouseUtils.isInside(mouseX, relativeMouseY, itemX, itemY + 116, 244, 35)) {
                    m.toggle();
                    if (m.isEnabled()) {
                        i.pressAnimation.onPressed(mouseX, relativeMouseY, itemX, itemY + 116);
                    } else {
                        i.pressAnimation.onReleased(mouseX, relativeMouseY, itemX, itemY + 116);
                    }
                }
                if (MouseUtils.isInside(mouseX, relativeMouseY, itemX, itemY, 244, 116)
                    && !Soar.getInstance().getModManager().getSettingsByMod(m).isEmpty()) {
                    parent.setCurrentPage(new SettingsImplPage(parent, this.getClass(), m));
                    this.setTransition(new LeftRightTransition(true));
                }
            }
            i.pressed = false;
        }
    }

    @Override
    public void onClosed() {
        this.setTransition(new RightLeftTransition(true));
    }

    private class Item {
        private Mod mod;
        private SimpleAnimation focusAnimation = new SimpleAnimation();
        private SimpleAnimation xAnimation = new SimpleAnimation();
        private SimpleAnimation yAnimation = new SimpleAnimation();
        private PressAnimation pressAnimation = new PressAnimation();
        private boolean pressed;

        private Item(Mod mod) {
            this.mod = mod;
            this.pressed = false;
        }
    }
}
