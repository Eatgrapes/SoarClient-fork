package com.soarclient.utils;

import java.awt.Color;
import java.util.regex.Pattern;

public class ColorUtils {

	public static Color getColorFromInt(int color) {

		float r = (float) (color >> 16 & 255) / 255.0F;
		float g = (float) (color >> 8 & 255) / 255.0F;
		float b = (float) (color & 255) / 255.0F;
		float a = (float) (color >> 24 & 255) / 255.0F;

		return new Color(r, g, b, a);
	}

	public static Color blend(Color color1, Color color2, double ratio) {
		float r = (float) MathUtils.clamp(ratio, 0.0, 1.0);
		float ir = 1.0f - r;
		float[] rgb1 = new float[3];
		float[] rgb2 = new float[3];
		color1.getColorComponents(rgb1);
		color2.getColorComponents(rgb2);
		Color color = new Color(rgb1[0] * r + rgb2[0] * ir, rgb1[1] * r + rgb2[1] * ir, rgb1[2] * r + rgb2[2] * ir);
		return color;
	}

	public static String removeColorCode(String text) {
		return Pattern.compile("\\u00a7[0-9a-fklmnor]").matcher(text).replaceAll("");
	}

	public static Color applyAlpha(Color color, int alpha) {

		int red = color.getRed();
		int green = color.getGreen();
		int blue = color.getBlue();

		return new Color(red, green, blue, MathUtils.clamp(alpha, 0, 255));
	}

	public static Color applyAlpha(Color color, float alpha) {
		return applyAlpha(color, (int) (alpha * 255));
	}
}
