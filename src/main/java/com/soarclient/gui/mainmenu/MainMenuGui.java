package com.soarclient.gui.mainmenu;

import java.util.ArrayList;
import java.util.List;

import com.soarclient.Soar;
import com.soarclient.animation.SimpleAnimation;
import com.soarclient.gui.api.SimpleSoarGui;
import com.soarclient.management.color.api.ColorPalette;
import com.soarclient.management.mod.impl.settings.ModMenuSettings;
import com.soarclient.skia.Skia;
import com.soarclient.skia.font.Fonts;
import com.soarclient.ui.component.handler.impl.SliderHandler;
import com.soarclient.skia.font.Icon;
import com.soarclient.ui.component.api.PressAnimation;
import com.soarclient.ui.component.impl.Button;
import com.soarclient.ui.component.impl.Slider;
import com.soarclient.ui.component.impl.Switch;
import com.soarclient.ui.component.handler.impl.ButtonHandler;
import com.soarclient.ui.component.handler.impl.SwitchHandler;
import com.soarclient.utils.ColorUtils;
import com.soarclient.utils.language.I18n;
import com.soarclient.utils.mouse.MouseUtils;

import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.realms.gui.screen.RealmsMainScreen;

public class MainMenuGui extends SimpleSoarGui {

    private List<MainMenuButton> buttons = new ArrayList<>();
    private MainMenuButton settingsButton;
    private float lastWindowWidth = 0;
    private float lastWindowHeight = 0;
    private boolean wasMinimized = false;
    private static boolean showCustomizationWindow = false;
    private static Switch darkModeSwitch;
    private Button exitCustomizationButton;

    public MainMenuGui() {
        super(false);
    }

    @Override
    public void init() {
        updateLayout();
        initCustomizationComponents();
    }

    private void updateLayout() {
        buttons.clear();

        float scaleFactor = calculateScaleFactor();

        float centerX = client.getWindow().getWidth() / 2f;
        float centerY = client.getWindow().getHeight() / 2f;
        float buttonWidth = 240 * scaleFactor;

        buttons.add(new MainMenuButton("menu.singleplayer", Icon.HOME,
            centerX - buttonWidth / 2, centerY - (120 * scaleFactor), buttonWidth, scaleFactor, () -> {
            client.setScreen(new SelectWorldScreen(this.build()));
        }));

        buttons.add(new MainMenuButton("menu.multiplayer", Icon.GROUPS,
            centerX - buttonWidth / 2, centerY - (60 * scaleFactor), buttonWidth, scaleFactor, () -> {
            client.setScreen(new MultiplayerScreen(this.build()));
        }));

        buttons.add(new MainMenuButton("menu.realms", Icon.DNS,
            centerX - buttonWidth / 2, centerY, buttonWidth, scaleFactor, () -> {
            client.setScreen(new RealmsMainScreen(this.build()));
        }));

        buttons.add(new MainMenuButton("menu.options", Icon.SETTINGS,
            centerX - buttonWidth / 2, centerY + (60 * scaleFactor), buttonWidth, scaleFactor, () -> {
            client.setScreen(new OptionsScreen(this.build(), client.options));
        }));

        buttons.add(new MainMenuButton("menu.quit", Icon.CLOSE,
            centerX - buttonWidth / 2, centerY + (120 * scaleFactor), buttonWidth, scaleFactor, () -> {
            client.scheduleStop();
        }));

        float settingsButtonSize = 40 * scaleFactor;
        settingsButton = new MainMenuButton("", Icon.SETTINGS,
            client.getWindow().getWidth() - settingsButtonSize - (20 * scaleFactor),
            20 * scaleFactor, settingsButtonSize, scaleFactor, () -> {
            showCustomizationWindow = true;
        });

        lastWindowWidth = client.getWindow().getWidth();
        lastWindowHeight = client.getWindow().getHeight();
    }

