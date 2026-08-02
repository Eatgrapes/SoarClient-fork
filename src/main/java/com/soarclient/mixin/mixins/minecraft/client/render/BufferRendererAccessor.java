package com.soarclient.mixin.mixins.minecraft.client.render;

import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.VertexBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BufferUploader.class)
public interface BufferRendererAccessor {
	@Accessor("currentVertexBuffer")
	static void setCurrentVertexBuffer(VertexBuffer vertexBuffer) {
	}
}