package com.soarclient.shader;

import static org.lwjgl.opengl.GL11C.GL_FLOAT;
import static org.lwjgl.opengl.GL11C.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL15C.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15C.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL15C.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.system.MemoryUtil.memAddress0;
import static org.lwjgl.system.MemoryUtil.memCopy;
import static org.lwjgl.system.MemoryUtil.memPutFloat;
import static org.lwjgl.system.MemoryUtil.memPutInt;

import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL32C;

import com.mojang.blaze3d.platform.GlStateManager;

import net.minecraft.client.util.math.MatrixStack;

public class Mesh {

	private final int primitiveVerticesSize;

	private final int vao, vbo, ibo;

	private ByteBuffer vertices;
	private long verticesPointerStart, verticesPointer;

	private ByteBuffer indices;
	private long indicesPointer;

	private int vertexI, indicesCount;

	private boolean building;
	private boolean beganRendering;

	public Mesh() {

		int drawMode = 3;
		int stride = 8;

		this.primitiveVerticesSize = stride * drawMode;

		vertices = BufferUtils.createByteBuffer(primitiveVerticesSize * 256 * 4);
		verticesPointerStart = memAddress0(vertices);

		indices = BufferUtils.createByteBuffer(drawMode * 512 * 4);
		indicesPointer = memAddress0(indices);

		vao = GlStateManager._glGenVertexArrays();
		ShaderHelper.bindVertexArray(vao);

		vbo = GlStateManager._glGenBuffers();
		GlStateManager._glBindBuffer(GL15C.GL_ARRAY_BUFFER, vbo);

		ibo = GlStateManager._glGenBuffers();
		ShaderHelper.bindIndexBuffer(ibo);
		GlStateManager._enableVertexAttribArray(0);
		GlStateManager._vertexAttribPointer(0, 2, GL_FLOAT, false, stride, 0);

		ShaderHelper.bindVertexArray(0);
		GlStateManager._glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0);
		ShaderHelper.bindIndexBuffer(0);
	}

	public void destroy() {
		GlStateManager._glDeleteBuffers(ibo);
		GlStateManager._glDeleteBuffers(vbo);
		GlStateManager._glDeleteVertexArrays(vao);
	}

	public void begin() {
		if (building)
			throw new IllegalStateException("Mesh.begin() called while already building.");

		verticesPointer = verticesPointerStart;
		vertexI = 0;
		indicesCount = 0;

		building = true;
	}

	public Mesh vec2(double x, double y) {
		long p = verticesPointer;

		memPutFloat(p, (float) x);
		memPutFloat(p + 4, (float) y);

		verticesPointer += 8;
		return this;
	}

	public int next() {
		return vertexI++;
	}

	public void quad(int i1, int i2, int i3, int i4) {
		long p = indicesPointer + indicesCount * 4L;

		memPutInt(p, i1);
		memPutInt(p + 4, i2);
		memPutInt(p + 8, i3);

		memPutInt(p + 12, i3);
		memPutInt(p + 16, i4);
		memPutInt(p + 20, i1);

		indicesCount += 6;

		growIfNeeded();
	}

	public void growIfNeeded() {

		if ((vertexI + 1) * primitiveVerticesSize >= vertices.capacity()) {
			int offset = getVerticesOffset();

			int newSize = vertices.capacity() * 2;
			if (newSize % primitiveVerticesSize != 0)
				newSize += newSize % primitiveVerticesSize;

			ByteBuffer newVertices = BufferUtils.createByteBuffer(newSize);
			memCopy(memAddress0(vertices), memAddress0(newVertices), offset);

			vertices = newVertices;
			verticesPointerStart = memAddress0(vertices);
			verticesPointer = verticesPointerStart + offset;
		}

		if (indicesCount * 4 >= indices.capacity()) {
			System.out.println("wao");
			int newSize = indices.capacity() * 2;
			if (newSize % 3 != 0)
				newSize += newSize % (3 * 4);

			ByteBuffer newIndices = BufferUtils.createByteBuffer(newSize);
			memCopy(memAddress0(indices), memAddress0(newIndices), indicesCount * 4L);

			indices = newIndices;
			indicesPointer = memAddress0(indices);
		}
	}

	public void end() {
		if (!building)
			throw new IllegalStateException("Mesh called while not building.");

		if (indicesCount > 0) {
			GlStateManager._glBindBuffer(GL15C.GL_ARRAY_BUFFER, vbo);
			GlStateManager._glBufferData(GL_ARRAY_BUFFER, vertices.limit(getVerticesOffset()), GL_DYNAMIC_DRAW);
			GlStateManager._glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0);

			ShaderHelper.bindIndexBuffer(ibo);
			GlStateManager._glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices.limit(indicesCount * 4), GL_DYNAMIC_DRAW);
			ShaderHelper.bindIndexBuffer(0);
		}

		building = false;
	}

	public void beginRender(MatrixStack matrices) {
		
		ShaderHelper.disableDepth();
		ShaderHelper.enableBlend();
		ShaderHelper.disableCull();
		ShaderHelper.enableLineSmooth();

		beganRendering = true;
	}

	public void render(MatrixStack matrices) {
		if (building)
			end();

		if (indicesCount > 0) {
			boolean wasBeganRendering = beganRendering;
			if (!wasBeganRendering)
				beginRender(matrices);

			Shader.BOUND.setDefaults();

			ShaderHelper.bindVertexArray(vao);
			GlStateManager._drawElements(GL32C.GL_TRIANGLES, indicesCount, GL_UNSIGNED_INT, 0);

			ShaderHelper.bindVertexArray(0);

			if (!wasBeganRendering)
				endRender();
		}
	}

	public void endRender() {
		beganRendering = false;
	}

	public boolean isBuilding() {
		return building;
	}

	private int getVerticesOffset() {
		return (int) (verticesPointer - verticesPointerStart);
	}
}