    private void initCustomizationComponents() {
        float centerX = client.getWindow().getWidth() / 2f;
        float centerY = client.getWindow().getHeight() / 2f;
        float panelWidth = 450;
        float panelX = centerX - panelWidth / 2;

        darkModeSwitch = new Switch(panelX + panelWidth - 72, centerY - 50, ModMenuSettings.getInstance().getDarkModeSetting().isEnabled());
        darkModeSwitch.setHandler(new SwitchHandler() {
            @Override
            public void onEnabled() {
                ModMenuSettings.getInstance().getDarkModeSetting().setEnabled(true);
            }

            @Override
            public void onDisabled() {
                ModMenuSettings.getInstance().getDarkModeSetting().setEnabled(false);
            }
        });
        // sorry lc
        exitCustomizationButton = new Button(I18n.get("gui.done"), centerX - 50, centerY + 50, Button.Style.TONAL);
        exitCustomizationButton.setHandler(new ButtonHandler() {
            @Override
            public void onAction() {
                showCustomizationWindow = false;
            }
        });
    }

    private boolean isWindowMinimized() {
        return client.getWindow().getWidth() < 100 || client.getWindow().getHeight() < 100;
    }

    private float calculateScaleFactor() {
        float currentWidth = client.getWindow().getWidth();
        float currentHeight = client.getWindow().getHeight();

        if (isWindowMinimized()) {
            return 0.5f;
        }

        float windowArea = currentWidth * currentHeight;

        if (windowArea < 800 * 600) {
            return 1.4f;
        } else if (windowArea < 1280 * 720) {
            return 1.2f;
        } else if (windowArea < 1920 * 1080) {
            return 1.0f;
        } else {
            return 0.9f;
        }
    }

    @Override
    public void draw(double mouseX, double mouseY) {
        boolean currentlyMinimized = isWindowMinimized();

        if (client.getWindow().getWidth() != lastWindowWidth ||
            client.getWindow().getHeight() != lastWindowHeight ||
            wasMinimized != currentlyMinimized) {
            updateLayout();
            initCustomizationComponents();
            wasMinimized = currentlyMinimized;
        }

        if (currentlyMinimized) {
            return;
        }

        Soar instance = Soar.getInstance();
        ColorPalette palette = instance.getColorManager().getPalette();

        drawCustomBackground(palette);
        drawLogoIcon();

        for (MainMenuButton button : buttons) {
            button.draw((int) mouseX, (int) mouseY);
        }

        settingsButton.draw((int) mouseX, (int) mouseY);

        if (showCustomizationWindow) {
            drawCustomizationWindow(mouseX, mouseY, palette);
        }
    }

    private void drawCustomizationWindow(double mouseX, double mouseY, ColorPalette palette) {
        float centerX = client.getWindow().getWidth() / 2f;
        float centerY = client.getWindow().getHeight() / 2f;
        float panelWidth = 450;
        float panelHeight = 200;
        float panelX = centerX - panelWidth / 2;
        float panelY = centerY - panelHeight / 2;

        Skia.drawRect(0, 0, client.getWindow().getWidth(), client.getWindow().getHeight(),
            ColorUtils.applyAlpha(palette.getSurface(), 0.3f));

        Skia.drawRoundedRect(panelX, panelY, panelWidth, panelHeight, 20, palette.getSurfaceContainer());

        Skia.drawCenteredText(I18n.get("gui.mainmenu.customization"), centerX, panelY + 25,
            palette.getOnSurface(), Fonts.getRegular(20));

        Skia.drawText(I18n.get("setting.darkmode"), panelX + 20, centerY - 35,
            palette.getOnSurface(), Fonts.getRegular(16));

        darkModeSwitch.draw(mouseX, mouseY);
        exitCustomizationButton.draw(mouseX, mouseY);
    }

    private void drawCustomBackground(ColorPalette palette) {
        Skia.drawImage("Background.png", 0, 0, client.getWindow().getWidth(), client.getWindow().getHeight());
    }

    private void drawLogoIcon() {
        float scaleFactor = calculateScaleFactor();
        float logoSize = 160 * scaleFactor;
        float logoX = client.getWindow().getWidth() / 2f - logoSize / 2;
        float logoY = 150 * scaleFactor;

        Skia.drawRoundedImage("logo.png", logoX, logoY, logoSize, logoSize, 10 * scaleFactor);
    }

