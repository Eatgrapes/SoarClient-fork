package com.soarclient.management.mod.impl.render;

import com.soarclient.management.mod.Mod;
import com.soarclient.management.mod.ModCategory;
import com.soarclient.skia.font.Icon;

public class BlockOverlayMod extends Mod {

    private static BlockOverlayMod instance;

    public BlockOverlayMod() {
        super("mod.blockoverlay.name", "mod.blockoverlay.description", Icon.CROP_FREE, ModCategory.RENDER);
        instance = this;
    }

    public static BlockOverlayMod getInstance() {
        return instance;
    }
}
