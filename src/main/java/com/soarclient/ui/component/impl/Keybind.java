package com.soarclient.ui.component.impl;

import org.lwjgl.glfw.GLFW;

import com.soarclient.Soar;
import com.soarclient.management.color.api.ColorPalette;
import com.soarclient.skia.Skia;
import com.soarclient.skia.font.Fonts;
import com.soarclient.ui.component.Component;
import com.soarclient.ui.component.api.PressAnimation;
import com.soarclient.ui.component.handler.impl.KeybindHandler;
import com.soarclient.utils.mouse.MouseUtils;

import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.InputUtil;

public class Keybind extends Component {

	private PressAnimation pressAnimation = new PressAnimation();
	private int[] pressedPos;

	private boolean binding;
	private InputUtil.Key key;

	public Keybind(float x, float y, InputUtil.Key key) {
		super(x, y);
		this.key = key;
		width = 126;
		height = 32;
		pressedPos = new int[] { 0, 0 };
	}

	@Override
	public void draw(double mouseX, double mouseY) {

		ColorPalette palette = Soar.getInstance().getColorManager().getPalette();

		Skia.drawRoundedRect(x, y, width, height, 12, palette.getPrimary());
		Skia.save();
		Skia.clip(x, y, width, height, 12);
		pressAnimation.draw(x + pressedPos[0], y + pressedPos[1], width, height, palette.getPrimaryContainer(), 0.12F);
		Skia.restore();

		Skia.drawFullCenteredText(binding ? "..." : key.getLocalizedText().getString(), x + (width / 2), y + (height / 2), palette.getSurface(),
				Fonts.getMedium(14));
	}

	@Override
	public void mousePressed(double mouseX, double mouseY, int button) {
		if (MouseUtils.isInside(mouseX, mouseY, x, y, width, height) && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			pressAnimation.onPressed(mouseX, mouseY, x, y);
		}
	}

	@Override
	public void mouseReleased(double mouseX, double mouseY, int button) {

		if (MouseUtils.isInside(mouseX, mouseY, x, y, width, height) && !binding) {
			if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
				binding = true;
			}
			return;
		}

		if (binding) {

			if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
				setKeyCode(InputUtil.UNKNOWN_KEY);
			} else if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT && button != GLFW.GLFW_MOUSE_BUTTON_RIGHT
					&& button != GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
				setKeyCode(InputUtil.Type.MOUSE.createFromCode(button));
			}

			binding = false;
		}

		pressAnimation.onReleased(mouseX, mouseY, x, y);
	}

	@Override
	public void keyPressed(int keyCode, int scanCode, int modifiers) {
		if (binding) {
			setKeyCode(InputUtil.fromKeyCode(keyCode, scanCode));
			this.binding = false;
		}
	}

	public InputUtil.Key getKeyCode() {
		return key;
	}

	public void setKeyCode(InputUtil.Key key) {

		this.key = key;

		if (handler instanceof KeybindHandler) {
			((KeybindHandler) handler).onBinded(key);
		}
	}
}
