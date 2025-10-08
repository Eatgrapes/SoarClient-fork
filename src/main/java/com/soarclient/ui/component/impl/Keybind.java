package com.soarclient.ui.component.impl;

import com.soarclient.management.mod.impl.settings.ModMenuSettings;
import org.lwjgl.glfw.GLFW;

import com.soarclient.Soar;
import com.soarclient.management.color.api.ColorPalette;
import com.soarclient.skia.Skia;
import com.soarclient.skia.font.Fonts;
import com.soarclient.ui.component.Component;
import com.soarclient.ui.component.api.PressAnimation;
import com.soarclient.ui.component.handler.impl.KeybindHandler;
import com.soarclient.utils.mouse.MouseUtils;

import net.minecraft.client.util.InputUtil;

import java.awt.Color;

public class Keybind extends Component {

	private final PressAnimation pressAnimation = new PressAnimation();

	private boolean binding;
	private InputUtil.Key key;

	public Keybind(float x, float y, InputUtil.Key key) {
		super(x, y);
		this.key = key;
		width = 126;
		height = 32;
	}

    @Override
    public void draw(double mouseX, double mouseY) {
        boolean isWinStyle = ModMenuSettings.getInstance().getUiStyleSetting().getOption().equals("win");
        ColorPalette palette = Soar.getInstance().getColorManager().getPalette();
        boolean isHovered = MouseUtils.isInside(mouseX, mouseY, x, y, width, height);

        String displayText;
        if (binding) {
            displayText = "...";
        } else if (key.equals(InputUtil.UNKNOWN_KEY)) {
            displayText = "None";
        } else {
            displayText = key.getLocalizedText().getString();
            if (displayText.startsWith("scancode.") || displayText.equals("scancode 0")) {
                displayText = "Key " + key.getCode();
            }
        }

        if (isWinStyle) {
            float borderRadius = 6;
            boolean isDarkMode = ModMenuSettings.getInstance().getDarkModeSetting().isEnabled();

            Color backgroundColor = palette.getSurfaceContainerHighest();
            Color textColor = palette.getOnSurface();
            Color borderColor = palette.getOutline();

            if (binding) {
                borderColor = palette.getPrimary();
            }

            // Draw base
            Skia.drawRoundedRect(x, y, width, height, borderRadius, backgroundColor);
            Skia.drawOutline(x, y, width, height, borderRadius, 1, borderColor);

            // Draw hover overlay
            if (isHovered && !binding) {
                Color hoverColor;
                if (isDarkMode) {
                    hoverColor = new Color(255, 255, 255, 10);
                } else {
                    hoverColor = new Color(0, 0, 0, 5);
                }
                Skia.drawRoundedRect(x, y, width, height, borderRadius, hoverColor);
            }

            Skia.drawFullCenteredText(displayText, x + (width / 2), y + (height / 2),
                textColor, Fonts.getMedium(14));

        } else { // MD3 Style
            Skia.drawRoundedRect(x, y, width, height, 12, palette.getPrimary());
            Skia.save();
            Skia.clip(x, y, width, height, 12);
            pressAnimation.draw(x, y, width, height, palette.getPrimaryContainer(), 0.12F);
            Skia.restore();

            Skia.drawFullCenteredText(displayText, x + (width / 2), y + (height / 2),
                palette.getSurface(), Fonts.getMedium(14));
        }
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
			} else if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT && button != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
				setKeyCode(InputUtil.Type.MOUSE.createFromCode(button));
			}

			binding = false;
		}

		pressAnimation.onReleased(mouseX, mouseY, x, y);
	}

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (binding) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                setKeyCode(InputUtil.UNKNOWN_KEY);
            } else {
                setKeyCode(InputUtil.fromKeyCode(keyCode, scanCode));
            }
            this.binding = false;
        }
    }

	public InputUtil.Key getKeyCode() {
		return key;
	}

	public void setKeyCode(InputUtil.Key key) {

		this.key = key;

		if (handler instanceof KeybindHandler keybindHandler) {
			keybindHandler.onBinded(key);
		}
	}
}
