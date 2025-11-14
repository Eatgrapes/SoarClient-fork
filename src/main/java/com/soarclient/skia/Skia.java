package com.soarclient.skia;

import java.awt.Color;
import java.io.File;

import com.soarclient.management.mod.impl.settings.HUDModSettings;
import com.soarclient.shader.impl.KawaseBlur;
import com.soarclient.skia.context.SkiaContext;
import com.soarclient.skia.image.ImageHelper;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.ClipMode;
import io.github.humbleui.skija.FilterTileMode;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.FontMetrics;
import io.github.humbleui.skija.ImageFilter;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.skija.Path;
import io.github.humbleui.skija.Shader;
import io.github.humbleui.skija.SurfaceOrigin;
import io.github.humbleui.types.Point;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import net.minecraft.util.Identifier;

public class Skia {

	private static final ImageHelper imageHelper = new ImageHelper();

	public static void drawRect(float x, float y, float width, float height, Color color) {
		getCanvas().drawRect(Rect.makeXYWH(x, y, width, height), getPaint(color));
	}

	public static void drawCircle(float x, float y, float radius, Color color) {
		Paint paint = getPaint(color);
		getCanvas().drawCircle(x, y, radius, paint);
	}

	public static void drawCircle(float x, float y, float radius, float strokeWidth, Color color) {
		Paint paint = getPaint(color);
		paint.setMode(PaintMode.STROKE);
		paint.setStrokeWidth(strokeWidth);
		getCanvas().drawCircle(x, y, radius, paint);
	}

	public static void drawRoundedRect(float x, float y, float width, float height, float radius, Color color) {
		getCanvas().drawRRect(RRect.makeXYWH(x, y, width, height, radius), getPaint(color));
	}

	public static void drawRoundedRectVarying(float x, float y, float width, float height, float topLeft,
			float topRight, float bottomRight, float bottomLeft, Color color) {

		float[] corners = new float[] { topLeft, topLeft, topRight, topRight, bottomRight, bottomRight, bottomLeft,
				bottomLeft };

		getCanvas().drawRRect(RRect.makeComplexXYWH(x, y, width, height, corners), getPaint(color));
	}

	public static void drawBlur(float x, float y, float width, float height) {

		if (HUDModSettings.getInstance().getBlurSetting().isEnabled()) {

			Window window = MinecraftClient.getInstance().getWindow();
            Path path = Path.makeRect(Rect.makeXYWH(x, y, width, height));

			save();
			getCanvas().clipPath(path, ClipMode.INTERSECT, true);
			drawImage(KawaseBlur.INGAME_BLUR.getTexture(), 0, 0, window.getScaledWidth(), window.getScaledHeight(), 1F,
					SurfaceOrigin.BOTTOM_LEFT);
			restore();
		}
	}

	public static void drawRoundedBlur(float x, float y, float width, float height, float radius) {

		if (HUDModSettings.getInstance().getBlurSetting().isEnabled()) {

			Window window = MinecraftClient.getInstance().getWindow();
            Path path = Path.makeRRect(RRect.makeXYWH(x, y, width, height, radius));

			save();
			getCanvas().clipPath(path, ClipMode.INTERSECT, true);
			drawImage(KawaseBlur.INGAME_BLUR.getTexture(), 0, 0, window.getScaledWidth(), window.getScaledHeight(), 1F,
					SurfaceOrigin.BOTTOM_LEFT);
			restore();
		}
	}

	public static void drawShadow(float x, float y, float width, float height, float radius) {

		Paint paint = getPaint(new Color(0, 0, 0, 120));

		paint.setImageFilter(ImageFilter.makeBlur(2.5F, 2.5F, FilterTileMode.DECAL));

		save();
		clip(x, y, width, height, radius, ClipMode.DIFFERENCE);
		getCanvas().drawRRect(RRect.makeXYWH(x, y, width, height, radius), paint);
		restore();
	}

