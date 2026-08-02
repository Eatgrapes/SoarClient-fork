package com.soarclient.management.cape;

import com.soarclient.skia.Skia;
import io.github.humbleui.skija.ClipMode;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.Path;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;
import net.minecraft.resources.Identifier;

public class CapeRenderer {

	public static void renderCapePreview(Identifier capeTexture, float x, float y, float width, float height) {
		Image image = image(capeTexture);
		if (image == null) {
			return;
		}

		float ratio = image.getWidth() / 64F;
		Skia.save();
		Skia.translate(x + 2, y + 8);
		Skia.scale(2F, 2F, 1F);
		Skia.getCanvas().drawImageRect(image, Rect.makeXYWH(ratio, ratio, 10 * ratio, 16 * ratio), Rect.makeXYWH(0, 0, 10, 16), null, false);
		Skia.restore();

		Skia.save();
		Skia.translate(x + 26, y + 8);
		Skia.scale(2F, 2F, 1F);
		Skia.getCanvas().drawImageRect(image, Rect.makeXYWH(12 * ratio, ratio, 10 * ratio, 16 * ratio), Rect.makeXYWH(0, 0, 10, 16), null, false);
		Skia.restore();
	}

	public static void renderRoundedCapePreview(Identifier capeTexture, float x, float y,
			float width, float height, float radius) {
		Image image = image(capeTexture);
		if (image == null) {
			return;
		}

		float ratio = image.getWidth() / 64F;
		try (Path path = Path.makeRRect(RRect.makeXYWH(x, y, width, height, radius))) {
			Skia.save();
			Skia.getCanvas().clipPath(path, ClipMode.INTERSECT, true);
			Skia.getCanvas().drawImageRect(image, Rect.makeXYWH(ratio, ratio, 10 * ratio, 16 * ratio), Rect.makeXYWH(x, y, width, height), null, false);
			Skia.restore();
		}
	}

	private static Image image(Identifier identifier) {
		return identifier == null ? null : Skia.getImageHelper().get(identifier);
	}
}
