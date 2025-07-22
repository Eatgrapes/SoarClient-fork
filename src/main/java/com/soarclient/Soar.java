package com.soarclient;

import com.soarclient.animation.Delta;
import com.soarclient.event.EventBus;
import com.soarclient.event.server.PacketHandler;
import com.soarclient.libraries.browser.JCefBrowser;
import com.soarclient.management.cape.CapeManager;
import com.soarclient.management.color.ColorManager;
import com.soarclient.management.config.ConfigManager;
import com.soarclient.management.hypixel.HypixelManager;
import com.soarclient.management.mod.ModManager;
import com.soarclient.management.music.MusicManager;
import com.soarclient.management.profile.ProfileManager;
import com.soarclient.management.user.UserManager;
import com.soarclient.management.websocket.WebSocketManager;
import com.soarclient.skia.font.Fonts;
import com.soarclient.utils.file.FileLocation;
import com.soarclient.utils.language.I18n;
import com.soarclient.utils.language.Language;

public class Soar {

    private final static Soar instance = new Soar();

    private final String name = "Soar-fork";
    private final String version = "8.0";

    private long launchTime;

    private ModManager modManager;
    private ColorManager colorManager;
    private MusicManager musicManager;
    private ConfigManager configManager;
    private ProfileManager profileManager;
    private WebSocketManager webSocketManager;
    private UserManager userManager;
    private HypixelManager hypixelManager;
    private CapeManager capeManager;

    public void start() {
        JCefBrowser.download();
        Fonts.loadAll();
        FileLocation.init();
        // I18n.setLanguage(Language.ENGLISH); // it is dead

        launchTime = System.currentTimeMillis();

        modManager = new ModManager();
        modManager.init();
        colorManager = new ColorManager();
        musicManager = new MusicManager();
        configManager = new ConfigManager();
        profileManager = new ProfileManager();
        webSocketManager = new WebSocketManager();
        userManager = new UserManager();
        hypixelManager = new HypixelManager();
        capeManager = new CapeManager();

        EventBus.getInstance().register(new SoarHandler());
        EventBus.getInstance().register(new PacketHandler());
        EventBus.getInstance().register(new Delta());
    }

    public static Soar getInstance() {
        return instance;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public long getLaunchTime() {
        return launchTime;
    }

    public ModManager getModManager() {
        return modManager;
    }

    public ColorManager getColorManager() {
        return colorManager;
    }

    public MusicManager getMusicManager() {
        return musicManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ProfileManager getProfileManager() {
        return profileManager;
    }

    public WebSocketManager getWebSocketManager() {
        return webSocketManager;
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public HypixelManager getHypixelManager() {
        return hypixelManager;
    }

    public CapeManager getCapeManager() {
        return capeManager;
    }
}
