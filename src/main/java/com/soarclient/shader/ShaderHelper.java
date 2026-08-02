package com.soarclient.shader;

import static org.lwjgl.opengl.GL11C.GL_FALSE;
import static org.lwjgl.opengl.GL11C.GL_LINE_SMOOTH;
import static org.lwjgl.opengl.GL11C.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11C.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11C.GL_UNPACK_ALIGNMENT;
import static org.lwjgl.opengl.GL11C.GL_UNPACK_LSB_FIRST;
import static org.lwjgl.opengl.GL11C.GL_UNPACK_ROW_LENGTH;
import static org.lwjgl.opengl.GL11C.GL_UNPACK_SKIP_PIXELS;
import static org.lwjgl.opengl.GL11C.GL_UNPACK_SKIP_ROWS;
import static org.lwjgl.opengl.GL11C.GL_UNPACK_SWAP_BYTES;
import static org.lwjgl.opengl.GL11C.glDisable;
import static org.lwjgl.opengl.GL11C.glEnable;
import static org.lwjgl.opengl.GL11C.glLineWidth;
import static org.lwjgl.opengl.GL11C.glTexImage2D;
import static org.lwjgl.opengl.GL12C.GL_UNPACK_IMAGE_HEIGHT;
import static org.lwjgl.opengl.GL12C.GL_UNPACK_SKIP_IMAGES;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE0;
import static org.lwjgl.opengl.GL15C.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL20C.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20C.GL_LINK_STATUS;
import static org.lwjgl.opengl.GL20C.glUniform1f;
import static org.lwjgl.opengl.GL20C.glUniform2f;
import static org.lwjgl.opengl.GL20C.glUniform3f;
import static org.lwjgl.opengl.GL20C.glUniform3fv;
import static org.lwjgl.opengl.GL20C.glUniform4f;

import java.nio.ByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.GlStateManager;
import com.soarclient.mixin.mixins.minecraft.client.render.BufferRendererAccessor;

public class ShaderHelper {

	public static int CURRENT_IBO;
	private static int prevIbo;

	private ShaderHelper() {
	}

	public static void bindVertexArray(int vao) {
		GlStateManager._glBindVertexArray(vao);
		BufferRendererAccessor.setCurrentVertexBuffer(null);
	}

	public static void bindIndexBuffer(int ibo) {
		if (ibo != 0)
			prevIbo = CURRENT_IBO;
		GlStateManager._glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo != 0 ? ibo : prevIbo);
	}

	public static String compileShader(int shader) {
		GlStateManager.glCompileShader(shader);

		if (GlStateManager.glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
			return GlStateManager.glGetShaderInfoLog(shader, 512);
		}

		return null;
	}

	public static String linkProgram(int program, int vertShader, int fragShader) {
		GlStateManager.glAttachShader(program, vertShader);
		GlStateManager.glAttachShader(program, fragShader);
		GlStateManager.glLinkProgram(program);

		if (GlStateManager.glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
			return GlStateManager.glGetProgramInfoLog(program, 512);
		}

		return null;
	}

	public static void useProgram(int program) {
		GlStateManager._glUseProgram(program);
	}

	public static void viewport(int x, int y, int width, int height) {
		GlStateManager._viewport(x, y, width, height);
	}

	public static int getUniformLocation(int program, String name) {
		return GlStateManager._glGetUniformLocation(program, name);
	}

	public static void uniformInt(int location, int v) {
		GlStateManager._glUniform1i(location, v);
	}

	public static void uniformFloat(int location, float v) {
		glUniform1f(location, v);
	}

	public static void uniformFloat2(int location, float v1, float v2) {
		glUniform2f(location, v1, v2);
	}

	public static void uniformFloat3(int location, float v1, float v2, float v3) {
		glUniform3f(location, v1, v2, v3);
	}

	public static void uniformFloat4(int location, float v1, float v2, float v3, float v4) {
		glUniform4f(location, v1, v2, v3, v4);
	}

	public static void uniformFloat3Array(int location, float[] v) {
		glUniform3fv(location, v);
	}

	public static void pixelStore(int name, int param) {
		GlStateManager._pixelStore(name, param);
	}

	public static void textureParam(int target, int name, int param) {
		GlStateManager._texParameter(target, name, param);
	}

	public static void textureImage2D(int target, int level, int internalFormat, int width, int height, int border,
			int format, int type, ByteBuffer pixels) {
		glTexImage2D(target, level, internalFormat, width, height, border, format, type, pixels);
	}

	public static void defaultPixelStore() {
		pixelStore(GL_UNPACK_SWAP_BYTES, GL_FALSE);
		pixelStore(GL_UNPACK_LSB_FIRST, GL_FALSE);
		pixelStore(GL_UNPACK_ROW_LENGTH, 0);
		pixelStore(GL_UNPACK_IMAGE_HEIGHT, 0);
		pixelStore(GL_UNPACK_SKIP_ROWS, 0);
		pixelStore(GL_UNPACK_SKIP_PIXELS, 0);
		pixelStore(GL_UNPACK_SKIP_IMAGES, 0);
		pixelStore(GL_UNPACK_ALIGNMENT, 4);
	}

	public static void framebufferTexture2D(int target, int attachment, int textureTarget, int texture, int level) {
		GlStateManager._glFramebufferTexture2D(target, attachment, textureTarget, texture, level);
	}

	public static void enableDepth() {
		GlStateManager._enableDepthTest();
	}

	public static void disableDepth() {
		GlStateManager._disableDepthTest();
	}

	public static void enableBlend() {
		GlStateManager._enableBlend();
		GlStateManager._blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
	}

	public static void disableBlend() {
		GlStateManager._disableBlend();
	}

	public static void enableCull() {
		GlStateManager._enableCull();
	}

	public static void disableCull() {
		GlStateManager._disableCull();
	}

	public static void enableLineSmooth() {
		glEnable(GL_LINE_SMOOTH);
		glLineWidth(1);
	}

	public static void disableLineSmooth() {
		glDisable(GL_LINE_SMOOTH);
	}

	public static void bindTexture(Identifier id) {
		AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(id);
		bindTexture(texture.getId(), 0);
	}

	public static void bindTexture(int i, int slot) {
		GlStateManager._activeTexture(GL_TEXTURE0 + slot);
		GlStateManager._bindTexture(i);
	}

	public static void bindTexture(int i) {
		bindTexture(i, 0);
	}

	public static void resetTextureSlot() {
		GlStateManager._activeTexture(GL_TEXTURE0);
	}
}
