package com.soarclient.management.mod;

import com.soarclient.event.EventBus;
import com.soarclient.event.impl.ModToggleEvent;
import com.soarclient.utils.language.I18n;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class Mod {

	protected MinecraftClient client = MinecraftClient.getInstance();
	private final String name, description, icon;
	private boolean enabled, movable, hidden;
	private ModCategory category;

	public Mod(String name, String description, String icon, ModCategory category) {
		this.name = name;
		this.description = description;
		this.icon = icon;
		this.movable = false;
		this.hidden = false;
		this.category = category;
	}

	public void toggle() {
		enabled = !enabled;
		EventBus.getInstance().post(new ModToggleEvent(this, enabled));

		if (enabled) {
			onEnable();
		} else {
			onDisable();
		}
	}

	public void setEnabled(boolean enabled) {
		if (this.enabled == enabled) return;

		this.enabled = enabled;
		EventBus.getInstance().post(new ModToggleEvent(this, enabled));

		if (enabled) {
			onEnable();
		} else {
			onDisable();
		}
	}

	public void onEnable() {
		EventBus.getInstance().register(this);
	}

	public void onDisable() {
		EventBus.getInstance().unregister(this);
	}

	public void onRender3D(DrawContext context, float partialTicks) {}

	public String getName() {
		return I18n.get(name);
	}

	public String getRawName() {
		return name;
	}

	public String getDescription() {
		return I18n.get(description);
	}

	public String getIcon() {
		return icon;
	}

	public boolean isMovable() {
		return movable;
	}

	public void setMovable(boolean movable) {
		this.movable = movable;
	}

	public ModCategory getCategory() {
		return category;
	}

	public void setCategory(ModCategory category) {
		this.category = category;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public boolean isHidden() {
		return hidden;
	}

	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}
}
