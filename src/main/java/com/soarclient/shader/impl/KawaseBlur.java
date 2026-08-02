package com.soarclient.shader.impl;

import com.soarclient.management.mod.impl.settings.SystemSettings;
import com.soarclient.shader.Framebuffer;
import com.soarclient.shader.PostProcessRenderer;
import com.soarclient.shader.Shader;
import com.soarclient.shader.ShaderHelper;
import com.soarclient.utils.TimerUtils;

import it.unimi.dsi.fastutil.ints.IntDoubleImmutablePair;
import net.minecraft.client.Minecraft;

public class KawaseBlur {

	public static final KawaseBlur GUI_BLUR = new KawaseBlur();
	public static final KawaseBlur INGAME_BLUR = new KawaseBlur();

	private static final IntDoubleImmutablePair[] STRENGTHS = new IntDoubleImmutablePair[] {
			IntDoubleImmutablePair.of(1, 1.25), IntDoubleImmutablePair.of(1, 2.25), IntDoubleImmutablePair.of(2, 2.0),
			IntDoubleImmutablePair.of(2, 3.0), IntDoubleImmutablePair.of(2, 4.25), IntDoubleImmutablePair.of(3, 2.5),
			IntDoubleImmutablePair.of(3, 3.25), IntDoubleImmutablePair.of(3, 4.25), IntDoubleImmutablePair.of(3, 5.5),
			IntDoubleImmutablePair.of(4, 3.25), IntDoubleImmutablePair.of(4, 4.0), IntDoubleImmutablePair.of(4, 5.0),
			IntDoubleImmutablePair.of(4, 6.0), IntDoubleImmutablePair.of(4, 7.25), IntDoubleImmutablePair.of(4, 8.25),
			IntDoubleImmutablePair.of(5, 4.5), IntDoubleImmutablePair.of(5, 5.25), IntDoubleImmutablePair.of(5, 6.25),
			IntDoubleImmutablePair.of(5, 7.25), IntDoubleImmutablePair.of(5, 8.5) };

	private static Shader shaderDown, shaderUp, shaderPassthrough;
	private final Framebuffer[] fbos = new Framebuffer[6];
	private final TimerUtils timer = new TimerUtils();
	private boolean firstTick = true;

	public void resize() {
		for (int i = 0; i < fbos.length; i++) {
			if (fbos[i] != null) {
				fbos[i].resize();
			} else {
				fbos[i] = new Framebuffer(1 / Math.pow(2, i));
			}
		}
	}

	public void draw(int radius) {

		if (shaderDown == null) {
			shaderDown = new Shader("blur.vert", "blur_down.frag");
			shaderUp = new Shader("blur.vert", "blur_up.frag");
			shaderPassthrough = new Shader("passthrough.vert", "passthrough.frag");
		}

		if (firstTick) {
			for (int i = 0; i < fbos.length; i++) {
				if (fbos[i] == null) {
					fbos[i] = new Framebuffer(1 / Math.pow(2, i));
				}
			}
			firstTick = false;
		}

		SystemSettings setting = SystemSettings.getInstance();
		
		if(setting.isFastBlur()) {
			if (timer.delay(16)) {
				timer.reset();
			} else {
				return;
			}
		}
		
		IntDoubleImmutablePair strength = STRENGTHS[(int) ((radius - 1))];
		int iterations = strength.leftInt();
		double offset = strength.rightDouble();

		PostProcessRenderer.beginRender();

		renderToFbo(fbos[0], Minecraft.getInstance().getMainRenderTarget().getColorTextureId(), shaderDown,
				offset);

		for (int i = 0; i < iterations; i++) {
			renderToFbo(fbos[i + 1], fbos[i].texture, shaderDown, offset);
		}

		for (int i = iterations; i >= 1; i--) {
			renderToFbo(fbos[i - 1], fbos[i].texture, shaderUp, offset);
		}

		Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
		shaderPassthrough.bind();
		ShaderHelper.bindTexture(fbos[0].texture);
		shaderPassthrough.set("uTexture", 0);
		PostProcessRenderer.endRender();
	}

	public int getTexture() {
		return fbos[0].texture;
	}

	private void renderToFbo(Framebuffer targetFbo, int sourceText, Shader shader, double offset) {
		targetFbo.bind();
		targetFbo.setViewport();
		shader.bind();
		ShaderHelper.bindTexture(sourceText);
		shader.set("uTexture", 0);
		shader.set("uHalfTexelSize", .5 / targetFbo.width, .5 / targetFbo.height);
		shader.set("uOffset", offset);
		PostProcessRenderer.render();
	}
}