	public static void drawOutline(float x, float y, float width, float height, float radius, float strokeWidth,
			Color color) {

		float halfStroke = strokeWidth / 2;

        Path path = Path.makeRRect(RRect.makeXYWH(x + halfStroke, y + halfStroke, width - strokeWidth, height - strokeWidth,
				radius - halfStroke));

		Paint paint = getPaint(color);
		paint.setStrokeWidth(strokeWidth);
		paint.setMode(PaintMode.STROKE);

		getCanvas().drawPath(path, paint);
	}

	public static void drawImage(String path, float x, float y, float width, float height) {

		path = "/assets/soar/" + path;

		if (imageHelper.load(path)) {
			getCanvas().drawImageRect(imageHelper.get(path), Rect.makeXYWH(x, y, width, height));
		}
	}

	public static void drawImage(int textureId, float x, float y, float width, float height, float alpha,
			SurfaceOrigin origin) {

		if (imageHelper.load(textureId, width, height, origin)) {
			Paint paint = new Paint();
			paint.setAlpha((int) (255 * alpha));
			getCanvas().drawImageRect(imageHelper.get(textureId), Rect.makeXYWH(x, y, width, height), paint);
		}
	}

	public static void drawImage(int textureId, float x, float y, float width, float height, float alpha) {
		drawImage(textureId, x, y, width, height, alpha, SurfaceOrigin.TOP_LEFT);
	}

	public static void drawImage(File file, float x, float y, float width, float height) {
		if (imageHelper.load(file)) {
			getCanvas().drawImageRect(imageHelper.get(file.getName()), Rect.makeXYWH(x, y, width, height));
		}
	}

	public static void drawImage(int textureId, float x, float y, float width, float height, SurfaceOrigin origin) {

		if (imageHelper.load(textureId, width, height, origin)) {
			getCanvas().drawImageRect(imageHelper.get(textureId), Rect.makeXYWH(x, y, width, height));
		}
	}

	public static void drawImage(int textureId, float x, float y, float width, float height) {
		drawImage(textureId, x, y, width, height, SurfaceOrigin.TOP_LEFT);
	}

	public static void drawRoundedImage(int textureId, float x, float y, float width, float height, float radius) {

        Path path = Path.makeRRect(RRect.makeXYWH(x, y, width, height, radius));

		save();
		getCanvas().clipPath(path, ClipMode.INTERSECT, true);
		drawImage(textureId, x, y, width, height);
		restore();
	}

	public static void drawRoundedImage(String filePath, float x, float y, float width, float height, float radius) {

        Path path = Path.makeRRect(RRect.makeXYWH(x, y, width, height, radius));

		save();
		getCanvas().clipPath(path, ClipMode.INTERSECT, true);
		drawImage(filePath, x, y, width, height);
		restore();
	}

	public static void drawRoundedImage(File file, float x, float y, float width, float height, float radius) {

        Path path = Path.makeRRect(RRect.makeXYWH(x, y, width, height, radius));

		save();
		getCanvas().clipPath(path, ClipMode.INTERSECT, true);
		drawImage(file, x, y, width, height);
		restore();
	}

	public static void drawRoundedImage(int textureId, float x, float y, float width, float height, float radius,
			float alpha, SurfaceOrigin origin) {
        Path path = Path.makeRRect(RRect.makeXYWH(x, y, width, height, radius));

		save();
		getCanvas().clipPath(path, ClipMode.INTERSECT, true);
		drawImage(textureId, x, y, width, height, alpha, origin);
		restore();
	}

	public static void drawRoundedImage(int textureId, float x, float y, float width, float height, float radius,
			float alpha) {
		drawRoundedImage(textureId, x, y, width, height, radius, alpha, SurfaceOrigin.TOP_LEFT);
	}