    @Override
    public void mousePressed(double mouseX, double mouseY, int button) {
        if (isWindowMinimized()) {
            return;
        }

        if (showCustomizationWindow) {
            darkModeSwitch.mousePressed(mouseX, mouseY, button);
            exitCustomizationButton.mousePressed(mouseX, mouseY, button);
            return;
        }

        for (MainMenuButton menuButton : buttons) {
            menuButton.mousePressed((int) mouseX, (int) mouseY, button);
        }

        settingsButton.mousePressed((int) mouseX, (int) mouseY, button);
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (isWindowMinimized()) {
            return;
        }

        if (showCustomizationWindow) {
            darkModeSwitch.mouseReleased(mouseX, mouseY, button);
            exitCustomizationButton.mouseReleased(mouseX, mouseY, button);
            return;
        }

        for (MainMenuButton menuButton : buttons) {
            menuButton.mouseReleased((int) mouseX, (int) mouseY, button);
        }

        settingsButton.mouseReleased((int) mouseX, (int) mouseY, button);
    }

    @Override
    public void charTyped(char chr, int modifiers) {
        if (showCustomizationWindow) {
            darkModeSwitch.charTyped(chr, modifiers);
            exitCustomizationButton.charTyped(chr, modifiers);
        }
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (showCustomizationWindow) {
            darkModeSwitch.keyPressed(keyCode, scanCode, modifiers);
            exitCustomizationButton.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    private class MainMenuButton {
        private PressAnimation pressAnimation = new PressAnimation();
        private SimpleAnimation focusAnimation = new SimpleAnimation();
        private String title, icon;
        private float x, y, width, height;
        private float scaleFactor;
        private int[] pressedPos;
        private Runnable action;

        public MainMenuButton(String title, String icon, float x, float y, float width, float scaleFactor, Runnable action) {
            this.title = title;
            this.icon = icon;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = 50 * scaleFactor;
            this.scaleFactor = scaleFactor;
            this.action = action;
            this.pressedPos = new int[]{0, 0};
        }

        public void draw(int mouseX, int mouseY) {
            ColorPalette palette = Soar.getInstance().getColorManager().getPalette();
            boolean hovered = MouseUtils.isInside(mouseX, mouseY, x, y, width, height);

            focusAnimation.onTick(hovered ? 1.0F : 0, 12);

            float radius = 20 * scaleFactor;

            Skia.drawRoundedRect(x, y, width, height, radius, palette.getSurface());

            java.awt.Color hoverColor = palette.getPrimary();
            Skia.drawRoundedRect(x, y, width, height, radius,
                ColorUtils.applyAlpha(hoverColor, focusAnimation.getValue() * 0.12F));

            Skia.save();
            Skia.clip(x, y, width, height, radius);
            pressAnimation.draw(x + pressedPos[0], y + pressedPos[1], width, height, palette.getPrimary(), 1);
            Skia.restore();

            java.awt.Color textColor = hovered ?
                ColorUtils.blend(palette.getOnSurfaceVariant(), palette.getPrimary(), focusAnimation.getValue()) :
                palette.getOnSurfaceVariant();

            float fontSize = 18 * scaleFactor;
            float iconSize = 22 * scaleFactor;
            float iconPadding = 16 * scaleFactor;

            if (!title.isEmpty()) {
                Skia.drawFullCenteredText(I18n.get(title), x + (width / 2), y + (height / 2), textColor,
                    Fonts.getRegular(fontSize));

                Skia.drawHeightCenteredText(icon, x + iconPadding, y + (height / 2), textColor, Fonts.getIcon(iconSize));
            } else {
                Skia.drawFullCenteredText(icon, x + (width / 2), y + (height / 2), textColor, Fonts.getIcon(iconSize));
            }
        }

        public void mousePressed(int mouseX, int mouseY, int mouseButton) {
            if (MouseUtils.isInside(mouseX, mouseY, x, y, width, height) && mouseButton == 0) {
                pressedPos = new int[]{mouseX - (int) x, mouseY - (int) y};
                pressAnimation.onPressed(mouseX, mouseY, x, y);
            }
        }

        public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
            if (MouseUtils.isInside(mouseX, mouseY, x, y, width, height) && mouseButton == 0) {
                action.run(); // execute action.
            }
            pressAnimation.onReleased(mouseX, mouseY, x, y); // always release
        }
    }
}
