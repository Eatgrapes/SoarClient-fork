package com.soarclient.management.mod.impl.hud;

import com.soarclient.animation.SimpleAnimation;
import com.soarclient.event.EventBus;
import com.soarclient.event.client.ClientTickEvent;
import com.soarclient.event.client.PlayerDirectionChangeEvent;
import com.soarclient.event.client.RenderSkiaEvent;
import com.soarclient.management.mod.api.hud.HUDMod;
import com.soarclient.skia.Skia;
import com.soarclient.skia.font.Icon;
import net.minecraft.util.Mth;

public class MouseStrokesMod extends HUDMod {

	private float mouseX, mouseY, lastMouseX, lastMouseY;
	private final SimpleAnimation xAnimation = new SimpleAnimation();
	private final SimpleAnimation yAnimation = new SimpleAnimation();

	public MouseStrokesMod() {
		super("mod.mousestrokes.name", "mod.mousestrokes.description", Icon.TOUCHPAD_MOUSE);
	}

	public final EventBus.EventListener<RenderSkiaEvent> onRenderSkia = event -> {

		float calculatedMouseX = lastMouseX + (mouseX - lastMouseX);
		float calculatedMouseY = lastMouseY + (mouseY - lastMouseY);

		xAnimation.onTick(calculatedMouseX, 20);
		yAnimation.onTick(calculatedMouseY, 20);

		this.begin();
		this.drawBackground(getX(), getY(), 58, 58);
		Skia.drawCircle(getX() + xAnimation.getValue() + 29, getY() + yAnimation.getValue() + 29, 4.5F,
				this.getDesign().getTextColor());
		this.finish();
		position.setSize(58, 58);
	};

	public final EventBus.EventListener<ClientTickEvent> onClientTick = event -> {
		lastMouseX = mouseX;
		lastMouseY = mouseY;
		mouseX *= 0.75f;
		mouseY *= 0.75f;
	};

	public final EventBus.EventListener<PlayerDirectionChangeEvent> onPlayerDirectionChange = event -> {
		mouseX += (event.getYaw() - event.getPrevYaw()) / 7F;
		mouseY += (event.getPitch() - event.getPrevPitch()) / 7F;
		mouseX = Mth.clamp(mouseX, -20, 20);
		mouseY = Mth.clamp(mouseY, -20, 20);
	};

	@Override
	public float getRadius() {
		return 6;
	}
}
