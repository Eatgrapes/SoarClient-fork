package com.soarclient.ui.component;

import com.soarclient.ui.component.handler.ComponentHandler;
import net.minecraft.client.Minecraft;

public class Component {

	protected Minecraft client = Minecraft.getInstance();
	protected ComponentHandler handler;
	protected float x, y, width, height;

	public Component(float x, float y) {
		this.x = x;
		this.y = y;
		this.handler = new ComponentHandler() {
		};
	}

	public void draw(double mouseX, double mouseY) {
	}

	public void mousePressed(double mouseX, double mouseY, int button) {
	}

	public void mouseReleased(double mouseX, double mouseY, int button) {
	}

	public void charTyped(char chr, int modifiers) {
	}

	public void keyPressed(int keyCode, int scanCode, int modifiers) {
	}

	public float getX() {
		return x;
	}

	public void setX(float x) {
		this.x = x;
	}

	public float getY() {
		return y;
	}

	public void setY(float y) {
		this.y = y;
	}

	public float getWidth() {
		return width;
	}

	public float getHeight() {
		return height;
	}

	public ComponentHandler getHandler() {
		return handler;
	}

	public void setHandler(ComponentHandler handler) {
		this.handler = handler;
	}
}
