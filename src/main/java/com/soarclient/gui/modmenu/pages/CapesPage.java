package com.soarclient.gui.modmenu.pages;

import com.soarclient.Soar;
import com.soarclient.animation.SimpleAnimation;
import com.soarclient.gui.api.SoarGui;
import com.soarclient.gui.api.page.Page;
import com.soarclient.gui.api.page.impl.RightLeftTransition;
import com.soarclient.management.cape.CapeManager;
import com.soarclient.management.cape.CapeRenderer;
import com.soarclient.management.color.api.ColorPalette;
import com.soarclient.management.mod.impl.settings.ModMenuSettings;
import com.soarclient.skia.Skia;
import com.soarclient.skia.font.Fonts;
import com.soarclient.skia.font.Icon;
import com.soarclient.ui.component.Component;
import com.soarclient.ui.component.handler.impl.ButtonHandler;
import com.soarclient.ui.component.impl.Button;
import com.soarclient.ui.component.impl.IconButton;
import com.soarclient.utils.ColorUtils;
import com.soarclient.utils.Multithreading;
import com.soarclient.utils.file.FileLocation;
import com.soarclient.utils.file.dialog.SoarFileDialog;
import com.soarclient.utils.language.I18n;
import com.soarclient.utils.mouse.MouseUtils;
import it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import org.lwjgl.glfw.GLFW;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class CapesPage extends Page {

    private final List<CapeItem> capeItems = new ArrayList<>();
    private Component addButton;
    private String selectedCapeId = null;

    public CapesPage(SoarGui parent) {
        super(parent, "text.capes", Icon.STYLE, new RightLeftTransition(true));
    }

    @Override
    public void init() {
        super.init();

        boolean isWinStyle = ModMenuSettings.getInstance().getUiStyleSetting().getOption().equals("win");
        if (isWinStyle) {
            addButton = new IconButton(Icon.ADD, x + width - 60, y + height - 60, IconButton.Size.NORMAL, IconButton.Style.PRIMARY);
        } else {
            addButton = new IconButton(Icon.ADD, x + width - 80, y + height - 80, IconButton.Size.LARGE, IconButton.Style.SECONDARY);
        }

        addButton.setHandler(new ButtonHandler() {
            @Override
            public void onAction() {
                uploadCape();
            }
        });

        loadExistingCapes();
        selectedCapeId = Soar.getInstance().getCapeManager().getSelectedCapeId();
    }

    private void loadExistingCapes() {
        capeItems.clear();
        File capesDir = FileLocation.CAPES_DIR;
        if (capesDir.exists() && capesDir.isDirectory()) {
            File[] capeFiles = capesDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
            if (capeFiles != null) {
                for (File capeFile : capeFiles) {
                    String capeId = capeFile.getName().replace(".png", "");
                    capeItems.add(new CapeItem(capeId, capeFile));
                    try {
                        byte[] capeData = Files.readAllBytes(capeFile.toPath());
                        Soar.getInstance().getCapeManager().loadCape(capeId, capeData);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void uploadCape() {
        Multithreading.runAsync(() -> {
            ObjectObjectImmutablePair<Boolean, File> result = SoarFileDialog.chooseFile("Select Cape", "png");

            if (result.left()) {
                File selectedFile = result.right();
                if (validateCape(selectedFile)) {
                    String originalName = selectedFile.getName();
                    String processedName = originalName.replace(" ", "_");
                    File targetFile = new File(FileLocation.CAPES_DIR, processedName);

                    if (targetFile.exists()) {
                        System.out.println("Cape already exists!");
                        return;
                    }

                    try {
                        Files.copy(selectedFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        loadExistingCapes();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("Invalid cape format!");
                }
            }
        });
    }

    private boolean validateCape(File file) {
        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null) return false;
            int width = image.getWidth();
            int height = image.getHeight();
            return (width == 64 && height == 32) || (width == 128 && height == 64);
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public void draw(double mouseX, double mouseY) {
        super.draw(mouseX, mouseY);

        boolean isWinStyle = ModMenuSettings.getInstance().getUiStyleSetting().getOption().equals("win");
        double relativeMouseY = mouseY - scrollHelper.getValue();

        addButton.draw(mouseX, mouseY);

        Skia.save();
        Skia.translate(0, scrollHelper.getValue());

        if (isWinStyle) {
            drawWinStyle(mouseX, relativeMouseY);
        } else {
            drawMd3Style(mouseX, relativeMouseY);
        }

        Skia.restore();
    }

    private void drawWinStyle(double mouseX, double mouseY) {
        ColorPalette palette = Soar.getInstance().getColorManager().getPalette();
        float leftMargin = 32;
        float topMargin = 80;
        int columnCount = 4;
        float itemSpacing = 24;
        float itemWidth = (width - leftMargin * 2 - (columnCount - 1) * itemSpacing) / columnCount;
        float itemHeight = itemWidth * 1.5f;

        float currentX = x + leftMargin;
        float currentY = y + topMargin;

        for (int i = 0; i < capeItems.size(); i++) {
            CapeItem item = capeItems.get(i);

            item.xAnimation.onTick(currentX, 14);
            item.yAnimation.onTick(currentY, 14);
            float itemX = item.xAnimation.getValue();
            float itemY = item.yAnimation.getValue();

            boolean isSelected = item.capeId.equals(selectedCapeId);
            boolean isHovered = MouseUtils.isInside(mouseX, mouseY, itemX, itemY, itemWidth, itemHeight);

            Skia.drawRoundedRect(itemX, itemY, itemWidth, itemHeight, 8, palette.getSurfaceContainer());

            if (isHovered) {
                Skia.drawRoundedRect(itemX, itemY, itemWidth, itemHeight, 8, ColorUtils.applyAlpha(palette.getOnSurface(), 0.05f));
            }

            if (isSelected) {
                Skia.drawOutline(itemX, itemY, itemWidth, itemHeight, 8, 2, palette.getPrimary());
            }

            if (item.capeFile.exists()) {
                CapeRenderer.renderRoundedCapePreview(Soar.getInstance().getCapeManager().getLoadedCape(item.capeId), itemX + 4, itemY + 4, itemWidth - 8, itemHeight - 8, 6);
            }

            currentX += itemWidth + itemSpacing;
            if ((i + 1) % columnCount == 0) {
                currentX = x + leftMargin;
                currentY += itemHeight + itemSpacing;
            }
        }

        float contentHeight = (currentY + itemHeight) - (y + topMargin);
        scrollHelper.setMaxScroll(contentHeight, height - topMargin);
    }

    private void drawMd3Style(double mouseX, double mouseY) {
        ColorPalette palette = Soar.getInstance().getColorManager().getPalette();
        float startX = x + 32;
        float startY = y + 120;
        float itemWidth = 140;
        float itemHeight = 220;
        float spacing = 20;
        int itemsPerRow = 5;

        for (int i = 0; i < capeItems.size(); i++) {
            CapeItem item = capeItems.get(i);
            int row = i / itemsPerRow;
            int col = i % itemsPerRow;
            float itemX = startX + col * (itemWidth + spacing);
            float itemY = startY + row * (itemHeight + spacing);

            item.xAnimation.onTick(itemX, 14);
            item.yAnimation.onTick(itemY, 14);
            itemX = item.xAnimation.getValue();
            itemY = item.yAnimation.getValue();

            boolean isSelected = item.capeId.equals(selectedCapeId);
            boolean isHovered = MouseUtils.isInside(mouseX, mouseY, itemX, itemY, itemWidth, itemHeight);
            item.focusAnimation.onTick(isHovered ? 1 : 0, 10);

            Color bgColor = isSelected ? palette.getPrimaryContainer() : palette.getSurface();
            Skia.drawRoundedRect(itemX + 5, itemY + 5, itemWidth - 10, itemHeight - 10, 12, bgColor);

            if (isSelected) {
                Skia.drawOutline(itemX + 3, itemY + 3, itemWidth - 6, itemHeight - 6, 14, 3, palette.getPrimary());
            }

            if (item.capeFile.exists()) {
                CapeRenderer.renderRoundedCapePreview(Soar.getInstance().getCapeManager().getLoadedCape(item.capeId), itemX + 5, itemY + 5, itemWidth - 10, itemHeight - 10, 8);
            }
        }

        int totalRows = (int) Math.ceil((double) capeItems.size() / itemsPerRow);
        float totalHeight = totalRows * (itemHeight + spacing);
        scrollHelper.setMaxScroll(totalHeight, height - 200);
    }

    @Override
    public void mousePressed(double mouseX, double mouseY, int button) {
        super.mousePressed(mouseX, mouseY, button);
        addButton.mousePressed(mouseX, mouseY, button);
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        super.mouseReleased(mouseX, mouseY, button);
        addButton.mouseReleased(mouseX, mouseY, button);

        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return;
        }

        double relativeMouseY = mouseY - scrollHelper.getValue();
        boolean isWinStyle = ModMenuSettings.getInstance().getUiStyleSetting().getOption().equals("win");

        if (isWinStyle) {
            handleWinMouseRelease(mouseX, relativeMouseY);
        } else {
            handleMd3MouseRelease(mouseX, relativeMouseY);
        }
    }

    private void handleWinMouseRelease(double mouseX, double mouseY) {
        float leftMargin = 32;
        float topMargin = 80;
        int columnCount = 4;
        float itemSpacing = 24;
        float itemWidth = (width - leftMargin * 2 - (columnCount - 1) * itemSpacing) / columnCount;
        float itemHeight = itemWidth * 1.5f;

        float currentX = x + leftMargin;
        float currentY = y + topMargin;

        for (int i = 0; i < capeItems.size(); i++) {
            if (MouseUtils.isInside(mouseX, mouseY, currentX, currentY, itemWidth, itemHeight)) {
                handleCapeItemClick(capeItems.get(i));
                return;
            }
            currentX += itemWidth + itemSpacing;
            if ((i + 1) % columnCount == 0) {
                currentX = x + leftMargin;
                currentY += itemHeight + itemSpacing;
            }
        }
    }

    private void handleMd3MouseRelease(double mouseX, double mouseY) {
        float startX = x + 32;
        float startY = y + 120;
        float itemWidth = 140;
        float itemHeight = 220;
        float spacing = 20;
        int itemsPerRow = 5;

        for (int i = 0; i < capeItems.size(); i++) {
            int row = i / itemsPerRow;
            int col = i % itemsPerRow;
            float itemX = startX + col * (itemWidth + spacing);
            float itemY = startY + row * (itemHeight + spacing);

            if (MouseUtils.isInside(mouseX, mouseY, itemX, itemY, itemWidth, itemHeight)) {
                handleCapeItemClick(capeItems.get(i));
                return;
            }
        }
    }

    private void handleCapeItemClick(CapeItem item) {
        CapeManager capeManager = Soar.getInstance().getCapeManager();
        if (item.capeId.equals(selectedCapeId)) {
            selectedCapeId = null;
            capeManager.clearSelectedCape();
        } else {
            selectedCapeId = item.capeId;
            capeManager.selectCape(item.capeId);
        }
    }

    private static class CapeItem {
        public final String capeId;
        public final File capeFile;
        public final SimpleAnimation xAnimation = new SimpleAnimation();
        public final SimpleAnimation yAnimation = new SimpleAnimation();
        public final SimpleAnimation focusAnimation = new SimpleAnimation();

        public CapeItem(String capeId, File capeFile) {
            this.capeId = capeId;
            this.capeFile = capeFile;
        }
    }
}
