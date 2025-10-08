package com.soarclient.gui.modmenu.pages;

import com.soarclient.Soar;
import com.soarclient.animation.SimpleAnimation;
import com.soarclient.gui.api.SoarGui;
import com.soarclient.gui.api.page.Page;
import com.soarclient.gui.api.page.impl.LeftRightTransition;
import com.soarclient.gui.api.page.impl.RightLeftTransition;
import com.soarclient.management.color.api.ColorPalette;
import com.soarclient.management.mod.Mod;
import com.soarclient.management.mod.impl.settings.ModMenuSettings;
import com.soarclient.skia.Skia;
import com.soarclient.skia.font.Fonts;
import com.soarclient.skia.font.Icon;
import com.soarclient.utils.ColorUtils;
import com.soarclient.utils.SearchUtils;
import com.soarclient.utils.language.I18n;
import com.soarclient.utils.mouse.MouseUtils;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SettingsPage extends Page {

	private final List<Item> items = new ArrayList<>();
	private List<Item> filteredItems = new ArrayList<>();

	public SettingsPage(SoarGui parent) {
		super(parent, "text.settings", Icon.SETTINGS, new RightLeftTransition(true));

		for (Mod m : Soar.getInstance().getModManager().getMods()) {
			if (m.isHidden()) {
				items.add(new Item(m));
			}
		}
	}

	@Override
	public void init() {
		super.init();
		updateFilteredItems();
	}

	private void updateFilteredItems() {
		if (searchBar.getText().isEmpty()) {
			filteredItems = new ArrayList<>(items);
		} else {
			filteredItems = items.stream()
				.filter(i -> SearchUtils.isSimilar(I18n.get(i.mod.getName()) + " " + I18n.get(i.mod.getDescription()), searchBar.getText()))
				.collect(Collectors.toList());
		}
	}

	@Override
	public void draw(double mouseX, double mouseY) {
		updateFilteredItems();
		boolean isWinStyle = ModMenuSettings.getInstance().getUiStyleSetting().getOption().equals("win");
		ColorPalette palette = Soar.getInstance().getColorManager().getPalette();

		double relativeMouseY = mouseY - scrollHelper.getValue();
		Skia.save();
		Skia.translate(0, scrollHelper.getValue());

		super.draw(mouseX, relativeMouseY);

		if (isWinStyle) {
			drawWinStyle(mouseX, relativeMouseY, palette);
		} else {
			drawMd3Style(mouseX, relativeMouseY, palette);
		}

		Skia.restore();
	}

	private void drawWinStyle(double mouseX, double mouseY, ColorPalette palette) {
		float topMargin = 96;
		float leftMargin = 32;
		float itemHeight = 56;
		float itemGap = 8;
		float itemWidth = width - (leftMargin * 2);
		float borderRadius = 6;

		float offsetY = topMargin;

		for (Item i : filteredItems) {
			Mod m = i.mod;
			float itemY = y + offsetY;
			i.yAnimation.onTick(itemY, 14);
			itemY = i.yAnimation.getValue();

			boolean isHovered = MouseUtils.isInside(mouseX, mouseY, x + leftMargin, itemY, itemWidth, itemHeight);

			Color backgroundColor = isHovered ? palette.getSurfaceContainer() : palette.getSurface();
			Skia.drawRoundedRect(x + leftMargin, itemY, itemWidth, itemHeight, borderRadius, backgroundColor);

			Skia.drawFullCenteredText(m.getIcon(), x + leftMargin + 30, itemY + (itemHeight / 2), palette.getOnSurface(), Fonts.getIcon(28));
			Skia.drawText(I18n.get(m.getName()), x + leftMargin + 52, itemY + 14, palette.getOnSurface(), Fonts.getRegular(16));
			Skia.drawText(I18n.get(m.getDescription()), x + leftMargin + 52, itemY + 31, palette.getOnSurfaceVariant(), Fonts.getRegular(13));
			Skia.drawHeightCenteredText(">", x + width - 54, itemY + (itemHeight / 2), palette.getOnSurface(), Fonts.getRegular(20));

			offsetY += itemHeight + itemGap;
		}

		scrollHelper.setMaxScroll(offsetY, height);
	}

	private void drawMd3Style(double mouseX, double mouseY, ColorPalette palette) {
		float topMargin = 96;
		float leftMargin = 32;
		float itemHeight = 68;
		float itemGap = 18;
		float itemWidth = width - (leftMargin * 2);
		float borderRadius = 18;

		float offsetY = topMargin;

		for (Item i : filteredItems) {
			Mod m = i.mod;
			float itemY = y + offsetY;
			i.yAnimation.onTick(itemY, 14);
			itemY = i.yAnimation.getValue();

			Skia.drawRoundedRect(x + leftMargin, itemY, itemWidth, itemHeight, borderRadius, palette.getSurface());
			Skia.drawFullCenteredText(m.getIcon(), x + leftMargin + 30, itemY + (itemHeight / 2), palette.getOnSurface(), Fonts.getIcon(32));
			Skia.drawText(I18n.get(m.getName()), x + leftMargin + 52, itemY + 20, palette.getOnSurface(), Fonts.getRegular(17));
			Skia.drawText(I18n.get(m.getDescription()), x + leftMargin + 52, itemY + 37, palette.getOnSurfaceVariant(), Fonts.getRegular(14));
			Skia.drawHeightCenteredText(">", x + width - 54, itemY + (itemHeight / 2), palette.getOnSurface(), Fonts.getRegular(20));

			offsetY += itemHeight + itemGap;
		}

		scrollHelper.setMaxScroll(offsetY, height);
	}


	@Override
	public void mousePressed(double mouseX, double mouseY, int button) {
		super.mousePressed(mouseX, mouseY, button);
		searchBar.mousePressed(mouseX, mouseY - scrollHelper.getValue(), button);
	}

	@Override
	public void mouseReleased(double mouseX, double mouseY, int button) {
		super.mouseReleased(mouseX, mouseY, button);

		double relativeMouseY = mouseY - scrollHelper.getValue();
		searchBar.mouseReleased(mouseX, relativeMouseY, button);

		boolean isWinStyle = ModMenuSettings.getInstance().getUiStyleSetting().getOption().equals("win");
		float topMargin = isWinStyle ? 96 : 96;
		float leftMargin = isWinStyle ? 32 : 32;
		float itemHeight = isWinStyle ? 56 : 68;
		float itemGap = isWinStyle ? 8 : 18;
		float itemWidth = width - (leftMargin * 2);

		float offsetY = topMargin;

		for (Item i : filteredItems) {
			float itemY = y + offsetY;

			if (MouseUtils.isInside(mouseX, relativeMouseY, x + leftMargin, itemY, itemWidth, itemHeight) && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
				parent.setCurrentPage(new SettingsImplPage(parent, this.getClass(), i.mod));
				this.setTransition(new LeftRightTransition(true));
				return; // Exit after finding the clicked item
			}
			offsetY += itemHeight + itemGap;
		}
	}

	@Override
	public void onClosed() {
		this.setTransition(new RightLeftTransition(true));
	}

	private static class Item {
		private final SimpleAnimation yAnimation = new SimpleAnimation();
		private final Mod mod;

		private Item(Mod mod) {
			this.mod = mod;
		}
	}
}
