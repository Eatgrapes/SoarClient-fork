package com.soarclient.skia.font;

import com.soarclient.management.mod.impl.settings.ModMenuSettings;
import com.soarclient.utils.language.I18n;
import io.github.humbleui.skija.*;
import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Fonts {

    private static final String REGULAR = "Inter-Regular-CJKsc.ttf";
    private static final String MEDIUM = "Inter-Medium-CJKsc.ttf";
    private static final String ICON_FILL = "MaterialSymbolsRounded_Fill.ttf";
    private static final String ICON = "MaterialSymbolsRounded.ttf";

    private static FontMgr fontMgr = FontMgr.getDefault();
    private static final FontStyle NORMAL_STYLE = FontStyle.NORMAL;
    private static final Map<String, Typeface> customFonts = new HashMap<>();
    private static File fontDir;

    public static void loadAll() {
        FontHelper.preloadFonts(REGULAR, MEDIUM, ICON_FILL, ICON);
        loadCustomFonts();
    }

    private static void loadCustomFonts() {
        try {
            fontDir = new File(MinecraftClient.getInstance().runDirectory, "soar/Font");
            if (!fontDir.exists()) {
                fontDir.mkdirs();
            }

            customFonts.clear();
            File[] fontFiles = fontDir.listFiles((dir, name) ->
                name.toLowerCase().endsWith(".ttf") ||
                    name.toLowerCase().endsWith(".otf"));

            if (fontFiles != null) {
                FontMgr fontMgr = FontMgr.getDefault();

                for (File fontFile : fontFiles) {
                    try {
                        byte[] fontData = Files.readAllBytes(fontFile.toPath());
                        Data skData = Data.makeFromBytes(fontData);
                        Typeface typeface = fontMgr.makeFromData(skData);
                        if (typeface != null) {
                            String fontName = fontFile.getName();
                            customFonts.put(fontName, typeface);
                            System.out.println("Loaded custom font: " + fontName); // 添加日志
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to load font: " + fontFile.getName() + ", error: " + e.getMessage());
                    }
                }
                System.out.println("Total custom fonts loaded: " + customFonts.size()); // 添加日志
            }
        } catch (Exception e) {
            System.err.println("Failed to load custom fonts: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private static Font createSystemFont(String familyName, float size) {
        try {
            Typeface typeface = fontMgr.matchFamilyStyle(familyName, NORMAL_STYLE);
            if (typeface == null) {
                return FontHelper.load(REGULAR, size);
            }
            return new Font(typeface, size);
        } catch (Exception e) {
            return FontHelper.load(REGULAR, size);
        }
    }

    public static Font getRegular(float size) {
        return getCurrentFont(size);
    }

    public static Font getMedium(float size) {
        String selectedFont = ModMenuSettings.getInstance().getFontSetting().getOption();
        if (selectedFont.equals("font.default")) {
            return FontHelper.load(MEDIUM, size);
        }
        return getCurrentFont(size);
    }

    public static Font getIconFill(float size) {
        return FontHelper.load(ICON_FILL, size);
    }

    public static Font getIcon(float size) {
        return FontHelper.load(ICON, size);
    }

    public static String[] getCustomFontNames() {
        List<String> fontNames = new ArrayList<>();
        for (String fontFile : customFonts.keySet()) {
            String fontName = fontFile;
            if (fontName.toLowerCase().endsWith(".ttf") || fontName.toLowerCase().endsWith(".otf")) {
                fontName = fontName.substring(0, fontName.lastIndexOf('.'));
            }
            fontNames.add(fontName);
        }
        System.out.println("Available custom fonts: " + fontNames);
        return fontNames.toArray(new String[0]);
    }

    private static Font getCurrentFont(float size) {
        String selectedFont = ModMenuSettings.getInstance().getFontSetting().getOption();
        if ("font.default".equals(selectedFont)) {
            return FontHelper.load(REGULAR, size);
        }

        try {
            for (String existingFont : customFonts.keySet()) {
                String existingFontName = existingFont;
                if (existingFontName.toLowerCase().endsWith(".ttf") || existingFontName.toLowerCase().endsWith(".otf")) {
                    existingFontName = existingFontName.substring(0, existingFontName.lastIndexOf('.'));
                }
                if (selectedFont.equals(existingFontName)) {
                    return new Font(customFonts.get(existingFont), size);
                }
            }

            // try system font
            switch (selectedFont) {
                case "font.microsoft_yahei":
                    return createSystemFont("Microsoft YaHei", size);
                case "font.noto_sans":
                    Font font = createSystemFont("Noto Sans CJK SC", size);
                    if (font == null) {
                        font = createSystemFont("Noto Sans", size);
                    }
                    return font != null ? font : FontHelper.load(REGULAR, size);
                default:
                    // if not found,return regular Font
                    return FontHelper.load(REGULAR, size);
            }
        } catch (Exception e) {
            System.err.println("Error loading font: " + e.getMessage());
            return FontHelper.load(REGULAR, size);
        }
    }
}
