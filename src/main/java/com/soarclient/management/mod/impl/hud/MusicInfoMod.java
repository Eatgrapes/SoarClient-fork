package com.soarclient.management.mod.impl.hud;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.soarclient.Soar;
import com.soarclient.event.EventBus;
import com.soarclient.event.client.ClientTickEvent;
import com.soarclient.event.client.RenderSkiaEvent;
import com.soarclient.gui.edithud.api.HUDCore;
import com.soarclient.management.mod.api.hud.SimpleHUDMod;
import com.soarclient.management.mod.settings.impl.BooleanSetting;
import com.soarclient.management.mod.settings.impl.ComboSetting;
import com.soarclient.management.mod.settings.impl.NumberSetting;
import com.soarclient.management.music.Music;
import com.soarclient.management.music.MusicManager;
import com.soarclient.management.music.MusicPlayer;
import com.soarclient.management.music.lyrics.LyricsManager;
import com.soarclient.skia.Skia;
import com.soarclient.skia.font.Fonts;
import com.soarclient.skia.font.Icon;
import com.soarclient.utils.ColorUtils;
import com.soarclient.utils.TimerUtils;

import io.github.humbleui.skija.Bitmap;
import io.github.humbleui.skija.FilterTileMode;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.ImageFilter;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.MaskFilter;
import io.github.humbleui.skija.FilterBlurMode;
import io.github.humbleui.types.Rect;

public class MusicInfoMod extends SimpleHUDMod {

    private final TimerUtils timer = new TimerUtils();
    private float mx, my;
    private final long startTime = System.currentTimeMillis();

    private float animatedWidth, animatedHeight;
    private float targetWidth, targetHeight;

    private float animatedBeatScale = 1.0f;
    private long lastFrameTime = 0;

    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();

    private Bitmap albumBitmap = null;
    private String currentAlbumPath = "";

    private boolean beatActive = false;

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

    private final BooleanSetting coverAnimationSetting = new BooleanSetting("setting.cover.animation", "setting.cover.animation.description", Icon.MOVIE, this, true) {
        @Override
        public boolean isVisible() {
            String type = typeSetting.getOption();
            return type.equals("setting.normal") || type.equals("setting.cover");
        }
    };

    private final BooleanSetting glowEffectSetting = new BooleanSetting("setting.glow", "setting.glow.description", Icon.LIGHTBULB_OUTLINE, this, false);

    private final NumberSetting glowIntensitySetting = new NumberSetting("setting.glow.intensity", "setting.glow.intensity.description", Icon.ARROW_UPWARD, this, 10, 1, 20, 1) {
        @Override
        public boolean isVisible() {
            return glowEffectSetting.isEnabled();
        }
    };

    private final NumberSetting glowRangeSetting = new NumberSetting("setting.glow.range", "setting.glow.range.description", Icon.EXPAND, this, 4, 1, 20, 1) {
        @Override
        public boolean isVisible() {
            return glowEffectSetting.isEnabled();
        }
    };


    private final LyricsManager lyricsManager = new LyricsManager();

    private String currentLyric = "";
    private String previousLyric = "";
    private long lyricChangeTime = 0;
    private static final int LYRIC_ANIMATION_DURATION = 200;

    private static final float DEFAULT_BLUR_RADIUS = 20.0f;
    private static final int DEFAULT_PARTICLE_AMOUNT = 40;

