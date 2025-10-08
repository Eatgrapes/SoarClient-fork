package com.soarclient.ui.component.api;

import java.awt.Color;

import com.soarclient.animation.Animation;
import com.soarclient.animation.Duration;
import com.soarclient.animation.cubicbezier.impl.EaseEmphasizedDecelerate;
import com.soarclient.animation.other.DummyAnimation;
import com.soarclient.skia.Skia;
import com.soarclient.utils.ColorUtils;
import com.soarclient.utils.MathUtils;

public class PressAnimation {

	private Animation animation;
	private double[] pressedPos;

	public PressAnimation() {
		animation = new DummyAnimation();
		pressedPos = new double[] { 0, 0 };
	}

	public void setPressed() {
		animation = new DummyAnimation(1);
	}

	public boolean isPressed() {
		return animation.getEnd() != 0;
	}

	public void draw(float x, float y, float width, float height, Color color, float alpha) {

		float radius = calculateMaxRadius(width, height) * 1.1F;

		float resultAlpha = alpha;

		if (animation.getEnd() == 1) {
			resultAlpha = animation.getValue() * resultAlpha;
		} else if (animation.getEnd() == 2) {
			resultAlpha = (2 - animation.getValue()) * resultAlpha;
		} else {
			resultAlpha = 0;
		}

		if (animation.getEnd() == 2 && animation.isFinished()) {
			animation = new DummyAnimation();
		}

		Skia.drawCircle((float) (x + pressedPos[0]), (float) (y + pressedPos[1]),
				radius * MathUtils.clamp(animation.getValue(), 0, 1), ColorUtils.applyAlpha(color, resultAlpha));
	}

	public void onPressed(double mouseX, double mouseY, double x, double y) {
		animation = new EaseEmphasizedDecelerate(Duration.EXTRA_LONG_2, 0, 1);
		pressedPos = new double[] { mouseX - x, mouseY - y };
	}

	public void onReleased(double mouseX, double mouseY, double x, double y) {
		if (animation.getEnd() == 1) {
			animation = new EaseEmphasizedDecelerate(Duration.EXTRA_LONG_2, animation.getValue(), 2);
		}
		pressedPos = new double[] { mouseX - x, mouseY - y };
	}

	private float calculateMaxRadius(float width, float height) {
		return (float) Math.sqrt(Math.pow(width, 2) + Math.pow(height, 2));
	}
}
