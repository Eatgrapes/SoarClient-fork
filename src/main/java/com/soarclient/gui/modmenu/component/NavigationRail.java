package com.soarclient.gui.modmenu.component;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

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

public class NavigationRail extends Component {

    private List<Navigation> navigations = new ArrayList<>();
    private Navigation currentNavigation;
    private IconButton editButton;
    private ScrollHelper scrollHelper = new ScrollHelper();

    private SoarGui parent;

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

        editButton = new IconButton(Icon.EDIT, x, y + 44, IconButton.Size.NORMAL, IconButton.Style.TERTIARY);
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

        ColorPalette palette = Soar.getInstance().getColorManager().getPalette();

        Skia.drawRoundedRectVarying(x, y, width, height, 35, 0, 0, 35, palette.getSurface());

        editButton.draw(mouseX, mouseY);

        float offsetY = 140;

        Skia.save();
        Skia.translate(0, scrollHelper.getValue());

        mouseY = mouseY - scrollHelper.getValue();

        for (Navigation n : navigations) {
            SimplePage p = n.page;
            String title = p.getTitle();
            String icon = p.getIcon();
            Font font = currentNavigation.equals(n) ? Fonts.getIconFill(24) : Fonts.getIcon(24);
            Rect bounds = Skia.getTextBounds(icon, font);
            float iconWidth = bounds.getWidth();
            float iconHeight = bounds.getHeight();

            Color c0 = currentNavigation.equals(n) ? palette.getOnSecondaryContainer() : palette.getOnSurfaceVariant();
            Color c1 = currentNavigation.equals(n) ? palette.getOnSurface() : palette.getOnSurfaceVariant();

            Animation animation = n.animation;
            float selWidth = 56;
            float selHeight = 32;
            boolean focus = MouseUtils.isInside(mouseX, mouseY, x + (width / 2) - (selWidth / 2), y + offsetY, selWidth,
                selHeight) || n.pressed;

            n.focusAnimation.onTick(focus ? n.pressed ? 0.12F : 0.08F : 0, 8);

            Skia.drawRoundedRect(x + (width / 2) - (selWidth / 2), y + offsetY, selWidth, selHeight, 16,
                ColorUtils.applyAlpha(palette.getOnSurfaceVariant(), n.focusAnimation.getValue()));

            if (animation.getEnd() != 0 || !animation.isFinished()) {
                Skia.drawRoundedRect(
                    x + (width / 2) - (selWidth / 2) + (selWidth - selWidth * animation.getValue()) / 2,
                    y + offsetY, selWidth * animation.getValue(), selHeight, 16,
                    ColorUtils.applyAlpha(palette.getSecondaryContainer(), animation.getValue()));
            }

            Skia.drawText(icon, x + (width / 2) - (iconWidth / 2), y + (offsetY + (selHeight / 2)) - (iconHeight / 2),
                c0, font);
            Skia.drawCenteredText(I18n.get(title), x + (width / 2), y + offsetY + selHeight + 5, c1,
                Fonts.getMedium(12));

            offsetY += 68;
        }

        scrollHelper.setMaxScroll(offsetY - 140, height - 140);
        Skia.restore();
    }

    @Override
    public void mousePressed(double mouseX, double mouseY, int button) {
        float offsetY = 140;
        float selWidth = 56;
        float selHeight = 32;

        editButton.mousePressed(mouseX, mouseY, button);

        mouseY = mouseY - scrollHelper.getValue();

        for (Navigation n : navigations) {
            if (MouseUtils.isInside(mouseX, mouseY, x + (width / 2) - (selWidth / 2), y + offsetY, selWidth, selHeight)
                && button == GLFW.GLFW_MOUSE_BUTTON_LEFT && !currentNavigation.equals(n)) {
                n.pressed = true;
            }
            offsetY += 68;
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        float offsetY = 140;
        float selWidth = 56;
        float selHeight = 32;

        editButton.mouseReleased(mouseX, mouseY, button);

        mouseY = mouseY - scrollHelper.getValue();

        for (Navigation n : navigations) {
            if (MouseUtils.isInside(mouseX, mouseY, x + (width / 2) - (selWidth / 2), y + offsetY, selWidth, selHeight)
                && button == GLFW.GLFW_MOUSE_BUTTON_LEFT && !currentNavigation.equals(n)) {
                currentNavigation.animation = new EaseStandard(Duration.MEDIUM_3, 1, 0);
                currentNavigation = n;
                parent.setCurrentPage(n.page);
                currentNavigation.animation = new EaseStandard(Duration.MEDIUM_3, 0, 1);
            }
            n.pressed = false;
            offsetY += 68;
        }
    }

    public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollHelper.onScroll(verticalAmount);
    }

    private class Navigation {
        private SimpleAnimation focusAnimation = new SimpleAnimation();
        private Animation animation;
        private SimplePage page;
        private boolean pressed;

        private Navigation(SimplePage page) {
            this.page = page;
            this.animation = new DummyAnimation();
            this.pressed = false;
        }
    }
}
