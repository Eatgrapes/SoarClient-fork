package com.soarclient.ui.component.impl;

import java.awt.Color;

import com.soarclient.management.mod.impl.settings.ModMenuSettings;
import org.lwjgl.glfw.GLFW;

import com.soarclient.Soar;
import com.soarclient.management.color.api.ColorPalette;
import com.soarclient.skia.Skia;
import com.soarclient.skia.font.Fonts;
import com.soarclient.ui.component.Component;
import com.soarclient.ui.component.api.PressAnimation;
import com.soarclient.ui.component.handler.impl.ButtonHandler;
import com.soarclient.utils.language.I18n;
import com.soarclient.utils.mouse.MouseUtils;

import io.github.humbleui.types.Rect;

public class Button extends Component {

	private final PressAnimation pressAnimation = new PressAnimation();

	private final String text;
	private final Style style;

	public Button(String text, float x, float y, Style style) {
		super(x, y);
		this.text = text;
		this.height = 40;
		this.style = style;
		Rect bounds = Skia.getTextBounds(I18n.get(text), Fonts.getRegular(16));
		this.width = bounds.getWidth() + (24 * 2);
	}

	@Override
	public void draw(double mouseX, double mouseY) {
		boolean isWinStyle = ModMenuSettings.getInstance().getUiStyleSetting().getOption().equals("win");
		boolean isHovered = MouseUtils.isInside(mouseX, mouseY, x, y, width, height);

		Color[] colors = getColor();
		float borderRadius = isWinStyle ? 6 : 25;

		Color backgroundColor = colors[0];
		Color textColor = colors[1];

		if (isWinStyle && isHovered && style != Style.FILLED) {
			boolean isDarkMode = ModMenuSettings.getInstance().getDarkModeSetting().isEnabled();
			if (isDarkMode) {
				backgroundColor = new Color(255, 255, 255, 10);
			} else {
				backgroundColor = new Color(0, 0, 0, 10);
			}
		}

		Skia.drawRoundedRect(x, y, width, height, borderRadius, backgroundColor);

		if(isWinStyle && style != Style.FILLED) {
			Skia.drawOutline(x, y, width, height, borderRadius, 1.0f, new Color(0, 0, 0, 10));
		}

		Skia.save();
		Skia.clip(x, y, width, height, borderRadius);

		if (isWinStyle) {
			if (pressAnimation.isPressed()) {
				boolean isDarkMode = ModMenuSettings.getInstance().getDarkModeSetting().isEnabled();
				Skia.drawRect(x, y, width, height, isDarkMode ? new Color(255, 255, 255, 5) : new Color(0, 0, 0, 5));
			}
		} else {
			pressAnimation.draw(x, y, width, height, colors[1], 0.12F);
		}

		Skia.restore();
		Skia.drawFullCenteredText(I18n.get(text), x + (width / 2), y + (height / 2), textColor, Fonts.getRegular(16));
	}

	@Override
	public void mousePressed(double mouseX, double mouseY, int button) {

		if (MouseUtils.isInside(mouseX, mouseY, x, y, width, height) && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			pressAnimation.onPressed(mouseX, mouseY, x, y);
		}
	}

	@Override
	public void mouseReleased(double mouseX, double mouseY, int button) {
		if (MouseUtils.isInside(mouseX, mouseY, x, y, width, height) && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			if (handler instanceof ButtonHandler) {
				((ButtonHandler) handler).onAction();
			}
		}
		pressAnimation.onReleased(mouseX, mouseY, x, y);
	}

	private Color[] getColor() {

		ColorPalette palette = Soar.getInstance().getColorManager().getPalette();
		boolean isWinStyle = ModMenuSettings.getInstance().getUiStyleSetting().getOption().equals("win");

		if (isWinStyle) {
			boolean isDarkMode = ModMenuSettings.getInstance().getDarkModeSetting().isEnabled();
			Color background, foreground;

			if (style == Style.FILLED) { // Accent color button
				background = palette.getPrimary();
				foreground = palette.getOnPrimary();
			} else { // Standard button
				if (isDarkMode) {
					background = new Color(47, 47, 47, 120);
					foreground = Color.WHITE;
				} else {
					background = new Color(240, 240, 240, 120);
					foreground = Color.BLACK;
				}
			}
			return new Color[] { background, foreground };
		}

		return switch (style) {
			case ELEVATED -> new Color[]{palette.getSurfaceContainerLow(), palette.getPrimary()};
			case FILLED -> new Color[]{palette.getPrimary(), palette.getOnPrimary()};
			case TONAL -> new Color[]{palette.getSecondaryContainer(), palette.getOnSecondaryContainer()};
		};
	}

	public enum Style {
		FILLED, ELEVATED, TONAL
	}
}
