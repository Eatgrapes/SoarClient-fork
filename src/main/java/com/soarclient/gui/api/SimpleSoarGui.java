package com.soarclient.gui.api;

import com.soarclient.skia.Skia;
import com.soarclient.skia.context.SkiaContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class SimpleSoarGui {

	protected Minecraft client = Minecraft.getInstance();
	private final boolean mcScale;

	public SimpleSoarGui(boolean mcScale) {
		this.mcScale = mcScale;
	}

	public void init() {
	}

	public void draw(double mouseX, double mouseY) {
	}

	public void mousePressed(double mouseX, double mouseY, int button) {
	}

	public void mouseReleased(double mouseX, double mouseY, int button) {
	}

	public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
	}

	public void charTyped(char chr, int modifiers) {
	}

	public void keyPressed(int keyCode, int scanCode, int modifiers) {
	}

	public Screen build() {
		return new Screen(Component.empty()) {

			@Override
			public void init() {
				SimpleSoarGui.this.init();
			}

			@Override
			public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
				SkiaContext.draw(canvas -> {
					Skia.save();
					if (mcScale) {
						Skia.scale((float) minecraft.getWindow().getGuiScale());
					}
					SimpleSoarGui.this.draw(mcScale ? mouseX : minecraft.mouseHandler.xpos(),
							mcScale ? mouseY : minecraft.mouseHandler.ypos());
					Skia.restore();
				});
			}

			@Override
			public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
				SimpleSoarGui.this.mousePressed(mcScale ? event.x() : minecraft.mouseHandler.xpos(),
						mcScale ? event.y() : minecraft.mouseHandler.ypos(), event.button());
				return true;
			}

			@Override
			public boolean mouseReleased(MouseButtonEvent event) {
				SimpleSoarGui.this.mouseReleased(mcScale ? event.x() : minecraft.mouseHandler.xpos(),
						mcScale ? event.y() : minecraft.mouseHandler.ypos(), event.button());
				return true;
			}

			@Override
			public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
				SimpleSoarGui.this.mouseScrolled(mcScale ? mouseX : minecraft.mouseHandler.xpos(),
						mcScale ? mouseY : minecraft.mouseHandler.ypos(), horizontalAmount, verticalAmount);
				return true;
			}

			@Override
			public boolean keyPressed(KeyEvent event) {
				SimpleSoarGui.this.keyPressed(event.key(), event.scancode(), event.modifiers());
				return true;
			}

			@Override
			public boolean charTyped(CharacterEvent event) {
				if (Character.isBmpCodePoint(event.codepoint())) {
					SimpleSoarGui.this.charTyped((char) event.codepoint(), 0);
				}
				return true;
			}

			@Override
			public boolean isPauseScreen() {
				return false;
			}
		};
	}
}