	public static void drawPlayerHead(File file, float x, float y, float width, float height, float radius) {
		if (imageHelper.load(file)) {

            Path path = Path.makeRRect(RRect.makeXYWH(x, y, width, height, radius));

			Rect srcRect = Rect.makeXYWH(8, 8, 8, 8);
			Rect srcRect1 = Rect.makeXYWH(40, 8, 8, 8);
			Rect dstRect = Rect.makeXYWH(x, y, width, height);

			save();
			getCanvas().clipPath(path, ClipMode.INTERSECT, true);
			getCanvas().drawImageRect(imageHelper.get(file.getName()), srcRect, dstRect, null, false);
			getCanvas().drawImageRect(imageHelper.get(file.getName()), srcRect1, dstRect, null, false);
			restore();
		}
	}

	public static void drawSkin(File file, float x, float y, float scale) {
		if (imageHelper.load(file)) {

			Rect head = Rect.makeXYWH(8, 8, 8, 8);
			Rect headLayer = Rect.makeXYWH(40, 8, 8, 8);
			Rect body = Rect.makeXYWH(20, 20, 8, 12);
			Rect bodyLayer = Rect.makeXYWH(20, 36, 8, 12);
			Rect leftArm = Rect.makeXYWH(36, 52, 4, 12);
			Rect leftArmLayer = Rect.makeXYWH(52, 52, 4, 12);
			Rect rightArm = Rect.makeXYWH(44, 20, 4, 12);
			Rect rightArmLayer = Rect.makeXYWH(44, 36, 4, 12);
			Rect leftLeg = Rect.makeXYWH(20, 52, 4, 12);
			Rect leftLegLayer = Rect.makeXYWH(4, 52, 4, 12);
			Rect rightLeg = Rect.makeXYWH(4, 20, 4, 12);
			Rect rightLegLayer = Rect.makeXYWH(4, 36, 4, 12);

			save();
			scale(x, y, scale);
			getCanvas().drawImageRect(imageHelper.get(file.getName()), head,
					Rect.makeXYWH(x + leftArm.getWidth(), y, head.getWidth(), head.getHeight()), null, false);
			getCanvas().drawImageRect(imageHelper.get(file.getName()), headLayer,
					Rect.makeXYWH(x + leftArm.getWidth(), y, headLayer.getWidth(), headLayer.getHeight()), null, false);
			getCanvas().drawImageRect(imageHelper.get(file.getName()), body,
					Rect.makeXYWH(x + leftArm.getWidth(), y + head.getHeight(), body.getWidth(), body.getHeight()),
					null, false);
			getCanvas().drawImageRect(imageHelper.get(file.getName()), bodyLayer, Rect.makeXYWH(x + leftArm.getWidth(),
					y + headLayer.getHeight(), bodyLayer.getWidth(), bodyLayer.getHeight()), null, false);
			getCanvas().drawImageRect(imageHelper.get(file.getName()), leftArm,
					Rect.makeXYWH(x, y + head.getHeight(), leftArm.getWidth(), leftArm.getHeight()), null, false);
			getCanvas().drawImageRect(imageHelper.get(file.getName()), leftArmLayer,
					Rect.makeXYWH(x, y + headLayer.getHeight(), leftArmLayer.getWidth(), leftArmLayer.getHeight()),
					null, false);
			getCanvas().drawImageRect(imageHelper.get(file.getName()), rightArm,
					Rect.makeXYWH(x + leftArm.getWidth() + body.getWidth(), y + head.getHeight(), rightArm.getWidth(),
							rightArm.getHeight()),
					null, false);
			getCanvas().drawImageRect(imageHelper.get(file.getName()), rightArmLayer,
					Rect.makeXYWH(x + leftArmLayer.getWidth() + bodyLayer.getWidth(), y + headLayer.getHeight(),
							rightArmLayer.getWidth(), rightArmLayer.getHeight()),
					null, false);
			getCanvas().drawImageRect(
					imageHelper.get(file.getName()), leftLeg, Rect.makeXYWH(x + leftArm.getWidth(),
							y + head.getHeight() + body.getHeight(), leftLeg.getWidth(), leftLeg.getHeight()),
					null, false);
			getCanvas().drawImageRect(imageHelper.get(file.getName()), leftLegLayer,
					Rect.makeXYWH(x + leftArmLayer.getWidth(), y + headLayer.getHeight() + bodyLayer.getHeight(),
							leftLegLayer.getWidth(), leftLegLayer.getHeight()),
					null, false);
			getCanvas()
					.drawImageRect(imageHelper.get(file.getName()), rightLeg,
							Rect.makeXYWH(x + leftArm.getWidth() + leftLeg.getWidth(),
									y + head.getHeight() + body.getHeight(), rightLeg.getWidth(), rightLeg.getHeight()),
							null, false);
			getCanvas().drawImageRect(imageHelper.get(file.getName()), rightLegLayer,
					Rect.makeXYWH(x + leftArmLayer.getWidth() + leftLegLayer.getWidth(),
							y + headLayer.getHeight() + bodyLayer.getHeight(), rightLegLayer.getWidth(),
							rightLegLayer.getHeight()),
					null, false);

			restore();
		}
	}
    public static void drawMinecraftImage(String path, float x, float y, float width, float height) {
        Identifier identifier = Identifier.of("minecraft", path);

        if (imageHelper.load(identifier)) {
            getCanvas().drawImageRect(imageHelper.get(identifier.getPath()), Rect.makeXYWH(x, y, width, height));
        }
    }

