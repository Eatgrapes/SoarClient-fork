package com.soarclient.management.mod.impl.settings;

import com.soarclient.Soar;
import com.soarclient.event.EventBus;
import com.soarclient.event.client.ClientTickEvent;
import com.soarclient.gui.modmenu.GuiModMenu;
import com.soarclient.libraries.material3.hct.Hct;
import com.soarclient.management.config.ConfigType;
import com.soarclient.management.mod.Mod;
import com.soarclient.management.mod.ModCategory;
import com.soarclient.management.mod.settings.impl.*;
import com.soarclient.skia.font.Fonts;
import com.soarclient.skia.font.Icon;
import com.soarclient.utils.language.I18n;
import com.soarclient.utils.language.Language;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ModMenuSettings extends Mod {

    private static ModMenuSettings instance;
    private String previousLanguageOption = "language.english";
    private boolean languageInitialized = false;

    private KeybindSetting keybindSetting = new KeybindSetting("setting.keybind", "setting.keybind.description",
        Icon.KEYBOARD, this, InputUtil.fromKeyCode(GLFW.GLFW_KEY_RIGHT_SHIFT, 0));
    private BooleanSetting darkModeSetting = new BooleanSetting("setting.darkmode", "setting.darkmode.description",
        Icon.DARK_MODE, this, false);
    private HctColorSetting hctColorSetting = new HctColorSetting("setting.color", "setting.color.description",
        Icon.PALETTE, this, Hct.from(220, 26, 6));
    private BooleanSetting blurSetting = new BooleanSetting("setting.blur", "setting.blur.description", Icon.LENS_BLUR,
        this, true);
    private NumberSetting blurIntensitySetting = new NumberSetting("setting.blurintensity",
        "setting.blurintensity.description", Icon.BLUR_LINEAR, this, 5, 1, 20, 1);

    private ComboSetting uiStyleSetting = new ComboSetting("setting.uistyle", "setting.uistyle.description",
            Icon.VIEW_QUILT, this, Arrays.asList("MD3", "win"), "MD3");

    private ComboSetting languageSetting = new ComboSetting("setting.language", "setting.language.description",
        Icon.LANGUAGE, this, Arrays.asList("language.english", "language.chinese", "language.japanese"), "language.english");

    private ComboSetting fontSetting;

    private Screen modMenu;

    public ModMenuSettings() {
        super("mod.modmenu.name", "mod.modmenu.description", Icon.MENU, ModCategory.MISC);

        instance = this;
        this.setHidden(true);
        this.setEnabled(true);
        initFontSetting();
    }

    private void initFontSetting() {
        List<String> fontOptions = new ArrayList<>();

        fontOptions.add("font.default");

        String[] customFonts = Fonts.getCustomFontNames();
        if (customFonts.length > 0) {
            fontOptions.addAll(Arrays.asList(customFonts));
            System.out.println("Added custom fonts to options: " + Arrays.toString(customFonts));
        }

        fontSetting = new ComboSetting("setting.font", "setting.font.description",
            Icon.TEXT_FORMAT, this, fontOptions, "font.default");
    }

    private void initializeLanguageSetting() {
        String configLanguage = languageSetting.getOption();
        Language targetLanguage = null;

        if (configLanguage != null && !configLanguage.isEmpty() && languageSetting.has(configLanguage)) {
            targetLanguage = getLanguageEnumFromOption(configLanguage);
        } else {
            targetLanguage = Language.ENGLISH;
            configLanguage = "language.english";
            languageSetting.setOption(configLanguage);
        }

        // set I18n language
        I18n.setLanguage(targetLanguage);
        previousLanguageOption = configLanguage;
    }

    private String getLanguageOptionFromEnum(Language language) {
        switch (language) {
            case CHINESE:
                return "language.chinese";
            case JAPANESE:
                return "language.japanese";
            default:
                return "language.english";
        }
    }

    private Language getLanguageEnumFromOption(String option) {
        if (option.contains("chinese")) {
            return Language.CHINESE;
        } else if (option.contains("japanese")) {
            return Language.JAPANESE;
        } else {
            return Language.ENGLISH;
        }
    }

    public final EventBus.EventListener<ClientTickEvent> onClientTick = event -> {

        if (!languageInitialized) {
            initializeLanguageSetting();
            languageInitialized = true;
        }

        if (keybindSetting.isPressed()) {
            if (modMenu == null) {
                modMenu = new GuiModMenu().build();
            }
            client.setScreen(modMenu);
        }

        handleLanguageChange();
    };

    private void handleLanguageChange() {
        String currentLanguageOption = languageSetting.getOption();

        if (currentLanguageOption == null || currentLanguageOption.isEmpty()) {
            return;
        }

        if (!currentLanguageOption.equals(previousLanguageOption)) {
            Language targetLanguage = getLanguageEnumFromOption(currentLanguageOption);

            if (I18n.getCurrentLanguage() != targetLanguage) {
                I18n.setLanguage(targetLanguage);

                if (modMenu != null) {
                    modMenu = null;
                }
            }

            previousLanguageOption = currentLanguageOption;

            // save config
            Soar.getInstance().getConfigManager().save(ConfigType.MOD);
        }
    }

    @Override
    public void onDisable() {
        this.setEnabled(true);
    }

    public static ModMenuSettings getInstance() {
        return instance;
    }

    public BooleanSetting getDarkModeSetting() {
        return darkModeSetting;
    }

    public HctColorSetting getHctColorSetting() {
        return hctColorSetting;
    }

    public BooleanSetting getBlurSetting() {
        return blurSetting;
    }

    public NumberSetting getBlurIntensitySetting() {
        return blurIntensitySetting;
    }

    public ComboSetting getUiStyleSetting() {
        return uiStyleSetting;
    }

    public ComboSetting getLanguageSetting() {
        return languageSetting;
    }

    public ComboSetting getFontSetting() {
        return fontSetting;
    }

    public Screen getModMenu() {
        return modMenu;
    }
}
