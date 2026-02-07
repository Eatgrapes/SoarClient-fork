package com.soarclient.gui.edithud;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.lwjgl.glfw.GLFW;

import com.soarclient.Soar;
import com.soarclient.gui.api.SimpleSoarGui;
import com.soarclient.gui.edithud.api.GrabOffset;
import com.soarclient.gui.edithud.api.HUDCore;
import com.soarclient.gui.edithud.api.SnappingLine;
import com.soarclient.management.config.ConfigType;
import com.soarclient.management.mod.api.Position;
import com.soarclient.management.mod.api.hud.HUDMod;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import net.minecraft.client.gui.screen.Screen;

public class GuiEditHUD extends SimpleSoarGui {

	private static final float SCALE_CHANGE_AMOUNT = 0.1F;
	private static final float DEFAULT_LINE_WIDTH = 0.5F;

	private final Screen prevScreen;
	private final List<HUDMod> mods;
	private final int snappingDistance;

	private Optional<ObjectObjectImmutablePair<HUDMod, GrabOffset>> selectedMod;
	private boolean snapping;

	public GuiEditHUD(Screen prevScreen) {
		super(true);
		this.prevScreen = prevScreen;
		this.snappingDistance = 6;
		this.mods = initializeMods();
		this.selectedMod = Optional.empty();
		HUDCore.isEditing = true;
	}

	private List<HUDMod> initializeMods() {
		List<HUDMod> modsList = Soar.getInstance().getModManager().getHUDMods();
		Collections.reverse(modsList);
		return modsList;
	}

	@Override
	public void draw(double mouseX, double mouseY) {
		selectedMod.ifPresent(mod -> updateModPosition(mod, mouseX, mouseY));
	}

	@Override
	public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (selectedMod.isEmpty()) {
			handleMouseWheel(mouseX, mouseY, verticalAmount);
		}
	}

	private void updateModPosition(ObjectObjectImmutablePair<HUDMod, GrabOffset> mod, double mouseX, double mouseY) {
		setHudPositions(mod, mouseX, mouseY, snapping, DEFAULT_LINE_WIDTH);
	}

	private void handleMouseWheel(double mouseX, double mouseY, double amount) {

		double dWheel = amount;

		getHoveredMod(mouseX, mouseY).ifPresent(mod -> {
			Position position = mod.getPosition();
			float newScale = calculateNewScale(position.getScale(), dWheel);
			position.setScale(newScale);
			Soar.getInstance().getConfigManager().save(ConfigType.MOD);
		});
	}

	private float calculateNewScale(float currentScale, double wheelDelta) {
		float change = wheelDelta > 0 ? SCALE_CHANGE_AMOUNT : -SCALE_CHANGE_AMOUNT;
		float newScale = currentScale + change;
		return Math.round(newScale * 10.0F) / 10.0F;
	}

	@Override
	public void mousePressed(double mouseX, double mouseY, int button) {
		getHoveredMod(mouseX, mouseY).ifPresent(mod -> {
			if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
				mod.getPosition().setScale(1.0F);
				return;
			}

			GrabOffset offset = new GrabOffset((float) (mouseX - mod.getPosition().getX()),
					(float) (mouseY - mod.getPosition().getY()));
			selectedMod = Optional.of(ObjectObjectImmutablePair.of(mod, offset));

			snapping = button == 0;
		});
	}

	@Override
	public void mouseReleased(double mouseX, double mouseY, int button) {
		selectedMod = Optional.empty();
		Soar.getInstance().getConfigManager().save(ConfigType.MOD);
	}

	@Override
	public void keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			HUDCore.isEditing = false;
			Soar.getInstance().getConfigManager().save(ConfigType.MOD);
			client.setScreen(prevScreen);
		}
	}

	private Optional<HUDMod> getHoveredMod(double mouseX, double mouseY) {
		return mods.stream().filter(mod -> isModInteractable(mod) && isInside(mod, mouseX, mouseY)).findFirst();
	}

	private boolean isModInteractable(HUDMod mod) {
		return mod.isEnabled() && !mod.isHidden() && mod.isMovable();
	}

	private boolean isInside(HUDMod mod, double mouseX, double mouseY) {
		Position pos = mod.getPosition();
		return mouseX >= pos.getX() && mouseX <= pos.getRightX() && mouseY >= pos.getY() && mouseY <= pos.getBottomY();
	}

	private void setHudPositions(ObjectObjectImmutablePair<HUDMod, GrabOffset> modPair, double mouseX, double mouseY,
			boolean snap, float lineWidth) {
		GrabOffset offset = modPair.right();
		Position position = modPair.left().getPosition();

		float x = (float) (mouseX - offset.getX());
		float y = (float) (mouseY - offset.getY());

		if (snap) {
			x = getXSnapping(lineWidth, x, position.getWidth(), true);
			y = getYSnapping(lineWidth, y, position.getHeight(), true);
		}

		position.setPosition(x, y);
	}

	private float getXSnapping(float lineWidth, float x, float width, boolean multipleSides) {
		return getSnappingPosition(getXSnappingLines(), x, width, multipleSides, lineWidth, true);
	}

	private float getYSnapping(float lineWidth, float y, float height, boolean multipleSides) {
		return getSnappingPosition(getYSnappingLines(), y, height, multipleSides, lineWidth, false);
	}

	private float getSnappingPosition(FloatArrayList lines, float position, float size, boolean multipleSides,
			float lineWidth, boolean isHorizontal) {
		List<SnappingLine> snappingLines = findClosestSnappingLines(lines, position, size, multipleSides);

		if (snappingLines.isEmpty()) {
			return position;
		}

		snappingLines.forEach(line -> line.drawLine(lineWidth, isHorizontal));
		return snappingLines.get(0).getPosition();
	}

	private List<SnappingLine> findClosestSnappingLines(FloatArrayList lines, float position, float size,
			boolean multipleSides) {
		List<SnappingLine> snappingLines = new ArrayList<>();
		float closest = snappingDistance;

		for (Float line : lines) {
			SnappingLine snappingLine = new SnappingLine(line, position, size, multipleSides);
			float distance = snappingLine.getDistance();

			if (Math.round(distance) == Math.round(closest)) {
				snappingLines.add(snappingLine);
			} else if (distance < closest) {
				closest = distance;
				snappingLines.clear();
				snappingLines.add(snappingLine);
			}
		}

		return snappingLines;
	}

	private FloatArrayList getXSnappingLines() {
		return getSnappingLines(true);
	}

	private FloatArrayList getYSnappingLines() {
		return getSnappingLines(false);
	}

	private FloatArrayList getSnappingLines(boolean isHorizontal) {

		FloatArrayList lines = new FloatArrayList();

		lines.add(isHorizontal ? client.getWindow().getScaledWidth() / 2F : client.getWindow().getScaledHeight() / 2F);

		mods.stream().filter(
				mod -> isModInteractable(mod) && !selectedMod.map(pair -> pair.left().equals(mod)).orElse(false))
				.forEach(mod -> {
					Position p = mod.getPosition();
					if (isHorizontal) {
						lines.add(p.getX());
						lines.add(p.getCenterX());
						lines.add(p.getRightX());
					} else {
						lines.add(p.getY());
						lines.add(p.getCenterY());
						lines.add(p.getBottomY());
					}
				});

		return lines;
	}
}