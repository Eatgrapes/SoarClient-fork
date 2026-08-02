package com.soarclient.management.mod.impl.render;

import java.util.Arrays;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import com.soarclient.event.EventBus;
import com.soarclient.event.client.ClientTickEvent;
import com.soarclient.management.mod.Mod;
import com.soarclient.management.mod.ModCategory;
import com.soarclient.management.mod.settings.impl.ComboSetting;
import com.soarclient.skia.font.Icon;

public class ProjectileTrailMod extends Mod {

	private ComboSetting effectSetting = new ComboSetting("setting.effect", "setting.effect.description",
			Icon.EXPLOSION, this,
			Arrays.asList("setting.blacksmoke", "setting.fire", "setting.greenstar", "setting.hearts", "setting.magic",
					"setting.musicnote", "setting.slime", "setting.spark", "setting.swirl", "setting.whitesmoke"),
			"setting.hearts");

	public ProjectileTrailMod() {
		super("mod.projectiletrail.name", "mod.projectiletrail.description", Icon.TRAIL_LENGTH, ModCategory.RENDER);
	}

	public final EventBus.EventListener<ClientTickEvent> onClientTick = event -> {

		if (client.player != null && client.level != null) {

			AABB box = new AABB(client.player.position().add(-150.0, -150.0, -150.0),
					client.player.position().add(150.0, 150.0, 150.0));

			for (Projectile projectile : client.level.getEntitiesOfClass(Projectile.class, box,
					entity -> true)) {

				if (!(projectile.getDeltaMovement().lengthSqr() > 0.01) || projectile.onGround()
						|| client.level.getBlockState(projectile.blockPosition()).isRedstoneConductor(client.level,
								projectile.blockPosition()) && projectile.getOwner().getId() != client.player.getId()) {
					continue;
				}

				ParticleType<?> type = getCurrentType();

				if (type != null && type instanceof ParticleOptions) {
					client.level.addParticle((ParticleOptions) type, projectile.getX(), projectile.getY(),
							projectile.getZ(), 0.0, 0.0, 0.0);
				}
			}
		}
	};

	private ParticleType<?> getCurrentType() {

		String option = effectSetting.getOption();

		if (option.contains("blacksmoke")) {
			return ParticleTypes.SMOKE;
		} else if (option.contains("fire")) {
			return ParticleTypes.FLAME;
		} else if (option.contains("greenstar")) {
			return ParticleTypes.HAPPY_VILLAGER;
		} else if (option.contains("hearts")) {
			return ParticleTypes.HEART;
		} else if (option.contains("magic")) {
			return ParticleTypes.WITCH;
		} else if (option.contains("musicnote")) {
			return ParticleTypes.NOTE;
		} else if (option.contains("slime")) {
			return ParticleTypes.ITEM_SLIME;
		} else if (option.contains("spark")) {
			return ParticleTypes.FIREWORK;
		} else if (option.contains("whitesmoke")) {
			return ParticleTypes.WHITE_SMOKE;
		}

		return null;
	}
}