package com.soarclient.gui.mainmenu;

import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import com.google.gson.JsonObject;
import com.soarclient.Soar;
import com.soarclient.animation.SimpleAnimation;
import com.soarclient.gui.api.SimpleSoarGui;
import com.soarclient.management.color.api.ColorPalette;
import com.soarclient.management.config.ConfigType;
import com.soarclient.management.mod.impl.settings.ModMenuSettings;
import com.soarclient.skia.Skia;
import com.soarclient.skia.font.Fonts;
import com.soarclient.skia.font.Icon;
import com.soarclient.ui.component.api.PressAnimation;
import com.soarclient.ui.component.impl.Button;
import com.soarclient.ui.component.impl.IconButton;
import com.soarclient.ui.component.impl.Switch;
import com.soarclient.ui.component.handler.impl.ButtonHandler;
import com.soarclient.ui.component.handler.impl.SwitchHandler;
import com.soarclient.utils.ColorUtils;
import com.soarclient.utils.language.I18n;
import com.soarclient.utils.mouse.MouseUtils;
import com.soarclient.utils.Multithreading;
import com.soarclient.utils.file.dialog.SoarFileDialog;
import com.soarclient.utils.file.FileLocation;
import com.soarclient.utils.mouse.ScrollHelper;
import it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;

import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.realms.gui.screen.RealmsMainScreen;

public class MainMenuGui extends SimpleSoarGui {

    private List<MainMenuButton> buttons = new ArrayList<>();
    private MainMenuButton settingsButton;
    private MainMenuButton backgroundButton;
    private float lastWindowWidth = 0;
    private float lastWindowHeight = 0;
    private boolean wasMinimized = false;
    private static boolean showCustomizationWindow = false;
    private static boolean showBackgroundWindow = false;
    private static Switch darkModeSwitch;
    private Button exitCustomizationButton;
    private Button exitBackgroundButton;
    private IconButton addBackgroundButton;
    private List<BackgroundItem> backgroundItems = new ArrayList<>();
    private String selectedBackgroundId = "Background.png";
    private ScrollHelper backgroundScrollHelper = new ScrollHelper();
    private float backgroundScale = 1.2f;
    private float parallaxX = 0;
    private float parallaxY = 0;
    private float parallaxStrength = 40;

    public MainMenuGui() {
        super(false);
    }

    @Override
    public void init() {
        updateLayout();
        loadBackgroundSettings();
        initCustomizationComponents();
    }

    private void loadBackgroundSettings() {
        JsonObject config = Soar.getInstance().getConfigManager().getConfig(ConfigType.MOD).getJsonObject();
        if (config.has("mainmenu.background")) {
            selectedBackgroundId = config.get("mainmenu.background").getAsString();
        }
    }

