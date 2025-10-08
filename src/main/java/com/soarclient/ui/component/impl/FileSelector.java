package com.soarclient.ui.component.impl;

import com.soarclient.Soar;
import com.soarclient.management.color.api.ColorPalette;
import com.soarclient.management.mod.impl.settings.ModMenuSettings;
import com.soarclient.skia.Skia;
import com.soarclient.skia.font.Fonts;
import com.soarclient.ui.component.Component;
import com.soarclient.ui.component.api.PressAnimation;
import com.soarclient.ui.component.handler.impl.FileSelectorHandler;
import com.soarclient.utils.Multithreading;
import com.soarclient.utils.file.dialog.SoarFileDialog;
import com.soarclient.utils.language.I18n;
import com.soarclient.utils.mouse.MouseUtils;
import it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.io.File;

public class FileSelector extends Component {

	private final PressAnimation pressAnimation = new PressAnimation();
	private final String[] extensions;
	private File file;

	public FileSelector(float x, float y, File file, String[] extensions) {
		super(x, y);
		width = 126;
		height = 32;
		this.file = file;
		this.extensions = extensions;
	}

	@Override
	public void draw(double mouseX, double mouseY) {

		boolean isWinStyle = ModMenuSettings.getInstance().getUiStyleSetting().getOption().equals("win");
		ColorPalette palette = Soar.getInstance().getColorManager().getPalette();
		String fileName = file != null ? file.getName() : "None";

		if (isWinStyle) {
			boolean isHovered = MouseUtils.isInside(mouseX, mouseY, x, y, width, height);
			boolean isDarkMode = ModMenuSettings.getInstance().getDarkModeSetting().isEnabled();
			float borderRadius = 6;

			// Background and border
			Skia.drawRoundedRect(x, y, width, height, borderRadius, palette.getSurfaceContainerHighest());
			Skia.drawOutline(x, y, width, height, borderRadius, 1, palette.getOutline());

			// Hover effect
			if (isHovered) {
				Color hoverColor = isDarkMode ? new Color(255, 255, 255, 10) : new Color(0, 0, 0, 5);
				Skia.drawRoundedRect(x, y, width, height, borderRadius, hoverColor);
			}

			// Text
			Skia.drawFullCenteredText(fileName, x + (width / 2), y + (height / 2), palette.getOnSurface(),
					Fonts.getMedium(14));

		} else { // MD3 Style
			Skia.drawRoundedRect(x, y, width, height, 12, palette.getPrimary());
			Skia.save();
			Skia.clip(x, y, width, height, 12);
			pressAnimation.draw(x, y, width, height, palette.getPrimaryContainer(), 0.12F);
			Skia.restore();

			Skia.drawFullCenteredText(fileName, x + (width / 2), y + (height / 2), palette.getSurface(),
					Fonts.getMedium(14));
		}
	}

	@Override
	public void mousePressed(double mouseX, double mouseY, int button) {
		if (MouseUtils.isInside(mouseX, mouseY, x, y, width, height) && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			pressAnimation.onPressed(mouseX, mouseY, x, y);
			Multithreading.runAsync(() -> {

				ObjectObjectImmutablePair<Boolean, File> p = SoarFileDialog.chooseFile(I18n.get("text.selectfile"), extensions);

				if (p.left()) {
					file = p.right();
					if (handler instanceof FileSelectorHandler fileSelectorHandler) {
						fileSelectorHandler.onSelect(file);
					}
				}
			});
		}
	}

	@Override
	public void mouseReleased(double mouseX, double mouseY, int button) {
		pressAnimation.onReleased(mouseX, mouseY, x, y);
	}
}
