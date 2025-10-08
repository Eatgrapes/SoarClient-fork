package com.soarclient.ui.component.impl;

import com.soarclient.Soar;
import com.soarclient.animation.SimpleAnimation;
import com.soarclient.management.color.api.ColorPalette;
import com.soarclient.management.mod.impl.settings.ModMenuSettings;
import com.soarclient.skia.Skia;
import com.soarclient.skia.font.Fonts;
import com.soarclient.ui.component.Component;
import com.soarclient.ui.component.api.PressAnimation;
import com.soarclient.ui.component.handler.impl.ButtonHandler;
import com.soarclient.utils.ColorUtils;
import com.soarclient.utils.mouse.MouseUtils;

import java.awt.Color;

public class IconButton extends Component {

	private final SimpleAnimation focusAnimation = new SimpleAnimation();
	private final PressAnimation pressAnimation;

	private final String icon;
	private final Size size;
	private final Style style;

	public IconButton(String icon, float x, float y, Size size, Style style) {
		super(x, y);

		this.icon = icon;
		this.size = size;
		this.style = style;
		float[] s = getPanelSize();

		width = s[0];
		height = s[1];
		pressAnimation = new PressAnimation();
	}

	@Override
	public void draw(double mouseX, double mouseY) {

		boolean isWinStyle = ModMenuSettings.getInstance().getUiStyleSetting().getOption().equals("win");
		boolean focus = MouseUtils.isInside(mouseX, mouseY, x, y, width, height);
		ColorPalette palette = Soar.getInstance().getColorManager().getPalette();

		if (isWinStyle) {
			boolean isDarkMode = ModMenuSettings.getInstance().getDarkModeSetting().isEnabled();
			float borderRadius = getRadius();

			// Background color on hover
			if (focus) {
				Color hoverColor;
				if (isDarkMode) {
					hoverColor = new Color(255, 255, 255, 10);
				} else {
					hoverColor = new Color(0, 0, 0, 10);
				}
				Skia.drawRoundedRect(x, y, width, height, borderRadius, hoverColor);
			}

			// Press effect
			if (pressAnimation.isPressed()) {
				Color pressColor;
				if (isDarkMode) {
					pressColor = new Color(255, 255, 255, 5);
				} else {
					pressColor = new Color(0, 0, 0, 5);
				}
				Skia.drawRoundedRect(x, y, width, height, borderRadius, pressColor);
			}

			// Icon
			Skia.drawFullCenteredText(icon, x + (width / 2), y + (height / 2), palette.getOnSurface(), Fonts.getIconFill(getFontSize()));

		} else { // MD3 Style
			Color[] c = getColor();

			focusAnimation.onTick(focus ? 1F : 0, 10);

			Skia.save();
			Skia.clip(x, y, width, height, getRadius());
			Skia.drawRoundedRect(x, y, width, height, getRadius(), c[0]);
			Skia.drawRoundedRect(x, y, width, height, getRadius(),
					ColorUtils.applyAlpha(c[1], focusAnimation.getValue() * 0.08F));
			pressAnimation.draw(x, y, width, height, c[1], 0.12F);

			Skia.drawFullCenteredText(icon, x + (width / 2), y + (height / 2), c[1], Fonts.getIconFill(getFontSize()));

			Skia.restore();
		}
	}

	@Override
	public void mousePressed(double mouseX, double mouseY, int button) {
		if (MouseUtils.isInside(mouseX, mouseY, x, y, width, height) && button == 0) {
			pressAnimation.onPressed(mouseX, mouseY, x, y);
		}
	}

	@Override
	public void mouseReleased(double mouseX, double mouseY, int button) {
		if (MouseUtils.isInside(mouseX, mouseY, x, y, width, height) && button == 0) {
			if (handler instanceof ButtonHandler buttonHandler) {
				buttonHandler.onAction();
			}
		}
		pressAnimation.onReleased(mouseX, mouseY, x, y);
	}

	private float[] getPanelSize() {
		boolean isWinStyle = ModMenuSettings.getInstance().getUiStyleSetting().getOption().equals("win");
		if (isWinStyle) {
			return new float[]{40, 40};
		}
		return switch (size) {
			case LARGE -> new float[]{64, 64};
			case NORMAL -> new float[]{56, 56};
			case SMALL -> new float[]{40, 40};
		};
	}

	private float getFontSize() {
		return switch (size) {
			case LARGE -> 30;
			case NORMAL, SMALL -> 24;
		};
	}

	private float getRadius() {
		boolean isWinStyle = ModMenuSettings.getInstance().getUiStyleSetting().getOption().equals("win");
		if (isWinStyle) {
			return 6F;
		}
		return switch (size) {
			case LARGE -> 18;
			case NORMAL -> 16;
			case SMALL -> 12;
		};
	}

	private Color[] getColor() {

		ColorPalette palette = Soar.getInstance().getColorManager().getPalette();

		return switch (style) {
			case PRIMARY -> new Color[]{palette.getPrimaryContainer(), palette.getOnPrimaryContainer()};
			case SECONDARY -> new Color[]{palette.getSecondaryContainer(), palette.getOnSecondaryContainer()};
			case SURFACE -> new Color[]{palette.getSurfaceContainer(), palette.getPrimary()};
			case TERTIARY -> new Color[]{palette.getTertiaryContainer(), palette.getOnTertiaryContainer()};
		};
	}

	public enum Size {
		SMALL, NORMAL, LARGE
	}

	public enum Style {
		SURFACE, PRIMARY, SECONDARY, TERTIARY
	}
}