    private void saveBackgroundSettings() {
        JsonObject config = Soar.getInstance().getConfigManager().getConfig(ConfigType.MOD).getJsonObject();
        config.addProperty("mainmenu.background", selectedBackgroundId);
        Soar.getInstance().getConfigManager().save(ConfigType.MOD);
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

        float buttonSize = 40 * scaleFactor;
        float buttonSpacing = 10 * scaleFactor;

        backgroundButton = new MainMenuButton("", Icon.IMAGE,
            client.getWindow().getWidth() - (buttonSize * 2) - buttonSpacing - (20 * scaleFactor),
            20 * scaleFactor, buttonSize, scaleFactor, () -> {
            showBackgroundWindow = true;
        });

        settingsButton = new MainMenuButton("", Icon.SETTINGS,
            client.getWindow().getWidth() - buttonSize - (20 * scaleFactor),
            20 * scaleFactor, buttonSize, scaleFactor, () -> {
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

        exitCustomizationButton = new Button(I18n.get("gui.done"), centerX - 50, centerY + 50, Button.Style.TONAL);
        exitCustomizationButton.setHandler(new ButtonHandler() {
            @Override
            public void onAction() {
                showCustomizationWindow = false;
            }
        });

        float backgroundPanelWidth = 600;
        float backgroundPanelHeight = 400;
        float backgroundPanelX = centerX - backgroundPanelWidth / 2;
        float backgroundPanelY = centerY - backgroundPanelHeight / 2;

        exitBackgroundButton = new Button(I18n.get("gui.done"), centerX - 50, backgroundPanelY + backgroundPanelHeight - 60, Button.Style.TONAL);
        exitBackgroundButton.setHandler(new ButtonHandler() {
            @Override
            public void onAction() {
                showBackgroundWindow = false;
            }
        });

        addBackgroundButton = new IconButton(Icon.ADD,
            backgroundPanelX + backgroundPanelWidth - 70,
            backgroundPanelY + backgroundPanelHeight - 70,
            IconButton.Size.NORMAL,
            IconButton.Style.PRIMARY);
        addBackgroundButton.setHandler(new ButtonHandler() {
            @Override
            public void onAction() {
                Multithreading.runAsync(() -> {
                    ObjectObjectImmutablePair<Boolean, File> result = SoarFileDialog.chooseFile("Select Background Image", "png", "jpg");

                    if (result.left()) {
                        File selectedFile = result.right();
                        copyBackgroundFile(selectedFile);
                    }
                });
            }
        });

        loadExistingBackgrounds();
    }

    private void loadExistingBackgrounds() {
        backgroundItems.clear();

        backgroundItems.add(new BackgroundItem("Background.png", null, true));

        File backgroundDir = FileLocation.BACKGROUND_DIR;
        if (backgroundDir.exists() && backgroundDir.isDirectory()) {
            File[] backgroundFiles = backgroundDir.listFiles((dir, name) ->
                name.toLowerCase().endsWith(".png") || name.toLowerCase().endsWith(".jpg"));
            if (backgroundFiles != null) {
                for (File backgroundFile : backgroundFiles) {
                    String backgroundId = backgroundFile.getName();
                    backgroundItems.add(new BackgroundItem(backgroundId, backgroundFile, false));
                }
            }
        }
    }

    private void copyBackgroundFile(File selectedFile) {
        try {
            String originalName = selectedFile.getName();
            String processedName = originalName.replace(" ", "_");
            File targetFile = new File(FileLocation.BACKGROUND_DIR, processedName);

            if (targetFile.exists()) {
                System.out.println("Background file already exists!");
                return;
            }

            Files.copy(selectedFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            loadExistingBackgrounds();

            // 自动选择新添加的背景
            selectedBackgroundId = processedName;
            saveBackgroundSettings();

        } catch (IOException e) {
            e.printStackTrace();
        }
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

        backgroundButton.draw((int) mouseX, (int) mouseY);
        settingsButton.draw((int) mouseX, (int) mouseY);

        if (showCustomizationWindow) {
            drawCustomizationWindow(mouseX, mouseY, palette);
        }

        if (showBackgroundWindow) {
            drawBackgroundWindow(mouseX, mouseY, palette);
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

    private void drawBackgroundWindow(double mouseX, double mouseY, ColorPalette palette) {
        float centerX = client.getWindow().getWidth() / 2f;
        float centerY = client.getWindow().getHeight() / 2f;
        float panelWidth = 600;
        float panelHeight = 400;
        float panelX = centerX - panelWidth / 2;
        float panelY = centerY - panelHeight / 2;

        Skia.drawRect(0, 0, client.getWindow().getWidth(), client.getWindow().getHeight(),
            ColorUtils.applyAlpha(palette.getSurface(), 0.3f));

        Skia.drawRoundedRect(panelX, panelY, panelWidth, panelHeight, 20, palette.getSurfaceContainer());

        Skia.drawText(I18n.get("setting.background"), panelX + 20, panelY + 30,
            palette.getOnSurface(), Fonts.getRegular(20));

        double adjustedMouseY = mouseY - backgroundScrollHelper.getValue();

        Skia.save();
        Skia.translate(0, backgroundScrollHelper.getValue());

        float startX = panelX + 20;
        float startY = panelY + 60;
        float itemWidth = 160;
        float itemHeight = 90;
        float spacing = 15;
        int itemsPerRow = 3;

        for (int i = 0; i < backgroundItems.size(); i++) {
            BackgroundItem item = backgroundItems.get(i);

            int row = i / itemsPerRow;
            int col = i % itemsPerRow;

            float itemX = startX + col * (itemWidth + spacing);
            float itemY = startY + row * (itemHeight + spacing);

            item.xAnimation.onTick(itemX, 14);
            item.yAnimation.onTick(itemY, 14);

            itemX = item.xAnimation.getValue();
            itemY = item.yAnimation.getValue();

            boolean isSelected = item.backgroundId.equals(selectedBackgroundId);
            boolean isHovered = MouseUtils.isInside(mouseX, adjustedMouseY, itemX, itemY, itemWidth, itemHeight);

            item.focusAnimation.onTick(isHovered ? 1 : 0, 10);

            java.awt.Color bgColor = palette.getSurface();
            Skia.drawRoundedRect(itemX, itemY, itemWidth, itemHeight, 8, bgColor);

            if (isSelected) {
                Skia.drawOutline(itemX - 2, itemY - 2, itemWidth + 4, itemHeight + 4, 10, 3, palette.getPrimary());
            }

            if (item.isDefault) {
                Skia.drawRoundedImage("Background.png", itemX, itemY, itemWidth, itemHeight, 8);
            } else if (item.backgroundFile != null && item.backgroundFile.exists()) {
                Skia.drawRoundedImage(item.backgroundFile, itemX, itemY, itemWidth, itemHeight, 8);
            }
        }

        int totalRows = (int) Math.ceil((double) backgroundItems.size() / itemsPerRow);
        float totalHeight = totalRows * (itemHeight + spacing);
        backgroundScrollHelper.setMaxScroll(totalHeight, panelHeight - 120);

        Skia.restore();

        exitBackgroundButton.draw(mouseX, mouseY);
        addBackgroundButton.draw(mouseX, mouseY);
    }

    private void drawCustomBackground(ColorPalette palette) {
        // 计算视差效果
        float targetParallaxX = (float) (client.mouse.getX() - client.getWindow().getWidth() / 2) / client.getWindow().getWidth() * parallaxStrength;
        float targetParallaxY = (float) (client.mouse.getY() - client.getWindow().getHeight() / 2) / client.getWindow().getHeight() * parallaxStrength;

        parallaxX += (targetParallaxX - parallaxX) * 0.1f;
        parallaxY += (targetParallaxY - parallaxY) * 0.1f;

        float scaledWidth = client.getWindow().getWidth() * backgroundScale;
        float scaledHeight = client.getWindow().getHeight() * backgroundScale;

        float offsetX = (scaledWidth - client.getWindow().getWidth()) / 2 - parallaxX;
        float offsetY = (scaledHeight - client.getWindow().getHeight()) / 2 - parallaxY;

        if (selectedBackgroundId.equals("Background.png")) {
            Skia.drawImage("Background.png", -offsetX, -offsetY, scaledWidth, scaledHeight);
        } else {
            for (BackgroundItem item : backgroundItems) {
                if (item.backgroundId.equals(selectedBackgroundId) && !item.isDefault) {
                    if (item.backgroundFile != null && item.backgroundFile.exists()) {
                        Skia.drawImage(item.backgroundFile, -offsetX, -offsetY, scaledWidth, scaledHeight);
                        return;
                    }
                }
            }
            Skia.drawImage("Background.png", -offsetX, -offsetY, scaledWidth, scaledHeight);
        }
    }

    private void drawLogoIcon() {
        float scaleFactor = calculateScaleFactor();
        float logoSize = 170 * scaleFactor;
        float logoX = client.getWindow().getWidth() / 2f - logoSize / 2;

        float centerY = client.getWindow().getHeight() / 2f;
        float singleplayerButtonY = centerY - (120 * scaleFactor);
        float logoY = singleplayerButtonY - logoSize - (1 * scaleFactor);

        Skia.drawRoundedImage("logo.png", logoX, logoY, logoSize, logoSize, 10 * scaleFactor);
    }

    @Override
    public void mousePressed(double mouseX, double mouseY, int button) {
        if (isWindowMinimized()) {
            return;
        }

        if (showBackgroundWindow) {
            exitBackgroundButton.mousePressed(mouseX, mouseY, button);
            addBackgroundButton.mousePressed(mouseX, mouseY, button);

            float adjustedMouseY = (float) (mouseY - backgroundScrollHelper.getValue());
            float panelX = client.getWindow().getWidth() / 2f - 300;
            float panelY = client.getWindow().getHeight() / 2f - 200;
            float startX = panelX + 20;
            float startY = panelY + 60;
            float itemWidth = 160;
            float itemHeight = 90;
            float spacing = 15;
            int itemsPerRow = 3;

            for (int i = 0; i < backgroundItems.size(); i++) {
                BackgroundItem item = backgroundItems.get(i);
                int row = i / itemsPerRow;
                int col = i % itemsPerRow;
                float itemX = startX + col * (itemWidth + spacing);
                float itemY = startY + row * (itemHeight + spacing);

                if (MouseUtils.isInside(mouseX, adjustedMouseY, itemX, itemY, itemWidth, itemHeight)) {
                    selectedBackgroundId = item.backgroundId;
                    saveBackgroundSettings();  // 保存新选择的背景
                    break;
                }
            }
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

        backgroundButton.mousePressed((int) mouseX, (int) mouseY, button);
        settingsButton.mousePressed((int) mouseX, (int) mouseY, button);
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (isWindowMinimized()) {
            return;
        }

        if (showBackgroundWindow) {
            exitBackgroundButton.mouseReleased(mouseX, mouseY, button);
            addBackgroundButton.mouseReleased(mouseX, mouseY, button);
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

        backgroundButton.mouseReleased((int) mouseX, (int) mouseY, button);
        settingsButton.mouseReleased((int) mouseX, (int) mouseY, button);
    }

    public void mouseScrolled(double mouseX, double mouseY, double amount) {
        if (showBackgroundWindow) {
            backgroundScrollHelper.onScroll(amount);
        }
    }

    @Override
    public void charTyped(char chr, int modifiers) {
        if (showBackgroundWindow) {
            exitBackgroundButton.charTyped(chr, modifiers);
            addBackgroundButton.charTyped(chr, modifiers);
        }

        if (showCustomizationWindow) {
            darkModeSwitch.charTyped(chr, modifiers);
            exitCustomizationButton.charTyped(chr, modifiers);
        }
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (showBackgroundWindow) {
            exitBackgroundButton.keyPressed(keyCode, scanCode, modifiers);
            addBackgroundButton.keyPressed(keyCode, scanCode, modifiers);
        }

        if (showCustomizationWindow) {
            darkModeSwitch.keyPressed(keyCode, scanCode, modifiers);
            exitCustomizationButton.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    private class BackgroundItem {
        private String backgroundId;
        private File backgroundFile;
        private SimpleAnimation xAnimation = new SimpleAnimation();
        private SimpleAnimation yAnimation = new SimpleAnimation();
        private SimpleAnimation focusAnimation = new SimpleAnimation();
        private boolean isDefault;

        public BackgroundItem(String backgroundId, File backgroundFile, boolean isDefault) {
            this.backgroundId = backgroundId;
            this.backgroundFile = backgroundFile;
            this.isDefault = isDefault;
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
                action.run();
            }
            pressAnimation.onReleased(mouseX, mouseY, x, y);
        }
    }
}
