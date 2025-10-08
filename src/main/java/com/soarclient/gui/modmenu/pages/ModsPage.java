package com.soarclient.gui.modmenu.pages;

import com.soarclient.Soar;
import com.soarclient.animation.SimpleAnimation;
import com.soarclient.gui.api.SoarGui;
import com.soarclient.gui.api.page.Page;
import com.soarclient.gui.api.page.impl.LeftRightTransition;
import com.soarclient.gui.api.page.impl.RightLeftTransition;
import com.soarclient.management.color.api.ColorPalette;
import com.soarclient.management.mod.Mod;
import com.soarclient.management.mod.impl.settings.ModMenuSettings;
import com.soarclient.skia.Skia;
import com.soarclient.skia.font.Fonts;
import com.soarclient.skia.font.Icon;
import com.soarclient.ui.component.api.PressAnimation;
import com.soarclient.utils.ColorUtils;
import com.soarclient.utils.SearchUtils;
import com.soarclient.utils.language.I18n;
import com.soarclient.utils.mouse.MouseUtils;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ModsPage extends Page {

    private final List<Item> items = new ArrayList<>();
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
            if (c >= '一' && c <= '鿿' || c >= '　' && c <= '〿' ||
                c >= '぀' && c <= 'ゟ' || c >= '゠' && c <= 'ヿ' ||
                c >= '＀' && c <= '￯') {
                width += wideCharWidth;
            } else {
                width += narrowCharWidth;
            }
        }
        return width;
    }

    @Override
    public void draw(double mouseX, double mouseY) {
        updateFilteredItems();
        boolean isWinStyle = ModMenuSettings.getInstance().getUiStyleSetting().getOption().equals("win");
        ColorPalette palette = Soar.getInstance().getColorManager().getPalette();

        double relativeMouseY = mouseY - scrollHelper.getValue();
        Skia.save();
        Skia.translate(0, scrollHelper.getValue());

        super.draw(mouseX, relativeMouseY);

        if (isWinStyle) {
            drawWinStyle(mouseX, relativeMouseY, palette);
        } else {
            drawMd3Style(mouseX, relativeMouseY, palette);
        }

        Skia.restore();
    }

    private void drawWinStyle(double mouseX, double mouseY, ColorPalette palette) {
        float leftMargin = 32;
        float topMargin = 56; // Below search bar

        // Category Bar
        float categoryBarY = y + topMargin;
        float categoryBarHeight = 28;
        float categoryX = x + leftMargin;

        for (Category category : Category.values()) {
            String categoryName = category.getName();
            float textWidth = getEstimatedTextWidth(categoryName);
            float padding = 16.0f;
            float buttonWidth = textWidth + padding;
            boolean isSelected = category == selectedCategory;
            boolean isHovered = MouseUtils.isInside(mouseX, mouseY, categoryX, categoryBarY, buttonWidth, categoryBarHeight);

            Color backgroundColor = isSelected ? palette.getPrimary() : (isHovered ? palette.getSurfaceContainerLow() : new Color(0,0,0,0));
            Color textColor = isSelected ? palette.getOnPrimary() : palette.getOnSurfaceVariant();

            Skia.drawRoundedRect(categoryX, categoryBarY, buttonWidth, categoryBarHeight, 6, backgroundColor);
            Skia.drawFullCenteredText(categoryName, categoryX + buttonWidth / 2f, categoryBarY + categoryBarHeight / 2f, textColor, Fonts.getRegular(14));
            categoryX += buttonWidth + 8;
        }

        // Mods Grid
        float gridTopY = categoryBarY + categoryBarHeight + 16;
        int columnCount = 4;
        float itemWidth = (width - (leftMargin * 2) - (16 * (columnCount - 1))) / columnCount;
        float itemHeight = 100;
        float footerHeight = 30;
        float totalItemHeight = itemHeight + footerHeight;

        float currentX = x + leftMargin;
        float currentY = gridTopY;

        for (int i = 0; i < filteredItems.size(); i++) {
            Item item = filteredItems.get(i);
            Mod m = item.mod;

            item.xAnimation.onTick(currentX, 14);
            item.yAnimation.onTick(currentY, 14);
            float itemX = item.xAnimation.getValue();
            float itemY = item.yAnimation.getValue();

            boolean isItemHovered = MouseUtils.isInside(mouseX, mouseY, itemX, itemY, itemWidth, totalItemHeight);

            Skia.drawRoundedRect(itemX, itemY, itemWidth, totalItemHeight, 8, palette.getSurfaceContainer());
            if (isItemHovered) {
                Skia.drawRoundedRect(itemX, itemY, itemWidth, totalItemHeight, 8, ColorUtils.applyAlpha(palette.getOnSurface(), 0.05f));
            }

            Skia.drawFullCenteredText(m.getIcon(), itemX + (itemWidth / 2f), itemY + (itemHeight / 2f), palette.getOnSurfaceVariant(), Fonts.getIcon(50));

            Color footerColor = m.isEnabled() ? palette.getPrimary() : palette.getSurfaceContainerHighest();
            Color footerTextColor = m.isEnabled() ? palette.getOnPrimary() : palette.getOnSurfaceVariant();
            Skia.drawRoundedRectVarying(itemX, itemY + itemHeight, itemWidth, footerHeight, 0, 0, 8, 8, footerColor);
            Skia.drawFullCenteredText(I18n.get(m.getName()), itemX + (itemWidth / 2f), itemY + itemHeight + (footerHeight / 2f), footerTextColor, Fonts.getRegular(14));

            currentX += itemWidth + 16;
            if ((i + 1) % columnCount == 0) {
                currentX = x + leftMargin;
                currentY += totalItemHeight + 16;
            }
        }

        float contentHeight = (currentY + totalItemHeight) - gridTopY;
        scrollHelper.setMaxScroll(contentHeight, height - topMargin);
    }

    private void drawMd3Style(double mouseX, double mouseY, ColorPalette palette) {
        float leftMargin = 26;
        float topMargin = 56;

        // Category Bar
        float categoryBarY = y + topMargin;
        float categoryBarHeight = 24;
        float categoryX = x + leftMargin;

        for (Category category : Category.values()) {
            String categoryName = category.getName();
            float textWidth = getEstimatedTextWidth(categoryName);
            float padding = 20.0f;
            float buttonWidth = textWidth + padding;
            boolean isSelected = category == selectedCategory;
            boolean isHovered = MouseUtils.isInside(mouseX, mouseY, categoryX, categoryBarY, buttonWidth, categoryBarHeight);
            Skia.drawRoundedRect(categoryX, categoryBarY, buttonWidth, categoryBarHeight, 12,
                isHovered ? palette.getSurfaceContainerLow() : palette.getSurface());
            Skia.drawFullCenteredText(categoryName, categoryX + buttonWidth / 2f, categoryBarY + categoryBarHeight / 2f,
                isSelected ? palette.getPrimary() : palette.getOnSurfaceVariant(), Fonts.getRegular(16));
            categoryX += buttonWidth + 10;
        }

        // Mods Grid
        float gridTopY = categoryBarY + categoryBarHeight + 16;
        int columnCount = 3;
        float itemWidth = 244;
        float itemHeight = 116;
        float footerHeight = 35;
        float totalItemHeight = itemHeight + footerHeight;

        float currentX = x + leftMargin;
        float currentY = gridTopY;

        for (int i = 0; i < filteredItems.size(); i++) {
            Item item = filteredItems.get(i);
            Mod m = item.mod;

            item.focusAnimation.onTick(MouseUtils.isInside(mouseX, mouseY, currentX, currentY, itemWidth, totalItemHeight) ? (item.pressed ? 0.12F : 0.08F) : 0, 8);
            item.xAnimation.onTick(currentX, 14);
            item.yAnimation.onTick(currentY, 14);
            float itemX = item.xAnimation.getValue();
            float itemY = item.yAnimation.getValue();

            Skia.drawRoundedRectVarying(itemX, itemY, itemWidth, itemHeight, 26, 26, 0, 0, palette.getSurface());
            Skia.drawRoundedRectVarying(itemX, itemY + itemHeight, itemWidth, footerHeight, 0, 0, 26, 26, palette.getSurfaceContainerLow());
            Skia.drawRoundedRectVarying(itemX, itemY + itemHeight, itemWidth, footerHeight, 0, 0, 26, 26, ColorUtils.applyAlpha(palette.getSurfaceContainerLowest(), item.focusAnimation.getValue()));

            Skia.save();
            Skia.clip(itemX, itemY + itemHeight, itemWidth, footerHeight, 0, 0, 26, 26);
            item.pressAnimation.draw(itemX, itemY + itemHeight, itemWidth, footerHeight, palette.getPrimaryContainer(), 1);
            Skia.restore();

            Skia.drawFullCenteredText(I18n.get(m.getName()), itemX + (itemWidth / 2f), itemY + itemHeight + (footerHeight / 2f), palette.getOnSurfaceVariant(), Fonts.getRegular(16));
            Skia.drawFullCenteredText(m.getIcon(), itemX + (itemWidth / 2f), itemY + (itemHeight / 2f), palette.getOnSurfaceVariant(), Fonts.getIcon(68));

            currentX += itemWidth + 32;
            if ((i + 1) % columnCount == 0) {
                currentX = x + leftMargin;
                currentY += totalItemHeight + 22;
            }
        }
        float contentHeight = (currentY + totalItemHeight) - gridTopY;
        scrollHelper.setMaxScroll(contentHeight, height - topMargin);
    }

    @Override
    public void mousePressed(double mouseX, double mouseY, int button) {
        double relativeMouseY = mouseY - scrollHelper.getValue();
        super.mousePressed(mouseX, relativeMouseY, button);
        boolean isWinStyle = ModMenuSettings.getInstance().getUiStyleSetting().getOption().equals("win");

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (isWinStyle) {
                handleWinMousePress(mouseX, relativeMouseY);
            } else {
                handleMd3MousePress(mouseX, relativeMouseY);
            }
        }
    }

    private void handleWinMousePress(double mouseX, double mouseY) {
        // Category Bar
        float categoryBarY = y + 56;
        float categoryBarHeight = 28;
        float categoryX = x + 32;

        for (Category category : Category.values()) {
            String categoryName = category.getName();
            float textWidth = getEstimatedTextWidth(categoryName);
            float padding = 16.0f;
            float buttonWidth = textWidth + padding;
            if (MouseUtils.isInside(mouseX, mouseY, categoryX, categoryBarY, buttonWidth, categoryBarHeight)) {
                if (this.selectedCategory != category) {
                    this.selectedCategory = category;
                }
                return;
            }
            categoryX += buttonWidth + 8;
        }

        // Mods Grid
        for (Item i : filteredItems) {
            float itemX = i.xAnimation.getValue();
            float itemY = i.yAnimation.getValue();
            float itemWidth = (width - (32 * 2) - (16 * (4 - 1))) / 4;
            if (MouseUtils.isInside(mouseX, mouseY, itemX, itemY + 100, itemWidth, 30)) {
                i.pressed = true;
            }
        }
    }

    private void handleMd3MousePress(double mouseX, double mouseY) {
        // Category Bar
        float categoryBarY = y + 56;
        float categoryBarHeight = 24;
        float categoryX = x + 26;

        for (Category category : Category.values()) {
            String categoryName = category.getName();
            float textWidth = getEstimatedTextWidth(categoryName);
            float padding = 20.0f;
            float buttonWidth = textWidth + padding;
            if (MouseUtils.isInside(mouseX, mouseY, categoryX, categoryBarY, buttonWidth, categoryBarHeight)) {
                if (this.selectedCategory != category) {
                    this.selectedCategory = category;
                }
                return;
            }
            categoryX += buttonWidth + 10;
        }

        // Mods Grid
        for (Item i : filteredItems) {
            float itemX = i.xAnimation.getValue();
            float itemY = i.yAnimation.getValue();
            if (MouseUtils.isInside(mouseX, mouseY, itemX, itemY + 116, 244, 35)) {
                i.pressed = true;
            }
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        double relativeMouseY = mouseY - scrollHelper.getValue();
        boolean isWinStyle = ModMenuSettings.getInstance().getUiStyleSetting().getOption().equals("win");

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (isWinStyle) {
                handleWinMouseRelease(mouseX, relativeMouseY);
            } else {
                handleMd3MouseRelease(mouseX, relativeMouseY);
            }
        }

        for (Item i : filteredItems) {
            i.pressed = false;
        }
    }

    private void handleWinMouseRelease(double mouseX, double mouseY) {
        for (Item i : filteredItems) {
            Mod m = i.mod;
            float itemX = i.xAnimation.getValue();
            float itemY = i.yAnimation.getValue();
            float itemWidth = (width - (32 * 2) - (16 * (4 - 1))) / 4;

            if (MouseUtils.isInside(mouseX, mouseY, itemX, itemY + 100, itemWidth, 30)) {
                m.toggle();
            } else if (MouseUtils.isInside(mouseX, mouseY, itemX, itemY, itemWidth, 100)) {
                if (!Soar.getInstance().getModManager().getSettingsByMod(m).isEmpty()) {
                    parent.setCurrentPage(new SettingsImplPage(parent, this.getClass(), m));
                    this.setTransition(new LeftRightTransition(true));
                }
            }
        }
    }

    private void handleMd3MouseRelease(double mouseX, double mouseY) {
        for (Item i : filteredItems) {
            Mod m = i.mod;
            float itemX = i.xAnimation.getValue();
            float itemY = i.yAnimation.getValue();
            if (MouseUtils.isInside(mouseX, mouseY, itemX, itemY + 116, 244, 35)) {
                m.toggle();
                if (m.isEnabled()) {
                    i.pressAnimation.onPressed(mouseX, mouseY, itemX, itemY + 116);
                } else {
                    i.pressAnimation.onReleased(mouseX, mouseY, itemX, itemY + 116);
                }
            }
            if (MouseUtils.isInside(mouseX, mouseY, itemX, itemY, 244, 116)
                && !Soar.getInstance().getModManager().getSettingsByMod(m).isEmpty()) {
                parent.setCurrentPage(new SettingsImplPage(parent, this.getClass(), m));
                this.setTransition(new LeftRightTransition(true));
            }
        }
    }

    @Override
    public void onClosed() {
        this.setTransition(new RightLeftTransition(true));
    }

    private static class Item {
        private final Mod mod;
        private final SimpleAnimation focusAnimation = new SimpleAnimation();
        private final SimpleAnimation xAnimation = new SimpleAnimation();
        private final SimpleAnimation yAnimation = new SimpleAnimation();
        private final PressAnimation pressAnimation = new PressAnimation();
        private boolean pressed;

        private Item(Mod mod) {
            this.mod = mod;
            this.pressed = false;
        }
    }
}
