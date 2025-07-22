package com.soarclient.gui.modmenu.pages;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.lwjgl.glfw.GLFW;

import com.soarclient.Soar;
import com.soarclient.animation.SimpleAnimation;
import com.soarclient.gui.api.SoarGui;
import com.soarclient.gui.api.page.Page;
import com.soarclient.gui.api.page.impl.RightLeftTransition;
import com.soarclient.management.color.api.ColorPalette;
import com.soarclient.management.cape.CapeRenderer;
import com.soarclient.skia.Skia;
import com.soarclient.skia.font.Icon;
import com.soarclient.ui.component.handler.impl.ButtonHandler;
import com.soarclient.ui.component.impl.IconButton;
import com.soarclient.utils.Multithreading;
import com.soarclient.utils.file.FileLocation;
import com.soarclient.utils.file.dialog.SoarFileDialog;
import com.soarclient.utils.mouse.MouseUtils;

import it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;

public class CapesPage extends Page {

    private List<CapeItem> capeItems = new ArrayList<>();
    private IconButton addButton;
    private String selectedCapeId = null;

    public CapesPage(SoarGui parent) {
        super(parent, "text.capes", Icon.STYLE, new RightLeftTransition(true));
    }

    @Override
    public void init() {
        super.init();

        addButton = new IconButton(Icon.ADD, x + width - 80, y + height - 80,
            IconButton.Size.LARGE, IconButton.Style.SECONDARY);
        addButton.setHandler(new ButtonHandler() {
            @Override
            public void onAction() {
                uploadCape();
            }
        });

        loadExistingCapes();
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

        ColorPalette palette = Soar.getInstance().getColorManager().getPalette();

        addButton.draw(mouseX, mouseY);

        float startX = x + 32;
        float startY = y + 120;
        float itemWidth = 140;
        float itemHeight = 180;
        float spacing = 30;
        int itemsPerRow = 5;

        float capeWidth = itemWidth - 10;
        float capeHeight = itemHeight - 10;
        float capeX = 5;
        float capeY = 5;

        mouseY = mouseY - scrollHelper.getValue();

        Skia.save();
        Skia.translate(0, scrollHelper.getValue());

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
            Skia.drawRoundedRect(itemX + capeX, itemY + capeY, capeWidth, capeHeight, 12, bgColor);

            if (isSelected) {
                Skia.drawOutline(itemX + capeX - 2, itemY + capeY - 2,
                    capeWidth + 4, capeHeight + 4, 14, 3, palette.getPrimary());
            }

            if (item.capeFile.exists()) {
                CapeRenderer.renderRoundedCapePreview(
                    Soar.getInstance().getCapeManager().getLoadedCape(item.capeId),
                    itemX + capeX, itemY + capeY, capeWidth, capeHeight, 8
                );
            }
        }

        int totalRows = (int) Math.ceil((double) capeItems.size() / itemsPerRow);
        float totalHeight = totalRows * (itemHeight + spacing);
        scrollHelper.setMaxScroll(totalHeight, height - 200);

        Skia.restore();
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

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            float startX = x + 32;
            float startY = y + 120 + scrollHelper.getValue();
            float itemWidth = 140;
            float itemHeight = 180;
            float spacing = 30;
            int itemsPerRow = 5;

            for (int i = 0; i < capeItems.size(); i++) {
                CapeItem item = capeItems.get(i);

                int row = i / itemsPerRow;
                int col = i % itemsPerRow;

                float itemX = startX + col * (itemWidth + spacing);
                float itemY = startY + row * (itemHeight + spacing);

                if (MouseUtils.isInside(mouseX, mouseY, itemX, itemY, itemWidth, itemHeight)) {
                    if (item.capeId.equals(selectedCapeId)) {
                        selectedCapeId = null;
                        Soar.getInstance().getCapeManager().clearSelectedCape();
                    } else {
                        selectedCapeId = item.capeId;
                        Soar.getInstance().getCapeManager().selectCape(item.capeId);
                    }
                    break;
                }
            }
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
