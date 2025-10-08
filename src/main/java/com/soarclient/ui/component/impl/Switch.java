package com.soarclient.ui.component.impl;

import java.awt.Color;

import com.soarclient.management.mod.impl.settings.ModMenuSettings;
import org.lwjgl.glfw.GLFW;

import com.soarclient.Soar;
import com.soarclient.animation.SimpleAnimation;
import com.soarclient.management.color.api.ColorPalette;
import com.soarclient.skia.Skia;
import com.soarclient.ui.component.Component;
import com.soarclient.ui.component.handler.impl.SwitchHandler;
import com.soarclient.utils.ColorUtils;
import com.soarclient.utils.mouse.MouseUtils;

public class Switch extends Component {

	private final SimpleAnimation enableAnimation = new SimpleAnimation();
	private final SimpleAnimation pressAnimation = new SimpleAnimation();
	private final SimpleAnimation focusAnimation = new SimpleAnimation();
	private boolean pressed;
	private boolean enabled;

	public Switch(float x, float y, boolean enabled) {
		super(x, y);
		this.width = 52;
		this.height = 32;
		this.enabled = enabled;
		this.pressed = false;
	}

	@Override
	public void draw(double mouseX, double mouseY) {

		boolean isWinStyle = ModMenuSettings.getInstance().getUiStyleSetting().getOption().equals("win");
		ColorPalette palette = Soar.getInstance().getColorManager().getPalette();
		boolean focus = MouseUtils.isInside(mouseX, mouseY, x, y, width, height);

		enableAnimation.onTick(enabled ? 1 : 0, 12);

		if (isWinStyle) {
			float trackWidth = 40;
			float trackHeight = 20;
			float trackX = x + (width - trackWidth) / 2;
			float trackY = y + (height - trackHeight) / 2;
			float trackCornerRadius = 10;

			// Determine colors based on state
			Color trackColor;
			Color thumbColor;
			Color outlineColor = ColorUtils.applyAlpha(palette.getOutline(), 0.5f); // Softer outline

			if (enabled) {
				trackColor = palette.getPrimary();
				thumbColor = new Color(255, 255, 255); // White thumb
			} else {
				trackColor = palette.getSurfaceContainerHighest();
				thumbColor = palette.getOnSurfaceVariant();
			}

			// Draw track
			Skia.drawRoundedRect(trackX, trackY, trackWidth, trackHeight, trackCornerRadius, trackColor);
			// Draw outline
			Skia.drawOutline(trackX, trackY, trackWidth, trackHeight, trackCornerRadius, 1, outlineColor);

			// Hover effect
			if (focus) {
				Skia.drawRoundedRect(trackX, trackY, trackWidth, trackHeight, trackCornerRadius, new Color(0, 0, 0, 20));
			}

			// Draw thumb
			float thumbRadius = (trackHeight / 2) - 3; // 7
			float startX = trackX + trackHeight / 2;
			float endX = trackX + trackWidth - trackHeight / 2;
			float thumbX = startX + (endX - startX) * enableAnimation.getValue();
			float thumbY = trackY + trackHeight / 2;

			Skia.drawCircle(thumbX, thumbY, thumbRadius, thumbColor);

		} else {
			pressAnimation.onTick(pressed ? 1 : 0, 12);
			focusAnimation.onTick(focus ? 1 : 0, 10);

			Skia.drawRoundedRect(x, y, width, height, 16, palette.getSurfaceContainerHighest());
			Skia.drawOutline(x, y, width, height, 16, 2, palette.getOutline());

			Skia.drawRoundedRect(x, y, width, height, 16,
					ColorUtils.applyAlpha(palette.getPrimary(), enableAnimation.getValue()));

			Color fc = enabled ? palette.getPrimaryContainer() : palette.getOnSurfaceVariant();
			Color ec = enabled ? palette.getOnPrimary() : palette.getOutline();

			Color pc = enabled ? palette.getPrimary() : palette.getOnSurface();

			Skia.drawCircle(x + 16 + (20 * enableAnimation.getValue()), y + 16,
					8 + (enableAnimation.getValue() * 4) + (pressAnimation.getValue() * 1), ec);
			Skia.drawCircle(x + 16 + (20 * enableAnimation.getValue()), y + 16,
					8 + (enableAnimation.getValue() * 4) + (pressAnimation.getValue() * 1),
					ColorUtils.applyAlpha(fc, focusAnimation.getValue()));
			Skia.drawCircle(x + 16 + (20 * enableAnimation.getValue()), y + 16,
					8 + (enableAnimation.getValue() * 4) + (pressAnimation.getValue() * 10),
					ColorUtils.applyAlpha(pc, pressAnimation.getValue() * 0.12F));
		}
	}

	@Override
	public void mousePressed(double mouseX, double mouseY, int button) {
		if (MouseUtils.isInside(mouseX, mouseY, x, y, width, height) && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			pressed = true;
		}
	}

	@Override
	public void mouseReleased(double mouseX, double mouseY, int button) {

		if (MouseUtils.isInside(mouseX, mouseY, x, y, width, height) && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			enabled = !enabled;

			if (handler instanceof SwitchHandler sHandler) {

				if (enabled) {
					sHandler.onEnabled();
				} else {
					sHandler.onDisabled();
				}
			}
		}

		pressed = false;
	}
}
