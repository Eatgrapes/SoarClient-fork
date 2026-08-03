package com.soarclient.utils.render;

import com.soarclient.event.EventBus;
import com.soarclient.event.client.RenderWorldEvent;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * World-space rendering for mods.
 *
 * <p>Shapes are collected during Fabric's level render pass and handed to the active
 * submit node collector, so drawing does not depend on the per-tick {@code Gizmos}
 * thread-local collector. Mods draw by listening for {@link RenderWorldEvent}.
 */
public final class Render3D {

	private static final float DEFAULT_LINE_WIDTH = 2.0f;

	private static boolean initialized;

	private Render3D() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;

		LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
			Minecraft client = Minecraft.getInstance();

			if (client.level == null || client.player == null || context.levelState() == null
					|| context.levelState().cameraRenderState == null) {
				return;
			}

			DrawableGizmoPrimitives primitives = new DrawableGizmoPrimitives();
			Renderer renderer = new Renderer(primitives);

			EventBus.getInstance().post(new RenderWorldEvent(renderer));

			if (!renderer.used) {
				return;
			}
			primitives.submit(context.submitNodeCollector(), context.levelState().cameraRenderState, true);
		});
	}

	public static final class Renderer {

		private final DrawableGizmoPrimitives primitives;
		private boolean used;

		private Renderer(DrawableGizmoPrimitives primitives) {
			this.primitives = primitives;
		}

		public void line(Vec3 from, Vec3 to, int color) {
			line(from, to, color, DEFAULT_LINE_WIDTH);
		}

		public void line(Vec3 from, Vec3 to, int color, float lineWidth) {
			used = true;
			primitives.addLine(from, to, color, lineWidth);
		}

		public void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color) {
			quad(a, b, c, d, color, DEFAULT_LINE_WIDTH);
		}

		public void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color, float lineWidth) {
			line(a, b, color, lineWidth);
			line(b, c, color, lineWidth);
			line(c, d, color, lineWidth);
			line(d, a, color, lineWidth);
		}

		public void box(AABB box, int color) {
			box(box, color, DEFAULT_LINE_WIDTH);
		}

		public void box(AABB box, int color, float lineWidth) {
			Vec3 p000 = new Vec3(box.minX, box.minY, box.minZ);
			Vec3 p001 = new Vec3(box.minX, box.minY, box.maxZ);
			Vec3 p010 = new Vec3(box.minX, box.maxY, box.minZ);
			Vec3 p011 = new Vec3(box.minX, box.maxY, box.maxZ);
			Vec3 p100 = new Vec3(box.maxX, box.minY, box.minZ);
			Vec3 p101 = new Vec3(box.maxX, box.minY, box.maxZ);
			Vec3 p110 = new Vec3(box.maxX, box.maxY, box.minZ);
			Vec3 p111 = new Vec3(box.maxX, box.maxY, box.maxZ);

			quad(p000, p001, p101, p100, color, lineWidth);
			quad(p010, p011, p111, p110, color, lineWidth);
			line(p000, p010, color, lineWidth);
			line(p001, p011, color, lineWidth);
			line(p100, p110, color, lineWidth);
			line(p101, p111, color, lineWidth);
		}
	}
}
