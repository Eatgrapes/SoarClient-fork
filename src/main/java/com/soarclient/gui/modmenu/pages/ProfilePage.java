package com.soarclient.gui.modmenu.pages;

import com.soarclient.Soar;
import com.soarclient.animation.SimpleAnimation;
import com.soarclient.gui.api.SoarGui;
import com.soarclient.gui.api.page.Page;
import com.soarclient.gui.api.page.impl.LeftRightTransition;
import com.soarclient.gui.api.page.impl.RightLeftTransition;
import com.soarclient.gui.modmenu.pages.profile.ProfileAddPage;
import com.soarclient.management.color.api.ColorPalette;
import com.soarclient.management.mod.impl.settings.ModMenuSettings;
import com.soarclient.management.profile.Profile;
import com.soarclient.management.profile.ProfileIcon;
import com.soarclient.skia.Skia;
import com.soarclient.skia.font.Fonts;
import com.soarclient.skia.font.Icon;
import com.soarclient.ui.component.Component;
import com.soarclient.ui.component.handler.impl.ButtonHandler;
import com.soarclient.ui.component.impl.Button;
import com.soarclient.ui.component.impl.IconButton;
import com.soarclient.utils.ColorUtils;
import com.soarclient.utils.SearchUtils;
import com.soarclient.utils.mouse.MouseUtils;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ProfilePage extends Page {

    private final List<Item> items = new ArrayList<>();
    private Component addButton;

    public ProfilePage(SoarGui parent) {
        super(parent, "text.profile", Icon.DESCRIPTION, new RightLeftTransition(true));
    }

    @Override
    public void init() {
        super.init();
        items.clear();

        for (Profile p : Soar.getInstance().getProfileManager().getProfiles()) {
            items.add(new Item(p));
        }

        for (Item i : items) {
            i.xAnimation.setFirstTick(true);
            i.yAnimation.setFirstTick(true);
        }

        boolean isWinStyle = ModMenuSettings.getInstance().getUiStyleSetting().getOption().equals("win");
        if (isWinStyle) {
            addButton = new IconButton(Icon.ADD, x + width - 60, y + height - 60, IconButton.Size.NORMAL, IconButton.Style.PRIMARY);
        } else {
            addButton = new IconButton(Icon.ADD, x + width - 84, y + height - 84, IconButton.Size.LARGE, IconButton.Style.SECONDARY);
        }

        addButton.setHandler(new ButtonHandler() {
            @Override
            public void onAction() {
                parent.setCurrentPage(new ProfileAddPage(parent, ProfilePage.this.getClass()));
                ProfilePage.this.setTransition(new LeftRightTransition(true));
            }
        });
    }

    @Override
    public void draw(double mouseX, double mouseY) {
        super.draw(mouseX, mouseY);

        boolean isWinStyle = ModMenuSettings.getInstance().getUiStyleSetting().getOption().equals("win");
        List<Item> filteredItems = items.stream()
            .filter(i -> searchBar.getText().isEmpty() || SearchUtils.isSimilar(i.profile.getName() + " " + i.profile.getAuthor(), searchBar.getText()))
            .collect(Collectors.toList());

        double relativeMouseY = mouseY - scrollHelper.getValue();

        addButton.draw(mouseX, mouseY);

        Skia.save();
        Skia.translate(0, scrollHelper.getValue());

        if (isWinStyle) {
            drawWinStyle(mouseX, relativeMouseY, filteredItems);
        } else {
            drawMd3Style(filteredItems);
        }

        Skia.restore();
    }

    private void drawWinStyle(double mouseX, double mouseY, List<Item> filteredItems) {
        ColorPalette palette = Soar.getInstance().getColorManager().getPalette();
        float leftMargin = 32;
        float topMargin = 80;
        float itemHeight = 60;
        float itemSpacing = 12;
        float listWidth = width - leftMargin * 2;

        float currentY = y + topMargin;

        for (Item i : filteredItems) {
            Profile p = i.profile;
            float itemX = x + leftMargin;

            i.xAnimation.onTick(itemX, 14);
            i.yAnimation.onTick(currentY, 14);
            itemX = i.xAnimation.getValue();
            float itemY = i.yAnimation.getValue();

            boolean isHovered = MouseUtils.isInside(mouseX, mouseY, itemX, itemY, listWidth, itemHeight);

            Skia.drawRoundedRect(itemX, itemY, listWidth, itemHeight, 8, palette.getSurfaceContainer());
            if (isHovered) {
                Skia.drawRoundedRect(itemX, itemY, listWidth, itemHeight, 8, ColorUtils.applyAlpha(palette.getOnSurface(), 0.05f));
            }

            Object icon = p.getIcon();
            float iconSize = 44;
            float iconX = itemX + 8;
            float iconY = itemY + (itemHeight - iconSize) / 2;
            if (icon instanceof ProfileIcon) {
                Skia.drawRoundedImage(((ProfileIcon) icon).getIconPath(), iconX, iconY, iconSize, iconSize, 6);
            } else if (icon instanceof File) {
                Skia.drawRoundedImage(((File) icon), iconX, iconY, iconSize, iconSize, 6);
            } else {
                Skia.drawRoundedRect(iconX, iconY, iconSize, iconSize, 6, palette.getSurfaceContainerHigh());
            }

            float textX = iconX + iconSize + 12;
            Skia.drawHeightCenteredText(p.getName(), textX, itemY + itemHeight / 2 - 8, palette.getOnSurface(), Fonts.getMedium(16));
            Skia.drawHeightCenteredText("By: " + p.getAuthor(), textX, itemY + itemHeight / 2 + 8, palette.getOnSurfaceVariant(), Fonts.getRegular(14));

            i.loadButton.setX(itemX + listWidth - i.loadButton.getWidth() - 8);
            i.loadButton.setY(itemY + (itemHeight - i.loadButton.getHeight()) / 2);
            i.loadButton.draw(mouseX, mouseY);

            currentY += itemHeight + itemSpacing;
        }

        float contentHeight = (currentY) - (y + topMargin);
        scrollHelper.setMaxScroll(contentHeight, height - topMargin);
    }

    private void drawMd3Style(List<Item> filteredItems) {
        ColorPalette palette = Soar.getInstance().getColorManager().getPalette();
        int index = 0;
        float offsetX = 26;
        float offsetY = 0;

        for (Item i : filteredItems) {
            Profile p = i.profile;
            float itemX = x + offsetX;
            float itemY = y + 96 + offsetY;

            i.xAnimation.onTick(itemX, 14);
            i.yAnimation.onTick(itemY, 14);
            itemX = i.xAnimation.getValue();
            itemY = i.yAnimation.getValue();

            Skia.drawRoundedRect(itemX, itemY, 245, 88, 12, palette.getSurface());

            Object icon = p.getIcon();
            if (icon instanceof ProfileIcon) {
                Skia.drawRoundedImage(((ProfileIcon) icon).getIconPath(), itemX + 8, itemY + 8, 72, 72, 12);
            } else if (icon instanceof File) {
                Skia.drawRoundedImage(((File) icon), itemX + 8, itemY + 8, 72, 72, 12);
            } else {
                Skia.drawRoundedRect(itemX + 8, itemY + 8, 72, 72, 12, palette.getSurfaceContainer());
            }

            Skia.drawText(p.getName(), itemX + 86, itemY + 16, palette.getOnSurface(), Fonts.getMedium(20));

            index++;
            offsetX += 26 + 245;

            if (index % 3 == 0) {
                offsetX = 26;
                offsetY += 22 + 88;
            }
        }
        scrollHelper.setMaxScroll(88, 22, index, 3, height - 96);
    }

    @Override
    public void mousePressed(double mouseX, double mouseY, int button) {
        super.mousePressed(mouseX, mouseY, button);
        addButton.mousePressed(mouseX, mouseY, button);

        if (ModMenuSettings.getInstance().getUiStyleSetting().getOption().equals("win")) {
            double relativeMouseY = mouseY - scrollHelper.getValue();
            for (Item i : items) {
                i.loadButton.mousePressed(mouseX, relativeMouseY, button);
            }
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        super.mouseReleased(mouseX, mouseY, button);
        addButton.mouseReleased(mouseX, mouseY, button);

        double relativeMouseY = mouseY - scrollHelper.getValue();
        boolean isWinStyle = ModMenuSettings.getInstance().getUiStyleSetting().getOption().equals("win");

        for (Item i : items) {
            if (isWinStyle) {
                i.loadButton.mouseReleased(mouseX, relativeMouseY, button);
            } else {
                float itemX = i.xAnimation.getValue();
                float itemY = i.yAnimation.getValue();
                if (MouseUtils.isInside(mouseX, relativeMouseY, itemX, itemY, 245, 88) && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    Soar.getInstance().getProfileManager().load(i.profile);
                }
            }
        }
    }

    @Override
    public void onClosed() {
        this.setTransition(new RightLeftTransition(true));
    }

    private static class Item {
        private final Profile profile;
        private final SimpleAnimation xAnimation = new SimpleAnimation();
        private final SimpleAnimation yAnimation = new SimpleAnimation();
        private final Button loadButton;

        private Item(Profile profile) {
            this.profile = profile;
            this.loadButton = new Button("Load", 0, 0, Button.Style.TONAL);
            this.loadButton.setHandler(new ButtonHandler() {
                @Override
                public void onAction() {
                    Soar.getInstance().getProfileManager().load(profile);
                }
            });
        }
    }
}