    public MusicInfoMod() {
        super("mod.musicinfo.name", "mod.musicinfo.description", Icon.MUSIC_NOTE);
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

    private void drawTextWithGlow(String text, float x, float y, Color color, Font font) {
        if (text == null || text.isEmpty()) {
            return;
        }

        if (!glowEffectSetting.isEnabled() || glowRangeSetting.getValue() <= 0) {
            Skia.drawText(text, x, y, color, font);
            return;
        }

        int intensity = Math.max(1, (int) glowIntensitySetting.getValue());
        float range = Math.max(0.5f, glowRangeSetting.getValue());

        float sigma = range;
        float normIntensity = Math.min(1.0f, intensity / 20.0f);

        float baseAlpha = 0.45f + 0.55f * normIntensity;
        int alpha255 = (int) Math.max(0, Math.min(255, baseAlpha * 255f));

        float baselineY = y - font.getMetrics().getAscent();

        try (Paint paint = new Paint()) {
            paint.setAntiAlias(true);
            paint.setColor(ColorUtils.applyAlpha(color, 1.0f).getRGB());

            try (MaskFilter mf = MaskFilter.makeBlur(FilterBlurMode.NORMAL, sigma * 0.6f)) {
                paint.setMaskFilter(mf);
                paint.setAlpha(alpha255);
                Skia.getCanvas().drawString(text, x - font.measureText(text).getLeft(), baselineY, font, paint);
            } catch (Throwable ignore) {
            }

            try (MaskFilter mf = MaskFilter.makeBlur(FilterBlurMode.NORMAL, sigma)) {
                paint.setMaskFilter(mf);
                paint.setAlpha((int) (alpha255 * 0.65f));
                Skia.getCanvas().drawString(text, x - font.measureText(text).getLeft(), baselineY, font, paint);
            } catch (Throwable ignore) {
            }

            try (MaskFilter mf = MaskFilter.makeBlur(FilterBlurMode.NORMAL, sigma * 1.6f)) {
                paint.setMaskFilter(mf);
                paint.setAlpha((int) (alpha255 * 0.35f));
                Skia.getCanvas().drawString(text, x - font.measureText(text).getLeft(), baselineY, font, paint);
            } catch (Throwable ignore) {
            }

            paint.setMaskFilter(null);
            paint.setAlpha(255);
            paint.setColor(ColorUtils.applyAlpha(color, 1.0f).getRGB());
            Skia.getCanvas().drawString(text, x - font.measureText(text).getLeft(), baselineY, font, paint);
        } catch (Throwable ex) {
            Skia.drawText(text, x, y, color, font);
        }
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

        updateAndDrawParticles();

        if (m != null && m.getAlbum() != null) {
            String albumPath = m.getAlbum().getAbsolutePath();
            if (!albumPath.equals(currentAlbumPath)) {
                currentAlbumPath = albumPath;
                if (Skia.getImageHelper().load(m.getAlbum())) {
                    Image image = Skia.getImageHelper().get(m.getAlbum().getName());
                    if (image != null) {
                        albumBitmap = new Bitmap();
                        albumBitmap.allocPixels(image.getImageInfo());
                        image.readPixels(albumBitmap, 0, 0);
                    } else {
                        albumBitmap = null;
                    }
                } else {
                    albumBitmap = null;
                }
            }
        } else {
            albumBitmap = null;
            currentAlbumPath = "";
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
                    drawBlurredImage(m.getAlbum(), getX() - mx, getY() - my, coverSize, coverSize);
                    Skia.restore();
                }

                if (m.getAlbum() != null) {

                    float targetBeatScale = 1.0f;

                    if (coverAnimationSetting.isEnabled() && musicManager.isPlaying()) {
                        float[] spectrum = MusicPlayer.VISUALIZER;
                        if (spectrum != null && spectrum.length > 0) {

                            float energy = 0;
                            int bandsToSample = Math.max(1, spectrum.length / 4);
                            for (int i = 0; i < bandsToSample; i++) {
                                energy += spectrum[i];
                            }

                            float averageBassMagnitude = energy / bandsToSample;
                            float sensitivity = 0.006f;
                            float maxMagnitude = 0.5f;

                            float dynamicPulseMagnitude = averageBassMagnitude * sensitivity;
                            dynamicPulseMagnitude = Math.min(dynamicPulseMagnitude, maxMagnitude);

                            targetBeatScale = 1.0f + dynamicPulseMagnitude;
                        }
                    }

                    long currentTime = System.currentTimeMillis();
                    if (lastFrameTime == 0) {
                        lastFrameTime = currentTime;
                    }
                    float deltaTime = (currentTime - lastFrameTime) / 1000.0f;
                    lastFrameTime = currentTime;

                    float smoothingFactor = 0.1f;

                    animatedBeatScale = animatedBeatScale + (targetBeatScale - animatedBeatScale) * (1.0f - (float)Math.pow(smoothingFactor, deltaTime));

                    float beatTriggerThreshold = 0.1f;

                    if (coverAnimationSetting.isEnabled() && musicManager.isPlaying()) {
                        if (!beatActive && targetBeatScale > animatedBeatScale + beatTriggerThreshold) {
                            spawnParticles(getX() + padding + albumSize / 2.0f, getY() + padding + albumSize / 2.0f, albumBitmap);
                            beatActive = true;
                        } else if (beatActive && targetBeatScale <= animatedBeatScale) {
                            beatActive = false;
                        }
                    } else {
                        beatActive = false;
                    }

                    float animatedAlbumSize = albumSize * animatedBeatScale;
                    float sizeOffset = (animatedAlbumSize - albumSize) / 2.0f;

                    Skia.drawRoundedImage(m.getAlbum(), getX() + padding - sizeOffset, getY() + padding - sizeOffset, animatedAlbumSize, animatedAlbumSize, 6 * animatedBeatScale);

                } else {
                    Skia.drawRoundedRect(getX() + padding, getY() + padding, albumSize, albumSize, 6,
                        ColorUtils.applyAlpha(textColor, 0.2F));
                }

                float offsetX = (padding * 2) + albumSize;

                drawTextWithGlow(m.getTitle(), getX() + offsetX, getY() + padding + 3F, textColor, Fonts.getRegular(9));
                drawTextWithGlow(m.getArtist(), getX() + offsetX, getY() + padding + 12F, ColorUtils.applyAlpha(textColor, 0.8F), Fonts.getRegular(6.5F));

                if (lyricsDisplaySetting.isEnabled()) {
                    float lyricY = getY() + padding + 24F;
                    float lyricX = getX() + offsetX;
                    float lyricAnimationHeight = 10.0f;

                    long timeSinceChange = System.currentTimeMillis() - lyricChangeTime;
                    float progress = Math.min(1.0f, (float) timeSinceChange / LYRIC_ANIMATION_DURATION);

                    progress = 1.0f - (float) Math.pow(1.0f - progress, 3.0f);

                    if (previousLyric != null && !previousLyric.isEmpty()) {
                        float yOffset = -lyricAnimationHeight * progress;
                        float alpha = 1.0f - progress;
                        if(alpha > 0.01f) {
                            drawTextWithGlow(previousLyric, lyricX, lyricY + yOffset, ColorUtils.applyAlpha(textColor, 0.9F * alpha), Fonts.getRegular(7));
                        }
                    }

                    if (currentLyric != null && !currentLyric.isEmpty()) {
                        float yOffset = lyricAnimationHeight * (1.0f - progress);
                        if(progress > 0.01f) {
                            drawTextWithGlow(currentLyric, lyricX, lyricY + yOffset, ColorUtils.applyAlpha(textColor, 0.9F * progress), Fonts.getRegular(7));
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

    private void drawBlurredImage(File file, float x, float y, float width, float height) {
        Paint blurPaint = new Paint();
        blurPaint.setImageFilter(ImageFilter.makeBlur(DEFAULT_BLUR_RADIUS, DEFAULT_BLUR_RADIUS, FilterTileMode.REPEAT));
        if (Skia.getImageHelper().load(file)) {
            Image image = Skia.getImageHelper().get(file.getName());
            if (image != null) {
                Skia.getCanvas().drawImageRect(image, Rect.makeWH(image.getWidth(), image.getHeight()),
                    Rect.makeXYWH(x, y, width, height), blurPaint, true);
            }
        }
    }

    private void updatePosition(float width, float height, float coverSize) {
        long currentTime = System.currentTimeMillis();
        float elapsedTime = (currentTime - startTime) / 1000.0f;

        float maxOffsetX = coverSize - width;
        float maxOffsetY = coverSize - height;

        float frequencyX = 0.1f;
        float frequencyY = 0.08f;

        mx = (float) ((Math.sin(elapsedTime * frequencyX) + 1) / 2.0 * maxOffsetX);
        my = (float) ((Math.cos(elapsedTime * frequencyY) + 1) / 2.0 * maxOffsetY);

        mx = Math.max(0, Math.min(mx, maxOffsetX));
        my = Math.max(0, Math.min(my, maxOffsetY));
    }

    private void spawnParticles(float x, float y, Bitmap albumBitmap) {
        for (int i = 0; i < DEFAULT_PARTICLE_AMOUNT; i++) {
            particles.add(new Particle(x, y, random, albumBitmap));
        }
    }

    private void updateAndDrawParticles() {
        Iterator<Particle> iterator = particles.iterator();
        while (iterator.hasNext()) {
            Particle p = iterator.next();
            p.update();
            if (p.isDead()) {
                iterator.remove();
            } else {
                p.draw();
            }
        }
    }

    private static class Particle {
        private float x, y;
        private float vx, vy;
        private float alpha;
        private final float size;
        private static final float GRAVITY = 0.04f;
        private static final float FRICTION = 0.99f;
        private final Color color;

        Particle(float x, float y, Random random, Bitmap albumBitmap) {
            this.x = x;
            this.y = y;
            double angle = random.nextDouble() * 2 * Math.PI;
            float speed = 1.0f + random.nextFloat() * 1.5f;
            this.vx = (float) (Math.cos(angle) * speed);
            this.vy = (float) (Math.sin(angle) * speed);
            this.alpha = 1.0f;
            this.size = 1.0f + random.nextFloat() * 1.5f;

            if (albumBitmap != null && !albumBitmap.isEmpty()) {
                int randomX = random.nextInt(albumBitmap.getWidth());
                int randomY = random.nextInt(albumBitmap.getHeight());
                this.color = new Color(albumBitmap.getColor(randomX, randomY), true);
            } else {
                this.color = Color.WHITE;
            }
        }

        void update() {
            this.x += this.vx;
            this.y += this.vy;
            this.vy += GRAVITY;
            this.vx *= FRICTION;
            this.vy *= FRICTION;
            this.alpha -= 0.02f;
        }

        void draw() {
            Color particleColor = ColorUtils.applyAlpha(this.color, this.alpha);
            Skia.drawCircle(this.x, this.y, this.size, particleColor);
        }

        boolean isDead() {
            return this.alpha <= 0;
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
