package com.soarclient.management.mod.api;

import net.minecraft.client.Minecraft;

public class Position {

	private AnchorPosition anchor;
	private float x, y, width, height;
	private float scale;

	public Position(float x, float y, float width, float height) {
		setSize(width, height);
		setPosition(x, y);
		scale = 1.0F;
	}

	public void setPosition(float x, float y) {

		float rightX = x + width;
		float bottomY = y + height;
		float screenWidth = getScreenWidth();
		float screenHeight = getScreenHeight();

		if (x <= screenWidth / 3f && y <= screenHeight / 3f) {
			this.anchor = AnchorPosition.TOP_LEFT;
		} else if (rightX >= screenWidth / 3f * 2f && y <= screenHeight / 3f) {
			this.anchor = AnchorPosition.TOP_RIGHT;
		} else if (x <= screenWidth / 3f && bottomY >= screenHeight / 3f * 2f) {
			this.anchor = AnchorPosition.BOTTOM_LEFT;
		} else if (rightX >= screenWidth / 3f * 2f && bottomY >= screenHeight / 3f * 2f) {
			this.anchor = AnchorPosition.BOTTOM_RIGHT;
		} else if (y <= screenHeight / 3f) {
			this.anchor = AnchorPosition.TOP_CENTER;
		} else if (x <= screenWidth / 3f) {
			this.anchor = AnchorPosition.MIDDLE_LEFT;
		} else if (rightX >= screenWidth / 3f * 2f) {
			this.anchor = AnchorPosition.MIDDLE_RIGHT;
		} else if (bottomY >= screenHeight / 3f * 2f) {
			this.anchor = AnchorPosition.BOTTOM_CENTER;
		} else {
			this.anchor = AnchorPosition.MIDDLE_CENTER;
		}

		this.x = x - getAnchorX(screenWidth) + getAnchorX(width);
		this.y = y - getAnchorY(screenHeight) + getAnchorY(height);
	}

	public void setRawPosition(float x, float y) {
		this.x = x;
		this.y = y;
	}

	public void setSize(float width, float height) {
		this.width = width * scale;
		this.height = height * scale;
	}

	public void updateSizePosition(float width, float height) {

		float x = getX();
		float y = getY();

		setSize(width, height);
		setPosition(x, y);
	}

	public float getX() {
		return x + getAnchorX(getScreenWidth()) - getAnchorX(width);
	}

	public float getY() {
		return y + getAnchorY(getScreenHeight()) - getAnchorY(height);
	}

	public float getRawX() {
		return x;
	}

	public float getRawY() {
		return y;
	}

	public float getRightX() {
		return getX() + width;
	}

	public float getBottomY() {
		return getY() + height;
	}

	public float getCenterX() {
		return getX() + width / 2f;
	}

	public float getCenterY() {
		return getY() + height / 2f;
	}

	public float getWidth() {
		return width;
	}

	public float getHeight() {
		return height;
	}

	public AnchorPosition getAnchor() {
		return anchor;
	}

	public void setAnchor(AnchorPosition anchor) {
		this.anchor = anchor;
	}

	private float getAnchorX(float value) {
		return value * anchor.getX();
	}

	private float getAnchorY(float value) {
		return value * anchor.getY();
	}

	private float getScreenWidth() {
		return Minecraft.getInstance().getWindow().getGuiScaledWidth();
	}

	private float getScreenHeight() {
		return Minecraft.getInstance().getWindow().getGuiScaledHeight();
	}

	public float getScale() {
		return scale;
	}

	public void setScale(float scale) {
		this.scale = Math.max(0.2F, Math.min(5.0F, scale));
	}
}