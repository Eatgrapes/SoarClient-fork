package com.soarclient.gui.modmenu.pages;

import com.soarclient.Soar;
import com.soarclient.gui.api.SoarGui;
import com.soarclient.gui.api.page.Page;
import com.soarclient.gui.api.page.impl.RightLeftTransition;
import com.soarclient.management.color.api.ColorPalette;
import com.soarclient.skia.Skia;
import com.soarclient.skia.font.Fonts;
import com.soarclient.skia.font.Icon;
import com.soarclient.utils.language.I18n;
import io.github.humbleui.skija.ClipMode;
import io.github.humbleui.skija.Font;
import io.github.humbleui.types.Rect;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;

import java.util.ArrayList;
import java.util.List;

public class HomePage extends Page {
    private static final float CARD_WIDTH = 400;
    private static final float CARD_HEIGHT = 120;
    private static final float TIPS_WIDTH = 400;
    private float cardX;
    private float cardY;
    private float tipsX;
    private float tipsY;

    public HomePage(SoarGui parent) {
        super(parent, "text.home", Icon.HOME, new RightLeftTransition(true));
    }

    @Override
    public void init() {
        super.init();
        cardX = x + 20;
        cardY = y + 90;
        tipsX = cardX;
        tipsY = cardY + CARD_HEIGHT + 20;
    }

    @Override
    public void draw(double mouseX, double mouseY) {
        super.draw(mouseX, mouseY);

        ColorPalette palette = Soar.getInstance().getColorManager().getPalette();
        ClientPlayerEntity player = MinecraftClient.getInstance().player;

        Skia.drawText(I18n.get("text.home"), x + 32, y + 30,
            palette.getOnSurface(), Fonts.getRegular(40));

        if (player != null) {
            Skia.drawRoundedRect(cardX, cardY, CARD_WIDTH, CARD_HEIGHT, 10, palette.getSurface());

            float avatarSize = 90;
            float avatarX = cardX + 20;
            float avatarY = cardY + (CARD_HEIGHT - avatarSize) / 2;

            Skia.drawRoundedRect(avatarX, avatarY, avatarSize, avatarSize, 8, palette.getSurfaceContainerHighest());

            SkinTextures skinTextures = player.getSkinTextures();
            if (skinTextures != null && skinTextures.texture() != null) {
                Skia.drawPlayerHead(skinTextures.texture(), avatarX, avatarY, avatarSize, avatarSize, 8);
            }

            float textX = avatarX + avatarSize + 15;
            float textBaselineY = cardY + CARD_HEIGHT/2 - 33;
            String playerName = player.getName().getString();
            Skia.drawText(playerName, textX, textBaselineY, palette.getOnSurface(), Fonts.getRegular(30));

            drawTipsCard(palette);
        }
    }

    private void drawTipsCard(ColorPalette palette) {
        String tipsTitle = I18n.get("tips.oldanimations.title");
        String tipsContent = I18n.get("tips.oldanimations.content");

        Font titleFont = Fonts.getMedium(16);
        Font contentFont = Fonts.getRegular(14);
        Font iconFont = Fonts.getIcon(20);

        float contentWidth = TIPS_WIDTH - 60;
        List<String> wrappedLines = wrapTextWithMeasurement(tipsContent, contentWidth, contentFont);

        float lineHeight = 18;
        float titleHeight = 25;
        float iconHeight = 20;
        float padding = 15;
        float totalHeight = padding + Math.max(iconHeight, titleHeight) + (wrappedLines.size() * lineHeight) + padding;

        Skia.drawRoundedRect(tipsX, tipsY, TIPS_WIDTH, totalHeight, 10, palette.getSurface());

        float iconX = tipsX + padding;
        float iconY = tipsY + padding;
        Skia.drawText(Icon.WARNING, iconX, iconY, palette.getOnSurfaceVariant(), iconFont);

        float titleX = iconX + 30;
        float titleY = tipsY + padding + 5;
        Skia.drawText(tipsTitle, titleX, titleY, palette.getOnSurface(), titleFont);

        float contentX = titleX;
        float contentY = titleY + titleHeight;

        for (int i = 0; i < wrappedLines.size(); i++) {
            Skia.drawText(wrappedLines.get(i), contentX, contentY + (i * lineHeight),
                palette.getOnSurfaceVariant(), contentFont);
        }
    }

    private List<String> wrapTextWithMeasurement(String text, float maxWidth, Font font) {
        List<String> lines = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return lines;
        }

        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            float testWidth = getTextWidth(testLine, font);

            if (testWidth <= maxWidth) {
                if (currentLine.length() > 0) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    List<String> brokenWord = breakLongWord(word, maxWidth, font);
                    lines.addAll(brokenWord.subList(0, brokenWord.size() - 1));
                    currentLine = new StringBuilder(brokenWord.get(brokenWord.size() - 1));
                }
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }

    private float getTextWidth(String text, Font font) {
        Rect bounds = Skia.getTextBounds(text, font);
        return bounds.getWidth();
    }

    private List<String> breakLongWord(String word, float maxWidth, Font font) {
        List<String> parts = new ArrayList<>();
        StringBuilder currentPart = new StringBuilder();

        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            String testPart = currentPart.toString() + c;

            if (getTextWidth(testPart, font) <= maxWidth) {
                currentPart.append(c);
            } else {
                if (currentPart.length() > 0) {
                    parts.add(currentPart.toString());
                    currentPart = new StringBuilder(String.valueOf(c));
                } else {
                    parts.add(String.valueOf(c));
                }
            }
        }

        if (currentPart.length() > 0) {
            parts.add(currentPart.toString());
        }

        return parts;
    }
}
