package com.soarclient.gui.edithud.api;

import java.awt.Color;
import net.minecraft.client.Minecraft;
import com.soarclient.skia.Skia;

public class SnappingLine {

	private float line;
	private float distance;
	private float position;

	public SnappingLine(float line, float left, float size, boolean multipleSides) {

		this.line = line;
		float center = left + size / 2f;
		float right = left + size;
		float leftDistance = Math.abs(line - left);
		float centerDistance = Math.abs(line - center);
		float rightDistance = Math.abs(line - right);

		if (!multipleSides || leftDistance <= centerDistance && leftDistance <= rightDistance) {
			distance = leftDistance;
			position = line;
		} else if (centerDistance <= rightDistance) {
			distance = centerDistance;
			position = line - size / 2f;
		} else {
			distance = rightDistance;
			position = line - size;
		}
	}

	public void drawLine(float lineWidth, boolean isX) {

		Minecraft client = Minecraft.getInstance();
		Color color = Color.WHITE;

		float pos = (float) (line - lineWidth / 2f);

		if (isX) {
			Skia.drawLine(pos, 0, pos, client.getWindow().getGuiScaledHeight(), lineWidth, color);
		} else {
			Skia.drawLine(0, pos, client.getWindow().getGuiScaledWidth(), pos, lineWidth, color);
		}
	}

	public float getPosition() {
		return position;
	}

	public float getDistance() {
		return distance;
	}
}