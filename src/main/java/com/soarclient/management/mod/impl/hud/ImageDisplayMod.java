package com.soarclient.management.mod.impl.hud;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.soarclient.Soar;
import com.soarclient.event.EventBus;
import com.soarclient.event.client.RenderSkiaEvent;
import com.soarclient.management.mod.api.hud.HUDMod;
import com.soarclient.management.mod.settings.impl.BooleanSetting;
import com.soarclient.management.mod.settings.impl.FileSetting;
import com.soarclient.management.mod.settings.impl.NumberSetting;
import com.soarclient.skia.Skia;
import com.soarclient.skia.font.Icon;

public class ImageDisplayMod extends HUDMod {

    private NumberSetting radiusSetting = new NumberSetting("setting.radius", "setting.radius.description",
        Icon.ROUNDED_CORNER, this, 6, 2, 64, 1);
    private NumberSetting alphaSetting = new NumberSetting("setting.alpha", "setting.alpha.description",
        Icon.OPACITY, this, 1.0F, 0.0F, 1.0F, 0.1F);
    private BooleanSetting shadowSetting = new BooleanSetting("setting.shadow", "setting.shadow.description",
        Icon.SHADOW, this, false);
    private FileSetting imageSetting = new FileSetting("setting.image", "setting.image.description",
        Icon.IMAGE, this, null, "png", "jpg", "jpeg", "gif", "bmp");

    private BufferedImage image;
    private File prevImage;

    public ImageDisplayMod() {
        super("mod.imagedisplay.name", "mod.imagedisplay.description", Icon.IMAGE);
    }

    public final EventBus.EventListener<RenderSkiaEvent> onRenderSkia = event -> {
        this.draw();
    };

    private void draw() {

        if(imageSetting.getFile() != null && prevImage != imageSetting.getFile()) {

            prevImage = imageSetting.getFile();

            try {
                image = ImageIO.read(imageSetting.getFile());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if(image != null) {

            int width = image.getWidth();
            int height = image.getHeight();

            if(width > 500 || height > 500) {

                if((width < 1000 || height < 1000)) {
                    width = width / 2;
                    height = height / 2;
                }

                if((width > 1000 || height > 1000)) {
                    width = width / 3;
                    height = height / 3;
                }
            }

            this.begin();

            if(shadowSetting.isEnabled()) {
                Skia.drawShadow(getX(), getY(), width, height, radiusSetting.getValue());
            }

            Skia.drawRoundedImage(imageSetting.getFile(), getX(), getY(), width, height,
                radiusSetting.getValue());

            this.finish();

            position.setSize(width, height);
        }
    }

    @Override
    public float getRadius() {
        return radiusSetting.getValue();
    }
}
