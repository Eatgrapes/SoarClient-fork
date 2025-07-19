package com.soarclient.management.mod.impl.hud;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.soarclient.Soar;
import com.soarclient.event.EventBus;
import com.soarclient.event.client.RenderSkiaEvent;
import com.soarclient.management.color.api.ColorPalette;
import com.soarclient.management.mod.Mod;
import com.soarclient.management.mod.ModCategory;
import com.soarclient.management.mod.api.hud.HUDMod;
import com.soarclient.management.mod.settings.impl.BooleanSetting;
import com.soarclient.management.mod.settings.impl.ComboSetting;
import com.soarclient.skia.Skia;
import com.soarclient.skia.font.Fonts;
import com.soarclient.skia.font.Icon;
import com.soarclient.utils.ColorUtils;

import com.soarclient.utils.language.I18n;
import io.github.humbleui.types.Rect;

public class ArrayListMod extends HUDMod {

    private static ArrayListMod instance;
    private BooleanSetting backgroundSetting = new BooleanSetting("setting.background",
        "setting.background.description", Icon.IMAGE, this, true);

    private BooleanSetting hudSetting = new BooleanSetting("setting.hud",
        "setting.hud.description", Icon.DASHBOARD, this, false);
    private BooleanSetting renderSetting = new BooleanSetting("setting.render",
        "setting.render.description", Icon.VISIBILITY, this, false);
    private BooleanSetting playerSetting = new BooleanSetting("setting.player",
        "setting.player.description", Icon.PERSON, this, false);
    private BooleanSetting otherSetting = new BooleanSetting("setting.other",
        "setting.other.description", Icon.MORE_HORIZ, this, false);

    private ComboSetting modeSetting = new ComboSetting("setting.mode",
        "setting.mode.description", Icon.ALIGN_HORIZONTAL_RIGHT, this,
        Arrays.asList("setting.right", "setting.left"), "setting.right");

    public ArrayListMod() {
        super("mod.arraylist.name", "mod.arraylist.description", Icon.LIST);
        instance = this;
    }

    public static ArrayListMod getInstance() {
        return instance;
    }

    public final EventBus.EventListener<RenderSkiaEvent> onRenderSkia = event -> {
        this.draw();
    };

    private void draw() {
        try {
            this.begin();
            drawArrayList();
        } catch (Exception e) {
            System.err.println("Error in ArrayListMod.draw(): " + e.getMessage());
            e.printStackTrace();
            position.setSize(100, 20);
        } finally {
            try {
                this.finish();
            } catch (Exception e) {
                System.err.println("Error in finish(): " + e.getMessage());
            }
        }
    }

    private void drawArrayList() {
        Soar instance = Soar.getInstance();
        ColorPalette colorPalette = instance.getColorManager().getPalette();

        ArrayList<Mod> enabledMods = new ArrayList<>();
        float maxWidth = 0;

        for (Mod m : instance.getModManager().getMods()) {


            if (!hudSetting.isEnabled() && m.getCategory().equals(ModCategory.HUD)) {
                continue;
            }

            if (!renderSetting.isEnabled() && m.getCategory().equals(ModCategory.RENDER)) {
                continue;
            }

            if (!playerSetting.isEnabled() && m.getCategory().equals(ModCategory.PLAYER)) {
                continue;
            }

            if (!otherSetting.isEnabled() && m.getCategory().equals(ModCategory.MISC)) {
                continue;
            }

            if (m.isEnabled() && !m.isHidden()) {

                String translatedName = I18n.get(m.getName());
                Rect textBounds = Skia.getTextBounds(translatedName, Fonts.getRegular(8.5f));
                float nameWidth = textBounds.getWidth();

                enabledMods.add(m);

                if (maxWidth < nameWidth) {
                    maxWidth = nameWidth;
                }
            }
        }

        enabledMods.sort((m1, m2) -> {
            String name1 = I18n.get(m1.getName());
            String name2 = I18n.get(m2.getName());
            float width1 = Skia.getTextBounds(name1, Fonts.getRegular(8.5f)).getWidth();
            float width2 = Skia.getTextBounds(name2, Fonts.getRegular(8.5f)).getWidth();
            return Float.compare(width2, width1);
        });

        float y = 0;
        int colorIndex = 0;
        boolean isRight = modeSetting.getOption().equals("setting.right");

        for (Mod m : enabledMods) {

            String translatedName = I18n.get(m.getName());
            Rect textBounds = Skia.getTextBounds(translatedName, Fonts.getRegular(8.5f));
            float nameWidth = textBounds.getWidth();

            if (backgroundSetting.isEnabled()) {
                float bgX = getX() + (isRight ? (maxWidth - nameWidth) : 0);
                Skia.drawRect(bgX, getY() + y, nameWidth + 5, 12, new Color(0, 0, 0, 100));
            }

            Color textColor = getInterpolatedColor(colorPalette, colorIndex);

            float textX = getX() + 3 + (isRight ? (maxWidth - nameWidth) : 0);
            float textY = getY() + y + 2.5f;

            Skia.drawText(translatedName, textX, textY, textColor, Fonts.getRegular(8.5f));

            y += 12;
            colorIndex -= 10;
        }

        position.setSize(maxWidth + 4, y);
    }

    private Color getInterpolatedColor(ColorPalette palette, int index) {
        if (palette == null) {
            return Color.WHITE;
        }

        try {
            Color primary = palette.getPrimary();
            Color secondary = palette.getSecondary();

            if (primary == null) primary = Color.WHITE;
            if (secondary == null) secondary = Color.LIGHT_GRAY;

            double factor = (Math.abs(index) % 100) / 100.0;
            return ColorUtils.blend(primary, secondary, factor);

        } catch (Exception e) {
            return Color.WHITE;
        }
    }

    @Override
    public float getRadius() {
        return 6;
    }
}
