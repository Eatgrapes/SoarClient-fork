package com.soarclient.management.mod.impl.hud;

import java.text.DecimalFormat;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import com.soarclient.event.EventBus;
import com.soarclient.event.client.RenderSkiaEvent;
import com.soarclient.management.mod.api.hud.SimpleHUDMod;
import com.soarclient.skia.font.Icon;

public class SpeedometerMod extends SimpleHUDMod {

	private DecimalFormat speedFormat = new DecimalFormat("0.00");

	public SpeedometerMod() {
		super("mod.speedometer.name", "mod.speedometer.description", Icon.SPEED);
	}

	public final EventBus.EventListener<RenderSkiaEvent> onRenderSkia = event -> {
		this.draw();
	};
	
	@Override
	public String getText() {

		String suffix = " m/s";
		
		if (client.player != null) {
			
			Entity entity = client.player.getVehicle() == null ? client.player : client.player.getVehicle();
			Vec3 vec = entity.getKnownMovement();
			
			if (entity.onGround() && vec.y < 0) {
				vec = new Vec3(vec.x, 0, vec.z);
			}
			
			return speedFormat.format(vec.length() * 20) + suffix;
		}

		return speedFormat.format(0) + suffix;
	}

	@Override
	public String getIcon() {
		return Icon.SPEED;
	}
}