	public static void drawArc(float x, float y, float radius, float startAngle, float endAngle, float strokeWidth,
			Color color) {

		Paint paint = getPaint(color);
		paint.setStrokeWidth(strokeWidth);
		paint.setMode(PaintMode.STROKE);

		getCanvas().drawArc(x - radius, y - radius, x + radius, y + radius, startAngle - 90, endAngle, false, paint);
	}

	public static void drawLine(float x, float y, float endX, float endY, float width, Color color) {

		Paint paint = getPaint(color);

		paint.setStroke(true);
		paint.setStrokeWidth(width);
		paint.setAntiAlias(true);

		getCanvas().drawLine(x, y, endX, endY, paint);
	}

	public static void drawGradientRoundedRect(float x, float y, float width, float height, float radius, Color color1,
			Color color2) {

		long currentTime = System.nanoTime();
		double speed = 0.0000000006;
		double tick = (currentTime * speed) % (2 * Math.PI);
		float max = Math.max(width, height);

        Path path = Path.makeRRect(RRect.makeXYWH(x, y, width, height, radius));

		float startX = x + width / 2 - (max / 2) * (float) Math.cos(tick);
		float startY = y + height / 2 - (max / 2) * (float) Math.sin(tick);
		float endX = x + width / 2 + (max / 2) * (float) Math.cos(tick);
		float endY = y + height / 2 + (max / 2) * (float) Math.sin(tick);

		int skColor1 = io.github.humbleui.skija.Color.makeARGB(color1.getAlpha(), color1.getRed(), color1.getGreen(),
				color1.getBlue());
		int skColor2 = io.github.humbleui.skija.Color.makeARGB(color2.getAlpha(), color2.getRed(), color2.getGreen(),
				color2.getBlue());

		int skColorMid = io.github.humbleui.skija.Color.makeARGB(color1.getAlpha(),
				(color1.getRed() + color2.getRed()) / 2, (color1.getGreen() + color2.getGreen()) / 2,
				(color1.getBlue() + color2.getBlue()) / 2);

		Paint paint = new Paint();

		paint.setShader(Shader.makeLinearGradient(new Point(startX, startY), new Point(endX, endY),
				new int[] { skColor1, skColorMid, skColor2 }, new float[] { 0, 0.5f, 1 }));

		getCanvas().drawPath(path, paint);
	}

	public static void clipPath(Path path, ClipMode mode, boolean arg) {
		getCanvas().clipPath(path, mode, arg);
	}

	public static void clipPath(Path path) {
		getCanvas().clipPath(path, ClipMode.INTERSECT, true);
	}

	public static void clip(float x, float y, float width, float height, float radius, ClipMode mode) {

        Path path = Path.makeRRect(RRect.makeXYWH(x, y, width, height, radius));
		clipPath(path, mode, true);
	}

