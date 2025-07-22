package com.soarclient.gui.modmenu;

import java.util.ArrayList;
import java.util.List;

import com.soarclient.gui.api.SoarGui;
import com.soarclient.gui.api.page.SimplePage;
import com.soarclient.gui.modmenu.component.NavigationRail;
import com.soarclient.gui.modmenu.pages.*;

public class GuiModMenu extends SoarGui {

	private NavigationRail navigationRail;

	public GuiModMenu() {
		super(false);
	}

	@Override
	public void init() {
		components.clear();
		navigationRail = new NavigationRail(this, getX(), getY(), 90, getHeight());
		components.add(navigationRail);
		super.init();
	}

    @Override
    public void setPageSize(SimplePage p) {
        p.setX(getX() + navigationRail.getWidth());
        p.setY(getY());
        p.setWidth(getWidth() - navigationRail.getWidth());
        p.setHeight(getHeight());
    }

	@Override
	public List<SimplePage> createPages() {

		List<SimplePage> pages = new ArrayList<>();

		pages.add(new HomePage(this));
		pages.add(new ModsPage(this));
		pages.add(new MusicPage(this));
		pages.add(new ProfilePage(this));
        pages.add(new CapesPage(this));
        pages.add(new SettingsPage(this));

		return pages;
	}

	@Override
	public float getX() {
		return (client.getWindow().getWidth() / 2) - (getWidth() / 2);
	}

	@Override
	public float getY() {
		return (client.getWindow().getHeight() / 2) - (getHeight() / 2);
	}

	@Override
	public float getWidth() {
		return 938;
	}

	@Override
	public float getHeight() {
		return 580;
	}
}
