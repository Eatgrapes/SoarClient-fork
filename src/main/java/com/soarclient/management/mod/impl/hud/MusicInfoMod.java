package com.soarclient.management.mod.impl.hud;

import java.awt.Color;
import java.io.File;
import java.util.Arrays;

import com.soarclient.Soar;
import com.soarclient.event.EventBus;
import com.soarclient.event.client.ClientTickEvent;
import com.soarclient.event.client.RenderSkiaEvent;
import com.soarclient.gui.edithud.api.HUDCore;
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

    private float animatedWidth, animatedHeight;
    private float targetWidth, targetHeight;

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

    private String currentLyric = "";
    private String previousLyric = "";
    private long lyricChangeTime = 0;
    private final int lyricAnimationDuration = 200;

    public MusicInfoMod() {
        super("mod.musicinfo.name", "mod.musicinfo.description", Icon.MUSIC_NOTE);
        dx = 1;
        dy = 1;
        this.animatedWidth = 0;
        this.animatedHeight = 0;
        this.targetWidth = 0;
        this.targetHeight = 0;
    }

    public final EventBus.EventListener<ClientTickEvent> onClientTick = event -> {
        String type = typeSetting.getOption();
        if (type.equals("setting.simple")) {
            targetWidth = position.getWidth();
            targetHeight = position.getHeight();
            return;
        }

        Music m = Soar.getInstance().getMusicManager().getCurrentMusic();

        if (lyricsDisplaySetting.isEnabled()) {
            String newLyric = "";
            if (m != null) {
                newLyric = lyricsManager.getCurrentLyric(m, Soar.getInstance().getMusicManager().getCurrentTime());
            }
            if (newLyric == null) {
                newLyric = "";
            }

            if (!newLyric.equals(this.currentLyric)) {
                this.previousLyric = this.currentLyric;
                this.currentLyric = newLyric;
                this.lyricChangeTime = System.currentTimeMillis();
            }
        }

        if (m != null || HUDCore.isEditing) {
            this.targetHeight = 45;
            this.targetWidth = calculateAdaptiveWidth();
        } else {
            this.targetWidth = 0;
            this.targetHeight = 0;
            if (!this.currentLyric.isEmpty()) {
                this.previousLyric = this.currentLyric;
                this.currentLyric = "";
                this.lyricChangeTime = System.currentTimeMillis();
            }
        }
    };

    public EventBus.EventListener<RenderSkiaEvent> onRenderSkia = event -> {
        String type = typeSetting.getOption();

        if (type.equals("setting.simple")) {
            this.draw();
            animatedWidth = position.getWidth();
            animatedHeight = position.getHeight();
            return;
        }

        float animationSpeed = 0.15f;
        float diffW = targetWidth - animatedWidth;
        float diffH = targetHeight - animatedHeight;

        if (Math.abs(diffW) > 0.5f) {
            animatedWidth += diffW * animationSpeed;
        } else {
            animatedWidth = targetWidth;
        }
        if (Math.abs(diffH) > 0.5f) {
            animatedHeight += diffH * animationSpeed;
        } else {
            animatedHeight = targetHeight;
        }

        if (animatedWidth < 1) {
            if (position.getWidth() != 0 || position.getHeight() != 0) {
                position.setSize(0, 0);
            }
            return;
        }

        this.begin();
        drawInfo(animatedWidth, animatedHeight);
        this.finish();
        position.setSize(animatedWidth, animatedHeight);
    };

    private float calculateAdaptiveWidth() {
        MusicManager musicManager = Soar.getInstance().getMusicManager();
        Music m = musicManager.getCurrentMusic();

        boolean isDummyMode = HUDCore.isEditing && m == null;

        if (isDummyMode) {
            return 180;
        }

        if (m == null) {
            return 0;
        }

        float maxTextWidth = 0;

        Rect titleBounds = Skia.getTextBounds(m.getTitle(), Fonts.getRegular(9));
        maxTextWidth = Math.max(maxTextWidth, titleBounds.getWidth());

        Rect artistBounds = Skia.getTextBounds(m.getArtist(), Fonts.getRegular(6.5F));
        maxTextWidth = Math.max(maxTextWidth, artistBounds.getWidth());

        if (lyricsDisplaySetting.isEnabled()) {
            if (currentLyric != null && !currentLyric.isEmpty()) {
                Rect lyricBounds = Skia.getTextBounds(currentLyric, Fonts.getRegular(7));
                maxTextWidth = Math.max(maxTextWidth, lyricBounds.getWidth());
            }
        }

        float padding = 4.5F;
        float albumSize = 45 - (padding * 2);
        float sidePaddings = 12;
        float totalWidth = padding + albumSize + padding + maxTextWidth + sidePaddings;

        return Math.max(180, totalWidth);
    }

    private void drawInfo(float width, float height) {
        String type = typeSetting.getOption();
        MusicManager musicManager = Soar.getInstance().getMusicManager();
        Music m = musicManager.getCurrentMusic();

        float padding = 4.5F;
        float albumSize = height - (padding * 2);

        boolean cover = type.equals("setting.cover");
        Color textColor = cover ? Color.WHITE : this.getDesign().getTextColor();

        float coverSize = Math.max(width, height) * 1.2f;

        if (backgroundSetting.isEnabled() && !cover) {
            this.drawBackground(getX(), getY(), width, height);
        }

        float animationProgress = targetWidth > 0 ? width / targetWidth : 0;
        if (animationProgress > 0.85f) {

            boolean isDummyMode = HUDCore.isEditing && m == null;

            if (isDummyMode) {
                Skia.drawRoundedRect(getX() + padding, getY() + padding, albumSize, albumSize, 6,
                    ColorUtils.applyAlpha(textColor, 0.2F));

            } else if (m != null) {
                if (cover && m.getAlbum() != null) {
                    Skia.save();
                    Skia.clip(getX(), getY(), width, height, getRadius());
                    // The blurred image is now drawn with the corrected dynamic size.
                    drawBlurredImage(m.getAlbum(), getX() - mx, getY() - my, coverSize, coverSize, 20);
                    Skia.restore();
                }

                if (m.getAlbum() != null) {
                    Skia.drawRoundedImage(m.getAlbum(), getX() + padding, getY() + padding, albumSize, albumSize, 6);
                } else {
                    Skia.drawRoundedRect(getX() + padding, getY() + padding, albumSize, albumSize, 6,
                        ColorUtils.applyAlpha(textColor, 0.2F));
                }

                float offsetX = (padding * 2) + albumSize;

                Skia.drawText(m.getTitle(), getX() + offsetX, getY() + padding + 3F, textColor, Fonts.getRegular(9));
                Skia.drawText(m.getArtist(), getX() + offsetX, getY() + padding + 12F,
                    ColorUtils.applyAlpha(textColor, 0.8F), Fonts.getRegular(6.5F));

                if (lyricsDisplaySetting.isEnabled()) {
                    float lyricY = getY() + padding + 24F;
                    float lyricX = getX() + offsetX;
                    float lyricAnimationHeight = 10.0f;

                    long timeSinceChange = System.currentTimeMillis() - lyricChangeTime;
                    float progress = Math.min(1.0f, (float) timeSinceChange / lyricAnimationDuration);

                    progress = 1.0f - (float) Math.pow(1.0f - progress, 3.0f);

                    if (previousLyric != null && !previousLyric.isEmpty()) {
                        float yOffset = -lyricAnimationHeight * progress;
                        float alpha = 1.0f - progress;
                        if(alpha > 0.01f) {
                            Skia.drawText(previousLyric, lyricX, lyricY + yOffset,
                                ColorUtils.applyAlpha(textColor, 0.9F * alpha), Fonts.getRegular(7));
                        }
                    }

                    if (currentLyric != null && !currentLyric.isEmpty()) {
                        float yOffset = lyricAnimationHeight * (1.0f - progress);
                        float alpha = progress;
                        if(alpha > 0.01f) {
                            Skia.drawText(currentLyric, lyricX, lyricY + yOffset,
                                ColorUtils.applyAlpha(textColor, 0.9F * alpha), Fonts.getRegular(7));
                        }
                    }
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
            if (mx <= 0) mx = 0;
            if (mx + width >= coverSize) mx = coverSize - width;
        }
        if (my <= 0 || my + height >= coverSize) {
            dy = -dy;
            if (my <= 0) my = 0;
            if (my + height >= coverSize) my = coverSize - height;
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
