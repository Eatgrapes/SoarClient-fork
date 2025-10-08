package com.soarclient.gui.modmenu.component;

import com.soarclient.Soar;
import com.soarclient.animation.Animation;
import com.soarclient.animation.Duration;
import com.soarclient.animation.SimpleAnimation;
import com.soarclient.animation.cubicbezier.impl.EaseStandard;
import com.soarclient.animation.other.DummyAnimation;
import com.soarclient.gui.api.SoarGui;
import com.soarclient.gui.api.page.SimplePage;
import com.soarclient.gui.edithud.GuiEditHUD;
import com.soarclient.management.color.api.ColorPalette;
import com.soarclient.management.mod.impl.settings.ModMenuSettings;
import com.soarclient.skia.Skia;
import com.soarclient.skia.font.Fonts;
import com.soarclient.skia.font.Icon;
import com.soarclient.ui.component.Component;
import com.soarclient.ui.component.handler.impl.ButtonHandler;
import com.soarclient.ui.component.impl.IconButton;
import com.soarclient.utils.ColorUtils;
import com.soarclient.utils.language.I18n;
import com.soarclient.utils.mouse.MouseUtils;
import com.soarclient.utils.mouse.ScrollHelper;
import io.github.humbleui.skija.Font;
import io.github.humbleui.types.Rect;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class NavigationRail extends Component {

    private final List<Navigation> navigations = new ArrayList<>();
    private Navigation currentNavigation;
    private final IconButton editButton;
    private final ScrollHelper scrollHelper = new ScrollHelper();

    private final SoarGui parent;

    public NavigationRail(SoarGui parent, float x, float y, float width, float height) {
        super(x, y);
        this.parent = parent;
        this.width = width;
        this.height = height;

        for (SimplePage p : parent.getPages()) {
            Navigation n = new Navigation(p);
            if (p.getTitle().equals(parent.getCurrentPage().getTitle())) {
                currentNavigation = n;
                n.animation = new EaseStandard(Duration.MEDIUM_3, 0, 1);
            }
            navigations.add(n);
        }

        boolean isWinStyle = ModMenuSettings.getInstance().getUiStyleSetting().getOption().equals("win");
        IconButton.Size buttonSize = isWinStyle ? IconButton.Size.SMALL : IconButton.Size.NORMAL;
        float buttonY = isWinStyle ? y + 20 : y + 44;

        editButton = new IconButton(Icon.EDIT, x, buttonY, buttonSize, IconButton.Style.TERTIARY);
        editButton.setX(x + (width / 2) - (editButton.getWidth() / 2));
        editButton.setHandler(new ButtonHandler() {
            @Override
            public void onAction() {
                parent.close(new GuiEditHUD(ModMenuSettings.getInstance().getModMenu()).build());
            }
        });
    }

    @Override
    public void draw(double mouseX, double mouseY) {
        scrollHelper.onUpdate();

        boolean isWinStyle = ModMenuSettings.getInstance().getUiStyleSetting().getOption().equals("win");
        ColorPalette palette = Soar.getInstance().getColorManager().getPalette();

        float borderRadius = isWinStyle ? 8 : 35;
        Skia.drawRoundedRectVarying(x, y, width, height, borderRadius, 0, 0, borderRadius, palette.getSurface());

        editButton.draw(mouseX, mouseY);

        float offsetY = isWinStyle ? 80 : 140;
        float itemSpacing = isWinStyle ? 50 : 68;

        Skia.save();
        Skia.translate(0, scrollHelper.getValue());

        double translatedMouseY = mouseY - scrollHelper.getValue();

        for (Navigation n : navigations) {
            if (isWinStyle) {
                drawWinNavItem(n, mouseX, translatedMouseY, offsetY, palette);
            } else {
                drawMd3NavItem(n, mouseX, translatedMouseY, offsetY, palette);
            }
            offsetY += itemSpacing;
        }

        scrollHelper.setMaxScroll(offsetY - (isWinStyle ? 80 : 140), height - (isWinStyle ? 80 : 140));
        Skia.restore();
    }

    private void drawWinNavItem(Navigation n, double mouseX, double mouseY, float offsetY, ColorPalette palette) {
        SimplePage p = n.page;
        String title = p.getTitle();
        String icon = p.getIcon();

        float selWidth = width - 16;
        float selHeight = 40;
        float itemX = x + (width - selWidth) / 2;
        float itemY = y + offsetY;

        boolean isSelected = currentNavigation.equals(n);
        boolean focus = MouseUtils.isInside(mouseX, mouseY, itemX, itemY, selWidth, selHeight);

        if (isSelected) {
            Skia.drawRoundedRect(itemX, itemY, selWidth, selHeight, 6, palette.getSecondaryContainer());
        } else if (focus) {
            Skia.drawRoundedRect(itemX, itemY, selWidth, selHeight, 6, ColorUtils.applyAlpha(palette.getOnSurfaceVariant(), 0.1f));
        }

        Color textColor = isSelected ? palette.getOnSecondaryContainer() : palette.getOnSurface();
        Font iconFont = isSelected ? Fonts.getIconFill(24) : Fonts.getIcon(24);
        Rect iconBounds = Skia.getTextBounds(icon, iconFont);

        float iconX = itemX + 12;
        float textX = iconX + iconBounds.getWidth() + 10;
        float itemCenterY = itemY + selHeight / 2;

        Skia.drawHeightCenteredText(icon, iconX, itemCenterY, textColor, iconFont);
        Skia.drawHeightCenteredText(I18n.get(title), textX, itemCenterY, textColor, Fonts.getMedium(14));
    }

    private void drawMd3NavItem(Navigation n, double mouseX, double mouseY, float offsetY, ColorPalette palette) {
        SimplePage p = n.page;
        String title = p.getTitle();
        String icon = p.getIcon();
        boolean isSelected = currentNavigation.equals(n);

        Font font = isSelected ? Fonts.getIconFill(24) : Fonts.getIcon(24);
        Rect bounds = Skia.getTextBounds(icon, font);
        float iconWidth = bounds.getWidth();
        float iconHeight = bounds.getHeight();

        Color c0 = isSelected ? palette.getOnSecondaryContainer() : palette.getOnSurfaceVariant();
        Color c1 = isSelected ? palette.getOnSurface() : palette.getOnSurfaceVariant();

        Animation animation = n.animation;
        float selWidth = 56;
        float selHeight = 32;
        boolean focus = MouseUtils.isInside(mouseX, mouseY, x + (width / 2) - (selWidth / 2), y + offsetY, selWidth, selHeight) || n.pressed;

        n.focusAnimation.onTick(focus ? n.pressed ? 0.12F : 0.08F : 0, 8);

        Skia.drawRoundedRect(x + (width / 2) - (selWidth / 2), y + offsetY, selWidth, selHeight, 16, ColorUtils.applyAlpha(palette.getOnSurfaceVariant(), n.focusAnimation.getValue()));

        if (animation.getEnd() != 0 || !animation.isFinished()) {
            Skia.drawRoundedRect(x + (width / 2) - (selWidth / 2) + (selWidth - selWidth * animation.getValue()) / 2, y + offsetY, selWidth * animation.getValue(), selHeight, 16, ColorUtils.applyAlpha(palette.getSecondaryContainer(), animation.getValue()));
        }

        Skia.drawText(icon, x + (width / 2) - (iconWidth / 2), y + (offsetY + (selHeight / 2)) - (iconHeight / 2), c0, font);
        Skia.drawCenteredText(I18n.get(title), x + (width / 2), y + offsetY + selHeight + 5, c1, Fonts.getMedium(12));
    }


    @Override
    public void mousePressed(double mouseX, double mouseY, int button) {
        editButton.mousePressed(mouseX, mouseY, button);

        double translatedMouseY = mouseY - scrollHelper.getValue();

        boolean isWinStyle = ModMenuSettings.getInstance().getUiStyleSetting().getOption().equals("win");
        float offsetY = isWinStyle ? 80 : 140;
        float itemSpacing = isWinStyle ? 50 : 68;

        for (Navigation n : navigations) {
            float selWidth = isWinStyle ? width - 16 : 56;
            float selHeight = isWinStyle ? 40 : 32;
            float itemX = isWinStyle ? x + (width - selWidth) / 2 : x + (width / 2) - (selWidth / 2);
            float itemY = y + offsetY;

            if (MouseUtils.isInside(mouseX, translatedMouseY, itemX, itemY, selWidth, selHeight) && button == GLFW.GLFW_MOUSE_BUTTON_LEFT && !currentNavigation.equals(n)) {
                n.pressed = true;
            }
            offsetY += itemSpacing;
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        editButton.mouseReleased(mouseX, mouseY, button);

        double translatedMouseY = mouseY - scrollHelper.getValue();

        boolean isWinStyle = ModMenuSettings.getInstance().getUiStyleSetting().getOption().equals("win");
        float offsetY = isWinStyle ? 80 : 140;
        float itemSpacing = isWinStyle ? 50 : 68;

        for (Navigation n : navigations) {
            float selWidth = isWinStyle ? width - 16 : 56;
            float selHeight = isWinStyle ? 40 : 32;
            float itemX = isWinStyle ? x + (width - selWidth) / 2 : x + (width / 2) - (selWidth / 2);
            float itemY = y + offsetY;

            if (MouseUtils.isInside(mouseX, translatedMouseY, itemX, itemY, selWidth, selHeight) && button == GLFW.GLFW_MOUSE_BUTTON_LEFT && !currentNavigation.equals(n)) {
                currentNavigation.animation = new EaseStandard(Duration.MEDIUM_3, 1, 0);
                currentNavigation = n;
                parent.setCurrentPage(n.page);
                currentNavigation.animation = new EaseStandard(Duration.MEDIUM_3, 0, 1);
            }
            n.pressed = false;
            offsetY += itemSpacing;
        }
    }

    public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollHelper.onScroll(verticalAmount);
    }

    private static class Navigation {
        private final SimpleAnimation focusAnimation = new SimpleAnimation();
        private Animation animation;
        private final SimplePage page;
        private boolean pressed;

        private Navigation(SimplePage page) {
            this.page = page;
            this.animation = new DummyAnimation();
            this.pressed = false;
        }
    }
}