	public static void clip(float x, float y, float width, float height, float topLeft, float topRight,
			float bottomRight, float bottomLeft) {

		float[] corners = new float[] { topLeft, topLeft, topRight, topRight, bottomRight, bottomRight, bottomLeft,
				bottomLeft };

        Path path = Path.makeRRect(RRect.makeComplexXYWH(x, y, width, height, corners));
		clipPath(path, ClipMode.INTERSECT, true);
	}

	public static void clip(float x, float y, float width, float height, float radius) {
		clip(x, y, width, height, radius, ClipMode.INTERSECT);
	}

	public static void drawText(String text, float x, float y, Color color, Font font) {
		Rect bounds = font.measureText(text);
		getCanvas().drawString(text, x - bounds.getLeft(), y - bounds.getTop(), font, getPaint(color));
	}

	public static void drawCenteredText(String text, float x, float y, Color color, Font font) {
		Rect bounds = font.measureText(text);
		getCanvas().drawString(text, x - bounds.getLeft() - (bounds.getWidth() / 2), y - bounds.getTop(), font,
				getPaint(color));
	}

	public static void drawHeightCenteredText(String text, float x, float y, Color color, Font font) {

		FontMetrics metrics = font.getMetrics();
		Rect bounds = font.measureText(text);

		float textCenterY = y + (metrics.getAscent() - metrics.getDescent()) / 2 - metrics.getAscent();

		getCanvas().drawString(text, x - bounds.getLeft(), textCenterY, font, getPaint(color));
	}

	public static void drawFullCenteredText(String text, float x, float y, Color color, Font font) {

		Rect bounds = font.measureText(text);

		FontMetrics metrics = font.getMetrics();

		float textCenterX = x - bounds.getLeft() - (bounds.getWidth() / 2);
		float textCenterY = y + (metrics.getAscent() - metrics.getDescent()) / 2 - metrics.getAscent();

		getCanvas().drawString(text, textCenterX, textCenterY, font, getPaint(color));
	}

	public static Rect getTextBounds(String text, Font font) {
		return font.measureText(text);
	}

	public static String getLimitText(String text, Font font, float width) {

		boolean isInRange = false;
		boolean isRemoved = false;

		while (!isInRange) {

			if (getTextBounds(text, font).getWidth() > width - getTextBounds("...", font).getWidth()) {
				text = text.substring(0, text.length() - 1);
				isRemoved = true;
			} else {
				isInRange = true;
			}
		}

		return text + (isRemoved ? "..." : "");
	}

	private static Paint getPaint(Color color) {
		Paint paint = new Paint();
		paint.setARGB(color.getAlpha(), color.getRed(), color.getGreen(), color.getBlue());
		return paint;
	}

	public static void save() {
		getCanvas().save();
	}

	public static void restore() {
		getCanvas().restore();
	}

	public static void scale(float scale) {
		getCanvas().scale(scale, scale);
	}

	public static void scale(float x, float y, float scale) {
		getCanvas().translate(x, y);
		getCanvas().scale(scale, scale);
		getCanvas().translate(-x, -y);
	}

	public static void scale(float x, float y, float width, float height, float scale) {

		float centerX = x + width / 2;
		float centerY = y + height / 2;

		getCanvas().translate(centerX, centerY);
		getCanvas().scale(scale, scale);
		getCanvas().translate(-centerX, -centerY);
	}

	public static void translate(float x, float y) {
		getCanvas().translate(x, y);
	}

	public static void rotate(float x, float y, float width, float height, float rotate) {

		float centerX = x + width / 2;
		float centerY = y + height / 2;

		getCanvas().translate(centerX, centerY);
		getCanvas().rotate(rotate);
		getCanvas().translate(-centerX, -centerY);
	}

	public static void setAlpha(int alpha) {

		Paint paint = new Paint();
		paint.setAlpha(alpha);

		getCanvas().saveLayer(null, paint);
	}

	public static Canvas getCanvas() {
		return SkiaContext.getCanvas();
	}

	public static ImageHelper getImageHelper() {
		return imageHelper;
	}
}
