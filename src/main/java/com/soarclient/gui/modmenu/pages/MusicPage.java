package com.soarclient.gui.modmenu.pages;

import com.soarclient.Soar;
import com.soarclient.animation.SimpleAnimation;
import com.soarclient.event.EventBus;
import com.soarclient.event.client.MusicLibraryUpdatedEvent;
import com.soarclient.gui.api.SoarGui;
import com.soarclient.gui.api.page.Page;
import com.soarclient.gui.api.page.impl.RightLeftTransition;
import com.soarclient.gui.modmenu.component.MusicControlBar;
import com.soarclient.management.color.api.ColorPalette;
import com.soarclient.management.mod.impl.settings.ModMenuSettings;
import com.soarclient.management.music.Music;
import com.soarclient.management.music.MusicManager;
import com.soarclient.skia.Skia;
import com.soarclient.skia.font.Fonts;
import com.soarclient.skia.font.Icon;
import com.soarclient.utils.ColorUtils;
import com.soarclient.utils.SearchUtils;
import com.soarclient.utils.mouse.MouseUtils;
import io.github.humbleui.skija.*;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MusicPage extends Page {

    private final SimpleAnimation controlBarAnimation = new SimpleAnimation();
    private MusicControlBar controlBar;
    private final List<Item> items = new ArrayList<>();

    final EventBus.EventListener<MusicLibraryUpdatedEvent> onMusicLibraryUpdated = event -> refreshMusicList();

    public MusicPage(SoarGui parent) {
        super(parent, "text.music", Icon.MUSIC_NOTE, new RightLeftTransition(true));
    }

    @Override
    public void init() {
        super.init();
        refreshMusicList();
        controlBar = new MusicControlBar(x + 22, y + height - 60 - 18, width - 44);
        EventBus.getInstance().register(onMusicLibraryUpdated);
    }

    @Override
    public void onClosed() {
        super.onClosed();
        EventBus.getInstance().unregister(onMusicLibraryUpdated);
    }

    public void refreshMusicList() {
        items.clear();
        for (Music m : Soar.getInstance().getMusicManager().getMusics()) {
            items.add(new Item(m));
        }
        for (Item i : items) {
            i.xAnimation.setFirstTick(true);
            i.yAnimation.setFirstTick(true);
        }
    }

    @Override
    public void draw(double mouseX, double mouseY) {
        super.draw(mouseX, mouseY);

        boolean isWinStyle = ModMenuSettings.getInstance().getUiStyleSetting().getOption().equals("win");

        List<Item> filteredItems = items.stream()
            .filter(i -> searchBar.getText().isEmpty() || SearchUtils.isSimilar(i.music.getTitle() + " " + i.music.getArtist(), searchBar.getText()))
            .toList();

        double relativeMouseY = mouseY - scrollHelper.getValue();
        Skia.save();
        Skia.translate(0, scrollHelper.getValue());

        if (isWinStyle) {
            drawWinStyle(mouseX, relativeMouseY, filteredItems);
        } else {
            drawMd3Style(mouseX, relativeMouseY, filteredItems);
        }

        Skia.restore();

        // Draw control bar separately to keep it on top
        Skia.save();
        controlBarAnimation.onTick(MouseUtils.isInside(mouseX, mouseY, controlBar.getX(), controlBar.getY(), controlBar.getWidth(), controlBar.getHeight()) ? 1 : 0, 12);
        Skia.translate(0, 100 - (controlBarAnimation.getValue() * 100));
        controlBar.draw(mouseX, mouseY);
        Skia.restore();
    }

    private void drawWinStyle(double mouseX, double mouseY, List<Item> filteredItems) {
        ColorPalette palette = Soar.getInstance().getColorManager().getPalette();
        float leftMargin = 32;
        float topMargin = 80; // Increased to prevent overlap with search bar
        int columnCount = 4; // Set to 4 columns as requested
        float itemSpacing = 24;
        float itemSize = (width - leftMargin * 2 - (columnCount - 1) * itemSpacing) / columnCount;

        float currentX = x + leftMargin;
        float currentY = y + topMargin;

        for (int i = 0; i < filteredItems.size(); i++) {
            Item item = filteredItems.get(i);
            Music m = item.music;

            item.xAnimation.onTick(currentX, 14);
            item.yAnimation.onTick(currentY, 14);
            float itemX = item.xAnimation.getValue();
            float itemY = item.yAnimation.getValue();

            boolean isHovered = MouseUtils.isInside(mouseX, mouseY, itemX, itemY, itemSize, itemSize);
            item.focusAnimation.onTick(isHovered ? 1 : 0, 10);

            // Album Art
            if (m.getAlbum() != null) {
                Skia.drawRoundedImage(m.getAlbum(), itemX, itemY, itemSize, itemSize, 8);
            } else {
                Skia.drawRoundedRect(itemX, itemY, itemSize, itemSize, 8, palette.getSurfaceContainerHigh());
            }

            // Hover & Play/Pause Icon
            if (isHovered) {
                Skia.drawRoundedRect(itemX, itemY, itemSize, itemSize, 8, new Color(0, 0, 0, 40));
                String icon = Soar.getInstance().getMusicManager().getCurrentMusic() == m && Soar.getInstance().getMusicManager().isPlaying() ? Icon.PAUSE : Icon.PLAY_ARROW;
                Skia.drawFullCenteredText(icon, itemX + itemSize / 2, itemY + itemSize / 2, ColorUtils.applyAlpha(Color.WHITE, item.focusAnimation.getValue()), Fonts.getIconFill(50));
            }

            // Text
            String limitedTitle = Skia.getLimitText(m.getTitle(), Fonts.getRegular(14), itemSize);
            String limitedArtist = Skia.getLimitText(m.getArtist(), Fonts.getRegular(12), itemSize);
            Skia.drawText(limitedTitle, itemX, itemY + itemSize + 8, palette.getOnSurface(), Fonts.getRegular(14));
            Skia.drawText(limitedArtist, itemX, itemY + itemSize + 24, palette.getOnSurfaceVariant(), Fonts.getRegular(12));

            currentX += itemSize + itemSpacing;
            if ((i + 1) % columnCount == 0) {
                currentX = x + leftMargin;
                currentY += itemSize + 48;
            }
        }
        float contentHeight = (currentY + itemSize + 48) - (y + topMargin);
        scrollHelper.setMaxScroll(contentHeight, height - topMargin - controlBar.getHeight() - 20);
    }

    private void drawMd3Style(double mouseX, double mouseY, List<Item> filteredItems) {
        ColorPalette palette = Soar.getInstance().getColorManager().getPalette();
        float initialOffsetY = 96;
        int index = 0;
        float offsetX = 28;
        float offsetY = initialOffsetY;

        for (Item i : filteredItems) {
            Music m = i.music;
            float itemX = x + offsetX;
            float itemY = y + offsetY;

            i.xAnimation.onTick(itemX, 14);
            i.yAnimation.onTick(itemY, 14);
            i.focusAnimation.onTick(MouseUtils.isInside(mouseX, mouseY, itemX, itemY, 174, 174) ? 1 : 0, 10);

            itemX = i.xAnimation.getValue();
            itemY = i.yAnimation.getValue();

            if (m.getAlbum() != null) {
                drawMd3AlbumArt(m.getAlbum(), itemX, itemY, (Math.abs(i.focusAnimation.getValue()) + 0.001F) * 6);
            } else {
                Skia.drawRoundedRect(itemX, itemY, 174, 174, 26, palette.getSurfaceContainerHigh());
            }

            String limitedTitle = Skia.getLimitText(m.getTitle(), Fonts.getRegular(15), 174);
            String limitedArtist = Skia.getLimitText(m.getArtist(), Fonts.getRegular(12), 174);
            Skia.drawText(limitedTitle, itemX, itemY + 174 + 6, palette.getOnSurface(), Fonts.getRegular(15));
            Skia.drawText(limitedArtist, itemX, itemY + 174 + 6 + 15, palette.getOnSurfaceVariant(), Fonts.getRegular(12));

            String icon = Soar.getInstance().getMusicManager().getCurrentMusic() == m && Soar.getInstance().getMusicManager().isPlaying() ? Icon.PAUSE : Icon.PLAY_ARROW;
            Skia.save();
            Skia.translate(0, 15 - (i.focusAnimation.getValue() * 15));
            Skia.drawFullCenteredText(icon, itemX + 87, itemY + 87, ColorUtils.applyAlpha(Color.WHITE, i.focusAnimation.getValue()), Fonts.getIconFill(64));
            Skia.restore();

            offsetX += 174 + 32;
            index++;
            if (index % 4 == 0) {
                offsetX = 28;
                offsetY += 206 + 23;
            }
        }
        scrollHelper.setMaxScroll(206, 23, index, 4, height - 96);
    }

    @Override
    public void mousePressed(double mouseX, double mouseY, int button) {
        super.mousePressed(mouseX, mouseY, button);
        controlBar.mousePressed(mouseX, mouseY, button);
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        super.mouseReleased(mouseX, mouseY, button);
        if (MouseUtils.isInside(mouseX, mouseY, controlBar.getX(), controlBar.getY(), controlBar.getWidth(), controlBar.getHeight())) {
            controlBar.mouseReleased(mouseX, mouseY, button);
            return;
        }

        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return;
        }

        double relativeMouseY = mouseY - scrollHelper.getValue();
        boolean isWinStyle = ModMenuSettings.getInstance().getUiStyleSetting().getOption().equals("win");

        List<Item> filteredItems = items.stream()
            .filter(i -> searchBar.getText().isEmpty() || SearchUtils.isSimilar(i.music.getTitle() + " " + i.music.getArtist(), searchBar.getText()))
            .toList();

        if (isWinStyle) {
            float leftMargin = 32;
            float topMargin = 80;
            int columnCount = 4;
            float itemSpacing = 24;
            float itemSize = (width - leftMargin * 2 - (columnCount - 1) * itemSpacing) / columnCount;
            float rowHeight = itemSize + 48;

            float currentX = x + leftMargin;
            float currentY = y + topMargin;

            for (int i = 0; i < filteredItems.size(); i++) {
                if (MouseUtils.isInside(mouseX, relativeMouseY, currentX, currentY, itemSize, itemSize)) {
                    handleMusicItemClick(filteredItems.get(i));
                    return;
                }
                currentX += itemSize + itemSpacing;
                if ((i + 1) % columnCount == 0) {
                    currentX = x + leftMargin;
                    currentY += rowHeight;
                }
            }
        } else { // MD3 Style
            float leftMargin = 28;
            float topMargin = 96;
            int columnCount = 4;
            float itemWidth = 174;
            float itemSpacing = 32;
            float rowHeight = 206 + 23;

            float currentX = x + leftMargin;
            float currentY = y + topMargin;

            for (int i = 0; i < filteredItems.size(); i++) {
                if (MouseUtils.isInside(mouseX, relativeMouseY, currentX, currentY, itemWidth, 174)) {
                    handleMusicItemClick(filteredItems.get(i));
                    return;
                }
                currentX += itemWidth + itemSpacing;
                if ((i + 1) % columnCount == 0) {
                    currentX = x + leftMargin;
                    currentY += rowHeight;
                }
            }
        }
    }

    private void handleMusicItemClick(Item item) {
        MusicManager musicManager = Soar.getInstance().getMusicManager();
        if (musicManager.getCurrentMusic() != item.music) {
            musicManager.stop();
            musicManager.setCurrentMusic(item.music);
            musicManager.play();
        } else {
            musicManager.switchPlayBack();
        }
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        super.keyPressed(keyCode, scanCode, modifiers);
        controlBar.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void charTyped(char chr, int modifiers) {
        super.charTyped(chr, modifiers);
        controlBar.charTyped(chr, modifiers);
    }

    private void drawMd3AlbumArt(File file, float x, float y, float blurRadius) {
        float width = 174, height = 174, cornerRadius = 26;
        try (Path path = new Path()) {
            path.addRRect(RRect.makeXYWH(x, y, width, height, cornerRadius));
            try (Paint blurPaint = new Paint()) {
                blurPaint.setImageFilter(ImageFilter.makeBlur(blurRadius, blurRadius, FilterTileMode.CLAMP));
                Skia.save();
                Skia.getCanvas().clipPath(path, ClipMode.INTERSECT, true);
                if (Skia.getImageHelper().load(file)) {
                    Image image = Skia.getImageHelper().get(file.getName());
                    if (image != null) {
                        try (Paint paint = new Paint()) {
                            Skia.getCanvas().drawImageRect(image, Rect.makeWH(image.getWidth(), image.getHeight()), Rect.makeXYWH(x, y, width, height), paint);
                            Skia.getCanvas().drawImageRect(image, Rect.makeWH(image.getWidth(), image.getHeight()), Rect.makeXYWH(x, y, width, height), blurPaint, true);
                        }
                    }
                }
                Skia.restore();
            }
        }
    }

    private static class Item {
        private final Music music;
        private final SimpleAnimation xAnimation = new SimpleAnimation();
        private final SimpleAnimation yAnimation = new SimpleAnimation();
        private final SimpleAnimation focusAnimation = new SimpleAnimation();

        private Item(Music music) {
            this.music = music;
        }
    }
}
