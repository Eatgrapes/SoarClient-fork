package com.soarclient.management.mod.impl.hud;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.KeyMapping;
import com.soarclient.animation.Animation;
import com.soarclient.animation.Duration;
import com.soarclient.animation.cubicbezier.impl.EaseStandard;
import com.soarclient.animation.other.DummyAnimation;
import com.soarclient.event.EventBus;
import com.soarclient.event.client.RenderSkiaEvent;
import com.soarclient.management.mod.api.hud.HUDMod;
import com.soarclient.management.mod.impl.settings.HUDModSettings;
import com.soarclient.management.mod.settings.impl.BooleanSetting;
import com.soarclient.mixin.interfaces.IMixinKeyBinding;
import com.soarclient.skia.Skia;
import com.soarclient.skia.font.Fonts;
import com.soarclient.skia.font.Icon;
import com.soarclient.utils.ColorUtils;

public class KeystrokesMod extends HUDMod {

	private List<Panel> panels = new ArrayList<>();

	private BooleanSetting spaceKeySetting = new BooleanSetting("setting.spacekey", "setting.spacekey.description",
			Icon.KEYBOARD, this, true);
	private BooleanSetting unmarkSetting = new BooleanSetting("setting.unmarked", "setting.unmarked.description",
			Icon.TITLE, this, false);
	private BooleanSetting snapTapSetting = new BooleanSetting("setting.snaptapcompatibility",
			"setting.snaptapcompatibility.description", Icon.KEYBOARD_KEYS, this, false);

	public KeystrokesMod() {
		super("mod.keystrokes.name", "mod.keystrokes.description", Icon.KEYBOARD);

		panels.add(new Panel(client.options.keyUp, 32, 0));
		panels.add(new Panel(client.options.keyLeft, 0, 32));
		panels.add(new Panel(client.options.keyDown, 32, 32));
		panels.add(new Panel(client.options.keyRight, 64, 32));
		panels.add(new Panel(client.options.keyJump, 0, 64, 92, 22, true));
	}

	@Override
	public void begin() {
		drawBlur();
		Skia.save();
		Skia.scale(position.getX(), position.getY(), position.getScale());
	}

	public final EventBus.EventListener<RenderSkiaEvent> onRenderSkia = event -> {
		this.begin();
		draw();
		this.finish();
		position.setSize(28 * 3 + 8, spaceKeySetting.isEnabled() ? 64 + 22 : 32 + 28);
	};

	private void draw() {

		for (Panel p : panels) {

			if (p.jumpKey && !spaceKeySetting.isEnabled()) {
				continue;
			}

			p.update();
			p.draw();
		}
	}

	private void drawBlur() {

		for (Panel p : panels) {

			if (p.jumpKey && !spaceKeySetting.isEnabled()) {
				continue;
			}

			p.drawBlur();
		}
	}

	@Override
	public float getRadius() {
		return 6;
	}

	private class Panel {

		private float x, y, width, height;
		private KeyMapping keyBinding;
		private boolean jumpKey;
		private Animation animation;

		private Panel(KeyMapping keyBinding, float x, float y, float width, float height, boolean jumpKey) {
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
			this.keyBinding = keyBinding;
			this.jumpKey = jumpKey;
			this.animation = new DummyAnimation(0);
		}

		private Panel(KeyMapping keyBinding, float x, float y) {
			this(keyBinding, x, y, 28, 28, false);
		}

		private void draw() {

			KeystrokesMod.this.drawBackground(getX() + x, getY() + y, width, height);

			if (!jumpKey && !unmarkSetting.isEnabled()) {
				Skia.drawFullCenteredText(keyBinding.getTranslatedKeyMessage().getString(), getX() + x + (width / 2),
						getY() + y + (height / 2), KeystrokesMod.this.getDesign().getTextColor(), Fonts.getRegular(12));
			}

			Skia.save();
			Skia.scale(getX() + x, getY() + y, width, height, animation.getValue());
			Skia.drawRoundedRect(getX() + x, getY() + y, width, height,
					(height - ((height - 6) * animation.getValue())), ColorUtils.applyAlpha(
							KeystrokesMod.this.getDesign().getTextColor(), (int) (180 * animation.getValue())));
			Skia.restore();

			if (jumpKey && !unmarkSetting.isEnabled()) {
				Skia.drawRoundedRect(getX() + 10, getY() + 74F, (26 * 3) - 6, 2, 1.5F,
						KeystrokesMod.this.getDesign().getTextColor());
			}
		}

		private void drawBlur() {

			if (HUDModSettings.getInstance().getBlurSetting().isEnabled()) {
				Skia.drawRoundedBlur(getX() + (x * position.getScale()), getY() + (y * position.getScale()),
						width * position.getScale(), height * position.getScale(), getRadius() * position.getScale());
			}
		}

		private void update() {

			boolean isKeyDown = snapTapSetting.isEnabled() ? keyBinding.isDown()
					: ((IMixinKeyBinding) keyBinding).getRealIsPressed();

			if (isKeyDown && animation.getEnd() != 1 && client.gui.screen() == null) {
				animation = new EaseStandard(Duration.MEDIUM_3, animation.getValue(), 1);
			} else if (!isKeyDown && animation.getEnd() != 0) {
				animation = new EaseStandard(Duration.MEDIUM_3, animation.getValue(), 0);
			}
		}
	}
}
