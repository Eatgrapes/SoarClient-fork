package com.soarclient.management.mod.impl.hud;

import java.awt.Color;
import java.io.File;
import java.util.Arrays;

import com.soarclient.Soar;
import com.soarclient.event.EventBus;
import com.soarclient.event.client.RenderSkiaEvent;
import com.soarclient.management.mod.api.hud.SimpleHUDMod;
import com.soarclient.management.mod.settings.impl.ComboSetting;
import com.soarclient.management.mod.settings.impl.BooleanSetting;
import com.soarclient.management.music.Music;
import com.soarclient.management.music.MusicManager;
import com.soarclient.management.music.lyrics.LyricsManager;
import com.soarclient.skia.Skia;
import com.soarclient.skia.font.Fonts;
import com.soarclient.skia.font.Icon;
import com.soarclient.utils.ColorUtils;
import com.soarclient.utils.TimerUtils;

import io.github.humbleui.skija.FilterTileMode;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.ImageFilter;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.types.Rect;

public class MusicInfoMod extends SimpleHUDMod {

    private final TimerUtils timer = new TimerUtils();
    private float mx, my, dx, dy;

    private final ComboSetting typeSetting = new ComboSetting("setting.type", "setting.type.description",
        Icon.FORMAT_LIST_BULLETED, this, Arrays.asList("setting.simple", "setting.normal", "setting.cover"),
        "setting.simple");

    private final BooleanSetting backgroundSetting = new BooleanSetting("setting.background",
        "setting.background.description", Icon.IMAGE, this, true) {
        @Override
        public boolean isVisible() {
            String type = typeSetting.getOption();
            return type.equals("setting.normal");
        }
    };

    private final BooleanSetting lyricsDisplaySetting = new BooleanSetting("setting.lyrics.display",
        "setting.lyrics.display.description", Icon.TEXT_FIELDS, this, false) {

        @Override
        public boolean isVisible() {
            String type = typeSetting.getOption();
            return type.equals("setting.normal") || type.equals("setting.cover");
        }
    };

    private final LyricsManager lyricsManager = new LyricsManager();

    public MusicInfoMod() {
        super("mod.musicinfo.name", "mod.musicinfo.description", Icon.MUSIC_NOTE);
        dx = 1;
        dy = 1;
    }

    public EventBus.EventListener<RenderSkiaEvent> onRenderSkia = event -> {

        String type = typeSetting.getOption();

        float width = 180;
        float height = 45;


        if (type.equals("setting.simple")) {
            this.draw();
        } else {
            this.begin();
            drawInfo(width, height);
            this.finish();
            position.setSize(width, height);
        }
    };

    private void drawInfo(float width, float height) {

        String type = typeSetting.getOption();

        MusicManager musicManager = Soar.getInstance().getMusicManager();
        Music m = musicManager.getCurrentMusic();
        float padding = 4.5F;
        float albumSize = height - (padding * 2);


        boolean cover = type.equals("setting.cover");
        Color textColor = cover ? Color.WHITE : this.getDesign().getTextColor();
        float coverSize = 256;

        if (backgroundSetting.isEnabled()) {
            this.drawBackground(getX(), getY(), width, height);
        }

        if (m != null && m.getAlbum() != null && cover) {
            Skia.save();
            Skia.clip(getX(), getY(), width, height, getRadius());
            drawBlurredImage(m.getAlbum(), getX() - mx, getY() - my, coverSize, coverSize, 20);
            Skia.restore();
        }

        if (m != null && m.getAlbum() != null) {
            Skia.drawRoundedImage(m.getAlbum(), getX() + padding, getY() + padding, albumSize, albumSize, 6);
        } else {
            Skia.drawRoundedRect(getX() + padding, getY() + padding, albumSize, albumSize, 6,
                ColorUtils.applyAlpha(textColor, 0.2F));
        }

        if (m != null) {

            float offsetX = (padding * 2) + albumSize;
            float maxTextWidth = width - offsetX - 12;

            String limitedTitle = Skia.getLimitText(m.getTitle(), Fonts.getRegular(9), maxTextWidth);
            String limitedArtist = Skia.getLimitText(m.getArtist(), Fonts.getRegular(6.5F), maxTextWidth);

            Skia.drawText(limitedTitle, getX() + offsetX, getY() + padding + 3F, textColor, Fonts.getRegular(9));
            Skia.drawText(limitedArtist, getX() + offsetX, getY() + padding + 12F,
                ColorUtils.applyAlpha(textColor, 0.8F), Fonts.getRegular(6.5F));

            if (lyricsDisplaySetting.isEnabled()) {
                float currentTime = musicManager.getCurrentTime();
                String currentLyric = lyricsManager.getCurrentLyric(m, currentTime);

                if (currentLyric != null && !currentLyric.isEmpty()) {
                    float lyricY = getY() + padding + 24F;
                    String limitedLyric = Skia.getLimitText(currentLyric, Fonts.getRegular(7), maxTextWidth);
                    Skia.drawText(limitedLyric, getX() + offsetX, lyricY,
                        ColorUtils.applyAlpha(textColor, 0.9F), Fonts.getRegular(7));
                }
            }
        }

        if (timer.delay(80)) {
            updatePosition(width, height, coverSize);
            timer.reset();
        }
    }

    private void drawBlurredImage(File file, float x, float y, float width, float height, float blurRadius) {

        Paint blurPaint = new Paint();
        blurPaint.setImageFilter(ImageFilter.makeBlur(blurRadius, blurRadius, FilterTileMode.REPEAT));

        Skia.drawImage(file, x, y, width, height);

        if (Skia.getImageHelper().load(file)) {
            Image image = Skia.getImageHelper().get(file.getName());
            if (image != null) {
                Skia.getCanvas().drawImageRect(image, Rect.makeWH(image.getWidth(), image.getHeight()),
                    Rect.makeXYWH(x, y, width, height), blurPaint, true);
            }
        }
    }

    private void updatePosition(float width, float height, float coverSize) {

        mx += dx;
        my += dy;

        if (mx <= 0 || mx + width >= coverSize) {
            dx = -dx;
            if (mx <= 0) {
                mx = 0;
            }
            if (mx + width >= coverSize) {
                mx = coverSize - width;
            }
        }
        if (my <= 0 || my + height >= coverSize) {
            dy = -dy;
            if (my <= 0) {
                my = 0;
            }
            if (my + height >= coverSize) {
                my = coverSize - height;
            }
        }
    }

    @Override
    public String getText() {

        MusicManager musicManager = Soar.getInstance().getMusicManager();

        if (musicManager.getCurrentMusic() != null && musicManager.isPlaying()) {
            return "Playing: " + musicManager.getCurrentMusic().getTitle();
        } else {
            return "Nothing is Playing";
        }
    }

    @Override
    public String getIcon() {
        return Icon.MUSIC_NOTE;
    }
}
