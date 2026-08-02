package com.soarclient.utils;

import java.io.File;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import com.soarclient.mixin.interfaces.IMixinMinecraftClient;

public class SkinUtils {

	public static File getSkin(Identifier identifier) {
		String fileName = identifier.getPath().replace("skins/", "");
		String folder = fileName.substring(0, 2);
		File file = new File(((IMixinMinecraftClient) Minecraft.getInstance()).getAssetDir(),
				"skins/" + folder + "/" + fileName);
		return file;
	}
}
