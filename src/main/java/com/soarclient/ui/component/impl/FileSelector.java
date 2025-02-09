package com.soarclient.ui.component.impl;

import java.io.File;

import org.lwjgl.glfw.GLFW;

import com.soarclient.Soar;
import com.soarclient.management.color.api.ColorPalette;
import com.soarclient.skia.Skia;
import com.soarclient.skia.font.Fonts;
import com.soarclient.ui.component.Component;
import com.soarclient.ui.component.api.PressAnimation;
import com.soarclient.utils.Multithreading;
import com.soarclient.utils.file.dialog.SoarFileDialog;
import com.soarclient.utils.mouse.MouseUtils;

import it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;

public class FileSelector extends Component {

	private PressAnimation pressAnimation = new PressAnimation();
	private File file;

	public FileSelector(float x, float y, File file) {
		super(x, y);
		width = 126;
		height = 32;
		this.file = file;
	}

	@Override
	public void draw(double mouseX, double mouseY) {

		ColorPalette palette = Soar.getInstance().getColorManager().getPalette();

		Skia.drawRoundedRect(x, y, width, height, 12, palette.getPrimary());
		Skia.save();
		Skia.clip(x, y, width, height, 12);
		pressAnimation.draw(x, y, width, height, palette.getPrimaryContainer(), 0.12F);
		Skia.restore();

		String fileName = file != null ? file.getName() : "None";

		Skia.drawFullCenteredText(fileName, x + (width / 2), y + (height / 2), palette.getSurface(),
				Fonts.getMedium(14));
	}

	@Override
	public void mousePressed(double mouseX, double mouseY, int button) {
		if (MouseUtils.isInside(mouseX, mouseY, x, y, width, height) && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			pressAnimation.onPressed(mouseX, mouseY, x, y);
			Multithreading.runAsync(() -> {

				ObjectObjectImmutablePair<Boolean, File> p = SoarFileDialog.chooseFile("Select Image", "png");

				if (p.left()) {
					file = p.right();
				}
			});
		}
	}

	@Override
	public void mouseReleased(double mouseX, double mouseY, int button) {
		pressAnimation.onReleased(mouseX, mouseY, x, y);
	}
}
