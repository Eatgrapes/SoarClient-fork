package com.soarclient.management.mod.impl.hud;

import java.text.DecimalFormat;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.soarclient.event.EventBus;
import com.soarclient.event.client.RenderSkiaEvent;
import com.soarclient.event.server.impl.AttackEntityEvent;
import com.soarclient.event.server.impl.DamageEntityEvent;
import com.soarclient.management.mod.api.hud.SimpleHUDMod;
import com.soarclient.skia.font.Icon;

public class ReachDisplayMod extends SimpleHUDMod {

	private DecimalFormat df = new DecimalFormat("0.##");

	private double distance = 0;
	private long hitTime = -1;
	private int possibleTarget;

	public ReachDisplayMod() {
		super("mod.reachdisplay.name", "mod.reachdisplay.description", Icon.SOCIAL_DISTANCE);
	}

	public final EventBus.EventListener<RenderSkiaEvent> onRenderSkia = event -> {
		this.draw();
	};

	public final EventBus.EventListener<DamageEntityEvent> onDamageEntity = event -> {

		if (event.getEntityId() == possibleTarget) {

			Entity entity = client.level.getEntity(event.getEntityId());

			possibleTarget = -1;
			distance = client.player.getEyePosition()
					.distanceTo(closestPointToBox(client.player.getEyePosition(), entity.getBoundingBox()));
			hitTime = System.currentTimeMillis();
		}
	};

	public final EventBus.EventListener<AttackEntityEvent> onAttackEntity = event -> {
		possibleTarget = event.getEntityId();
	};

	private Vec3 closestPointToBox(Vec3 start, AABB box) {
		return new Vec3(coerceIn(start.x, box.minX, box.maxX), coerceIn(start.y, box.minY, box.maxY),
				coerceIn(start.z, box.minZ, box.maxZ));
	}

	private double coerceIn(double target, double min, double max) {
		if (target > max) {
			return max;
		}
		return Math.max(target, min);
	}

	@Override
	public String getText() {

		if ((System.currentTimeMillis() - hitTime) > 5000) {
			distance = 0;
		}

		if (distance == 0) {
			return "Hasn't attacked";
		} else {
			return df.format(distance) + " blocks";
		}
	}

	@Override
	public String getIcon() {
		return Icon.SOCIAL_DISTANCE;
	}
}