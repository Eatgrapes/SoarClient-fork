package com.soarclient.ui.component.impl;

import com.soarclient.Soar;
import com.soarclient.animation.SimpleAnimation;
import com.soarclient.libraries.material3.hct.Hct;
import com.soarclient.management.color.api.ColorPalette;
import com.soarclient.management.mod.impl.settings.ModMenuSettings;
import com.soarclient.skia.Skia;
import com.soarclient.ui.component.Component;
import com.soarclient.ui.component.handler.impl.HctColorPickerHandler;
import com.soarclient.utils.mouse.MouseUtils;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.types.Rect;
import io.github.humbleui.skija.Shader;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;

public class HctColorPicker extends Component {

	private final SimpleAnimation slideAnimation = new SimpleAnimation();

	private Hct hct;
	private final float minValue, maxValue;
	private float value;
	private boolean dragging;

	public HctColorPicker(float x, float y, Hct hct) {
		super(x, y);
		this.hct = hct;
		minValue = 0;
		maxValue = 360;
		value = (float) (hct.getHue() - minValue) / (maxValue - minValue);
		width = 126;
		height = 32;
	}

	@Override
	public void draw(double mouseX, double mouseY) {

		boolean isWinStyle = ModMenuSettings.getInstance().getUiStyleSetting().getOption().equals("win");
		ColorPalette palette = Soar.getInstance().getColorManager().getPalette();

		slideAnimation.onTick(width * value, 20);

		if (isWinStyle) {
			float borderRadius = 6;

			// Draw hue gradient
			int[] hueColors = {0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000};
			Paint paint = new Paint();
			paint.setShader(Shader.makeLinearGradient(x, y, x + width, y, hueColors));
			Skia.getCanvas().drawRect(Rect.makeXYWH(x, y, width, height), paint);

			// Draw outline
			Skia.drawOutline(x, y, width, height, borderRadius, 1, palette.getOutline());

			// Draw thumb
			float thumbX = x + slideAnimation.getValue();
			float thumbY = y + height / 2;
			float thumbRadius = 6;
			Skia.drawCircle(thumbX, thumbY, thumbRadius, Color.getHSBColor((float) (hct.getHue() / 360), 1, 1));
			Skia.drawCircle(thumbX, thumbY, thumbRadius, 1.5f, palette.getOnSurface());

		} else { // MD3 Style
			Skia.drawRoundedImage("hue-h.png", x, y, width, height, 12);
			Skia.drawCircle(x + slideAnimation.getValue(), y + (height / 2), 9.8F,
					Color.getHSBColor((float) (hct.getHue() / 360), 1, 1));
			Skia.drawCircle(x + slideAnimation.getValue(), y + (height / 2), 10, 2F, palette.getSurface());
		}

		if (dragging) {

			value = (float) Math.min(1, Math.max(0, (mouseX - x) / width));
			hct = Hct.from((value * (maxValue - minValue) + minValue), hct.getChroma(), hct.getTone());

			onPicking(hct);
		}
	}

	@Override
	public void mousePressed(double mouseX, double mouseY, int button) {

		if (MouseUtils.isInside(mouseX, mouseY, x, y, width, height) && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			dragging = true;
		}
	}

	@Override
	public void mouseReleased(double mouseX, double mouseY, int button) {
		dragging = false;
	}

	private void onPicking(Hct hct) {
		if (handler instanceof HctColorPickerHandler hctColorPickerHandler) {
			hctColorPickerHandler.onPicking(hct);
		}
	}
}
